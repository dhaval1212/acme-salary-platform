import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { Location } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

import { EmployeeDetailComponent } from './employee-detail.component';
import { EmployeeService } from '../../../core/services/employee.service';
import { CompensationService } from '../../../core/services/compensation.service';
import { Employee } from '../../../core/models/employee.model';
import { Compensation } from '../../../core/models/compensation.model';

const ALICE: Employee = {
  id: 'emp-1', fullName: 'Alice Smith', email: 'alice@acme.com',
  department: 'Engineering', jobTitle: 'SWE', country: 'US',
  status: 'ACTIVE', createdAt: '2025-01-01T00:00:00Z', updatedAt: '2025-01-01T00:00:00Z'
};

const COMP: Compensation = {
  id: 'comp-1', employeeId: 'emp-1', amount: 90000, currency: 'USD',
  effectiveDate: '2026-01-01', changedBy: 'hr@acme.com', createdAt: '2026-01-01T00:00:00Z'
};

describe('EmployeeDetailComponent', () => {
  let fixture: ComponentFixture<EmployeeDetailComponent>;
  let component: EmployeeDetailComponent;
  let employeeServiceSpy: jasmine.SpyObj<EmployeeService>;
  let compensationServiceSpy: jasmine.SpyObj<CompensationService>;
  let dialogSpy: jasmine.SpyObj<MatDialog>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;
  let locationSpy: jasmine.SpyObj<Location>;

  beforeEach(async () => {
    employeeServiceSpy     = jasmine.createSpyObj('EmployeeService', ['getById']);
    compensationServiceSpy = jasmine.createSpyObj('CompensationService', ['getCurrent', 'getHistory']);
    dialogSpy              = jasmine.createSpyObj('MatDialog', ['open']);
    snackBarSpy            = jasmine.createSpyObj('MatSnackBar', ['open']);
    locationSpy            = jasmine.createSpyObj('Location', ['back']);

    employeeServiceSpy.getById.and.returnValue(of(ALICE));
    compensationServiceSpy.getCurrent.and.returnValue(of(COMP));
    compensationServiceSpy.getHistory.and.returnValue(of([COMP]));

    await TestBed.configureTestingModule({
      imports: [EmployeeDetailComponent],
      providers: [
        provideAnimationsAsync(),
        { provide: EmployeeService,     useValue: employeeServiceSpy },
        { provide: CompensationService, useValue: compensationServiceSpy },
        { provide: MatDialog,           useValue: dialogSpy },
        { provide: MatSnackBar,         useValue: snackBarSpy },
        { provide: Location,            useValue: locationSpy },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => 'emp-1' } } }
        }
      ]
    }).compileComponents();

    fixture   = TestBed.createComponent(EmployeeDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('initialisation', () => {
    it('loads employee on init', () => {
      expect(employeeServiceSpy.getById).toHaveBeenCalledWith('emp-1');
      expect(component.employee).toEqual(ALICE);
    });

    it('loads current compensation after employee is fetched', () => {
      expect(compensationServiceSpy.getCurrent).toHaveBeenCalledWith('emp-1');
      expect(component.currentCompensation).toEqual(COMP);
    });

    it('loads compensation history after employee is fetched', () => {
      expect(compensationServiceSpy.getHistory).toHaveBeenCalledWith('emp-1');
      expect(component.compensationHistory.length).toBe(1);
    });

    it('sets loading to false after all data is loaded', () => {
      expect(component.loading).toBeFalse();
    });
  });

  describe('error handling', () => {
    it('shows snack bar and sets loading false when employee is not found', async () => {
      employeeServiceSpy.getById.and.returnValue(throwError(() => new Error('not found')));
      await TestBed.resetTestingModule();
      await TestBed.configureTestingModule({
        imports: [EmployeeDetailComponent],
        providers: [
          provideAnimationsAsync(),
          { provide: EmployeeService,     useValue: employeeServiceSpy },
          { provide: CompensationService, useValue: compensationServiceSpy },
          { provide: MatDialog,           useValue: dialogSpy },
          { provide: MatSnackBar,         useValue: snackBarSpy },
          { provide: Location,            useValue: locationSpy },
          { provide: ActivatedRoute,      useValue: { snapshot: { paramMap: { get: () => 'emp-x' } } } }
        ]
      }).compileComponents();
      const f = TestBed.createComponent(EmployeeDetailComponent);
      f.detectChanges();
      expect(snackBarSpy.open).toHaveBeenCalledWith('Employee not found', 'Dismiss', jasmine.any(Object));
      expect(f.componentInstance.loading).toBeFalse();
    });

    it('sets loading false when compensation history call fails', () => {
      compensationServiceSpy.getHistory.and.returnValue(throwError(() => new Error('error')));
      component.loadCompensation('emp-1');
      expect(component.loading).toBeFalse();
    });
  });

  describe('goBack()', () => {
    it('calls location.back()', () => {
      component.goBack();
      expect(locationSpy.back).toHaveBeenCalled();
    });
  });

  describe('openCompensationDialog()', () => {
    it('opens compensation dialog and reloads on close with result', () => {
      dialogSpy.open.and.returnValue({ afterClosed: () => of(true) } as any);
      component.openCompensationDialog();
      expect(dialogSpy.open).toHaveBeenCalled();
      expect(compensationServiceSpy.getCurrent).toHaveBeenCalledTimes(2);
    });

    it('does nothing when employee is not set', () => {
      component.employee = undefined;
      component.openCompensationDialog();
      expect(dialogSpy.open).not.toHaveBeenCalled();
    });
  });

  describe('compensation UI rendering', () => {
    it('renders empty state when currentCompensation is undefined', () => {
      component.currentCompensation = undefined;
      fixture.detectChanges();
      const emptyStates = fixture.nativeElement.querySelectorAll('app-empty-state');
      expect(emptyStates.length).toBeGreaterThan(0);
    });

    it('renders currency display when currentCompensation is present', () => {
      component.currentCompensation = COMP;
      fixture.detectChanges();
      const currencyDisplay = fixture.nativeElement.querySelector('.salary app-currency-display');
      expect(currencyDisplay).toBeTruthy();
    });

    it('renders history table when history items exist', () => {
      component.compensationHistory = [COMP];
      fixture.detectChanges();
      const table = fixture.nativeElement.querySelector('table[mat-table]');
      expect(table).toBeTruthy();
    });
  });
});
