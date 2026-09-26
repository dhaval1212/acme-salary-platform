import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter } from '@angular/router';

import { EmployeeListComponent } from './employee-list.component';
import { EmployeeService } from '../../../core/services/employee.service';
import { Employee } from '../../../core/models/employee.model';
import { PageMeta } from '../../../core/models/api-response.model';

const META: PageMeta = { page: 0, pageSize: 20, totalElements: 1, totalPages: 1 };
const ALICE: Employee = {
  id: 'emp-1', fullName: 'Alice Smith', email: 'alice@acme.com',
  department: 'Engineering', jobTitle: 'SWE', country: 'US',
  status: 'ACTIVE', createdAt: '2025-01-01T00:00:00Z', updatedAt: '2025-01-01T00:00:00Z'
};

describe('EmployeeListComponent', () => {
  let fixture: ComponentFixture<EmployeeListComponent>;
  let component: EmployeeListComponent;
  let employeeServiceSpy: jasmine.SpyObj<EmployeeService>;
  let dialogSpy: jasmine.SpyObj<MatDialog>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    employeeServiceSpy = jasmine.createSpyObj('EmployeeService', ['list', 'deactivate']);
    dialogSpy          = jasmine.createSpyObj('MatDialog', ['open']);
    snackBarSpy        = jasmine.createSpyObj('MatSnackBar', ['open']);
    routerSpy          = jasmine.createSpyObj('Router', ['navigate']);

    employeeServiceSpy.list.and.returnValue(of({ data: [ALICE], meta: META }));

    await TestBed.configureTestingModule({
      imports: [EmployeeListComponent],
      providers: [
        provideAnimationsAsync(),
        provideRouter([]),
        { provide: EmployeeService, useValue: employeeServiceSpy },
        { provide: MatDialog,       useValue: dialogSpy },
        { provide: MatSnackBar,     useValue: snackBarSpy },
        { provide: Router,          useValue: routerSpy }
      ]
    }).compileComponents();

    fixture   = TestBed.createComponent(EmployeeListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('initialisation', () => {
    it('calls list() on init', () => {
      expect(employeeServiceSpy.list).toHaveBeenCalled();
    });

    it('populates employees array on success', () => {
      expect(component.employees.length).toBe(1);
      expect(component.employees[0].fullName).toBe('Alice Smith');
    });

    it('sets loading to false after successful load', () => {
      expect(component.loading).toBeFalse();
    });
  });

  describe('loadEmployees() — error handling', () => {
    it('sets loading to false and shows snack bar on error', () => {
      employeeServiceSpy.list.and.returnValue(throwError(() => new Error('server error')));
      component.loadEmployees();
      expect(component.loading).toBeFalse();
      expect(snackBarSpy.open).toHaveBeenCalledWith('Failed to load employees', 'Dismiss', jasmine.any(Object));
    });
  });

  describe('applyFilter()', () => {
    it('merges partial filter into current filter and reloads', () => {
      component.applyFilter({ department: 'Product', page: 0 });
      expect(employeeServiceSpy.list).toHaveBeenCalledTimes(2); // init + applyFilter
      const lastCallArgs = employeeServiceSpy.list.calls.mostRecent().args[0];
      expect(lastCallArgs?.department).toBe('Product');
    });
  });

  describe('onPageChange()', () => {
    it('updates page and pageSize from PageEvent', () => {
      component.onPageChange({ pageIndex: 2, pageSize: 50, length: 100 });
      const lastCallArgs = employeeServiceSpy.list.calls.mostRecent().args[0];
      expect(lastCallArgs?.page).toBe(2);
      expect(lastCallArgs?.pageSize).toBe(50);
    });
  });

  describe('viewDetail()', () => {
    it('navigates to /employees/:id', () => {
      component.viewDetail('emp-1');
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/employees', 'emp-1']);
    });
  });

  describe('search debounce', () => {
    it('calls applyFilter after debounce when search value changes', fakeAsync(() => {
      const initialCallCount = employeeServiceSpy.list.calls.count();
      component.searchControl.setValue('alice');
      tick(300);
      expect(employeeServiceSpy.list.calls.count()).toBeGreaterThan(initialCallCount);
    }));
  });

  describe('openCreateDialog()', () => {
    it('opens the EmployeeFormDialog and reloads on close with result', () => {
      dialogSpy.open.and.returnValue({ afterClosed: () => of(true) } as any);
      component.openCreateDialog();
      expect(dialogSpy.open).toHaveBeenCalled();
      // reload triggered by afterClosed result
      expect(employeeServiceSpy.list.calls.count()).toBeGreaterThan(1);
    });

    it('does not reload when dialog is closed without result', () => {
      dialogSpy.open.and.returnValue({ afterClosed: () => of(null) } as any);
      const countBefore = employeeServiceSpy.list.calls.count();
      component.openCreateDialog();
      expect(employeeServiceSpy.list.calls.count()).toBe(countBefore);
    });
  });

  describe('openEditDialog()', () => {
    it('opens the EmployeeFormDialog pre-filled with employee and reloads on close with result', () => {
      dialogSpy.open.and.returnValue({ afterClosed: () => of(true) } as any);
      const event = new MouseEvent('click');
      spyOn(event, 'stopPropagation');

      component.openEditDialog(ALICE, event);

      expect(event.stopPropagation).toHaveBeenCalled();
      expect(dialogSpy.open).toHaveBeenCalledWith(jasmine.any(Function), {
        width: '520px',
        data: { employee: ALICE }
      });
      expect(employeeServiceSpy.list.calls.count()).toBeGreaterThan(1);
    });

    it('does not reload when edit dialog is cancelled', () => {
      dialogSpy.open.and.returnValue({ afterClosed: () => of(null) } as any);
      const event = new MouseEvent('click');
      const countBefore = employeeServiceSpy.list.calls.count();

      component.openEditDialog(ALICE, event);

      expect(employeeServiceSpy.list.calls.count()).toBe(countBefore);
    });
  });

  describe('onFilterChange()', () => {
    it('reads all filter controls, resets page to 0, and reloads', () => {
      component.departmentControl.setValue('Engineering');
      component.countryControl.setValue('US');
      component.statusControl.setValue('ACTIVE');

      component.onFilterChange();

      const lastCallArgs = employeeServiceSpy.list.calls.mostRecent().args[0];
      expect(lastCallArgs?.department).toBe('Engineering');
      expect(lastCallArgs?.country).toBe('US');
      expect(lastCallArgs?.status).toBe('ACTIVE');
      expect(lastCallArgs?.page).toBe(0);
    });
  });

  describe('template rendering states', () => {
    it('renders empty state component when no employees are found and not loading', () => {
      component.employees = [];
      component.loading = false;
      fixture.detectChanges();

      const emptyState = fixture.nativeElement.querySelector('app-empty-state');
      expect(emptyState).toBeTruthy();
    });

    it('renders table rows when employees exist and not loading', () => {
      component.employees = [ALICE];
      component.loading = false;
      fixture.detectChanges();

      const rows = fixture.nativeElement.querySelectorAll('tr[mat-row]');
      expect(rows.length).toBe(1);
    });

    it('renders loading spinner overlay when loading is true', () => {
      component.loading = true;
      fixture.detectChanges();

      const spinner = fixture.nativeElement.querySelector('.loading-overlay mat-spinner');
      expect(spinner).toBeTruthy();
    });
  });

  describe('deactivate()', () => {
    it('calls deactivate service after confirm dialog returns true', () => {
      dialogSpy.open.and.returnValue({ afterClosed: () => of(true) } as any);
      employeeServiceSpy.deactivate.and.returnValue(of({ ...ALICE, status: 'INACTIVE' }));
      const event = new MouseEvent('click');
      component.deactivate(ALICE, event);
      expect(employeeServiceSpy.deactivate).toHaveBeenCalledWith('emp-1');
    });

    it('does not call deactivate when confirm dialog returns false', () => {
      dialogSpy.open.and.returnValue({ afterClosed: () => of(false) } as any);
      const event = new MouseEvent('click');
      component.deactivate(ALICE, event);
      expect(employeeServiceSpy.deactivate).not.toHaveBeenCalled();
    });

    it('shows snack bar on deactivation error', () => {
      dialogSpy.open.and.returnValue({ afterClosed: () => of(true) } as any);
      employeeServiceSpy.deactivate.and.returnValue(throwError(() => new Error('error')));
      const event = new MouseEvent('click');
      component.deactivate(ALICE, event);
      expect(snackBarSpy.open).toHaveBeenCalledWith('Failed to deactivate', 'Dismiss', jasmine.any(Object));
    });
  });

  describe('ngOnDestroy()', () => {
    it('completes destroy$ subject without error', () => {
      expect(() => component.ngOnDestroy()).not.toThrow();
    });
  });
});
