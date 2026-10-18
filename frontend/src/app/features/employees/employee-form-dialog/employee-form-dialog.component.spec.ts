import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

import { EmployeeFormDialogComponent } from './employee-form-dialog.component';
import { EmployeeService } from '../../../core/services/employee.service';
import { Employee } from '../../../core/models/employee.model';

const MOCK_EMPLOYEE: Employee = {
  id: 'emp-1',
  fullName: 'Alice Smith',
  email: 'alice@acme.com',
  department: 'Engineering',
  jobTitle: 'Software Engineer',
  country: 'US',
  status: 'ACTIVE',
  createdAt: '2025-01-01T00:00:00Z',
  updatedAt: '2025-01-01T00:00:00Z'
};

describe('EmployeeFormDialogComponent — create mode', () => {
  let fixture: ComponentFixture<EmployeeFormDialogComponent>;
  let component: EmployeeFormDialogComponent;
  let employeeServiceSpy: jasmine.SpyObj<EmployeeService>;
  let dialogRefSpy: jasmine.SpyObj<MatDialogRef<EmployeeFormDialogComponent>>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    employeeServiceSpy = jasmine.createSpyObj('EmployeeService', ['create', 'update']);
    dialogRefSpy       = jasmine.createSpyObj('MatDialogRef', ['close']);
    snackBarSpy        = jasmine.createSpyObj('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      imports: [EmployeeFormDialogComponent],
      providers: [
        provideAnimationsAsync(),
        { provide: EmployeeService, useValue: employeeServiceSpy },
        { provide: MatDialogRef,    useValue: dialogRefSpy },
        { provide: MatSnackBar,     useValue: snackBarSpy },
        { provide: MAT_DIALOG_DATA, useValue: { employee: null } }
      ]
    }).compileComponents();

    fixture   = TestBed.createComponent(EmployeeFormDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('sets isEdit to false when no employee is provided', () => {
    expect(component.isEdit).toBeFalse();
  });

  it('initialises form fields as empty in create mode', () => {
    expect(component.form.value.fullName).toBe('');
    expect(component.form.value.email).toBe('');
    expect(component.form.value.department).toBe('');
    expect(component.form.value.jobTitle).toBe('');
    expect(component.form.value.country).toBe('');
  });

  it('form is invalid when required fields are empty', () => {
    expect(component.form.invalid).toBeTrue();
  });

  it('form is valid when all required fields are filled correctly', () => {
    component.form.setValue({
      fullName: 'Alice Smith', email: 'alice@acme.com',
      department: 'Engineering', jobTitle: 'SWE', country: 'US'
    });
    expect(component.form.valid).toBeTrue();
  });

  it('form is invalid when email is malformed', () => {
    component.form.setValue({
      fullName: 'Alice', email: 'not-an-email',
      department: 'Eng', jobTitle: 'SWE', country: 'US'
    });
    expect(component.form.get('email')?.hasError('email')).toBeTrue();
  });

  it('does not call create when form is invalid', () => {
    component.save();
    expect(employeeServiceSpy.create).not.toHaveBeenCalled();
  });

  it('calls create and closes dialog on success', () => {
    employeeServiceSpy.create.and.returnValue(of(MOCK_EMPLOYEE));
    component.form.setValue({
      fullName: 'Alice Smith', email: 'alice@acme.com',
      department: 'Engineering', jobTitle: 'SWE', country: 'US'
    });
    component.save();
    expect(employeeServiceSpy.create).toHaveBeenCalled();
    expect(dialogRefSpy.close).toHaveBeenCalledWith(true);
  });

  it('shows snack bar on create error and does not close dialog', () => {
    employeeServiceSpy.create.and.returnValue(throwError(() => new Error('server error')));
    component.form.setValue({
      fullName: 'Alice Smith', email: 'alice@acme.com',
      department: 'Engineering', jobTitle: 'SWE', country: 'US'
    });
    component.save();
    expect(snackBarSpy.open).toHaveBeenCalled();
    expect(dialogRefSpy.close).not.toHaveBeenCalled();
    expect(component.saving).toBeFalse();
  });
});

describe('EmployeeFormDialogComponent — edit mode', () => {
  let fixture: ComponentFixture<EmployeeFormDialogComponent>;
  let component: EmployeeFormDialogComponent;
  let employeeServiceSpy: jasmine.SpyObj<EmployeeService>;
  let dialogRefSpy: jasmine.SpyObj<MatDialogRef<EmployeeFormDialogComponent>>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    employeeServiceSpy = jasmine.createSpyObj('EmployeeService', ['create', 'update']);
    dialogRefSpy       = jasmine.createSpyObj('MatDialogRef', ['close']);
    snackBarSpy        = jasmine.createSpyObj('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      imports: [EmployeeFormDialogComponent],
      providers: [
        provideAnimationsAsync(),
        { provide: EmployeeService, useValue: employeeServiceSpy },
        { provide: MatDialogRef,    useValue: dialogRefSpy },
        { provide: MatSnackBar,     useValue: snackBarSpy },
        { provide: MAT_DIALOG_DATA, useValue: { employee: MOCK_EMPLOYEE } }
      ]
    }).compileComponents();

    fixture   = TestBed.createComponent(EmployeeFormDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('sets isEdit to true when an employee is provided', () => {
    expect(component.isEdit).toBeTrue();
  });

  it('pre-fills form fields with the existing employee values', () => {
    expect(component.form.value.fullName).toBe('Alice Smith');
    expect(component.form.value.email).toBe('alice@acme.com');
    expect(component.form.value.department).toBe('Engineering');
  });

  it('calls update (not create) on save', () => {
    employeeServiceSpy.update.and.returnValue(of(MOCK_EMPLOYEE));
    component.save();
    expect(employeeServiceSpy.update).toHaveBeenCalledWith('emp-1', jasmine.any(Object));
    expect(employeeServiceSpy.create).not.toHaveBeenCalled();
  });

  it('closes dialog with true after successful update', () => {
    employeeServiceSpy.update.and.returnValue(of(MOCK_EMPLOYEE));
    component.save();
    expect(dialogRefSpy.close).toHaveBeenCalledWith(true);
  });
});
