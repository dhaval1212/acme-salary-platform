import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

import {
  CompensationFormDialogComponent,
  CompensationFormDialogData
} from './compensation-form-dialog.component';
import { CompensationService } from '../../../core/services/compensation.service';
import { Compensation } from '../../../core/models/compensation.model';

const MOCK_DATA: CompensationFormDialogData = {
  employeeId: 'emp-1',
  employeeName: 'Alice Smith'
};

const MOCK_COMPENSATION: Compensation = {
  id: 'comp-1',
  employeeId: 'emp-1',
  amount: 95000,
  currency: 'USD',
  effectiveDate: '2026-02-01',
  changedBy: 'hr-manager@acme.com',
  createdAt: '2026-02-01T00:00:00Z'
};

describe('CompensationFormDialogComponent', () => {
  let fixture: ComponentFixture<CompensationFormDialogComponent>;
  let component: CompensationFormDialogComponent;
  let compensationServiceSpy: jasmine.SpyObj<CompensationService>;
  let dialogRefSpy: jasmine.SpyObj<MatDialogRef<CompensationFormDialogComponent>>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    compensationServiceSpy = jasmine.createSpyObj('CompensationService', ['record']);
    dialogRefSpy           = jasmine.createSpyObj('MatDialogRef', ['close']);
    snackBarSpy            = jasmine.createSpyObj('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      imports: [CompensationFormDialogComponent],
      providers: [
        provideAnimationsAsync(),
        { provide: CompensationService, useValue: compensationServiceSpy },
        { provide: MatDialogRef,        useValue: dialogRefSpy },
        { provide: MatSnackBar,         useValue: snackBarSpy },
        { provide: MAT_DIALOG_DATA,     useValue: MOCK_DATA }
      ]
    }).compileComponents();

    fixture   = TestBed.createComponent(CompensationFormDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('initialisation', () => {
    it('initialises form with default currency USD and hr-manager changedBy', () => {
      expect(component.form.get('currency')?.value).toBe('USD');
      expect(component.form.get('changedBy')?.value).toBe('hr-manager@acme.com');
      expect(component.form.get('amount')?.value).toBeNull();
      expect(component.form.get('effectiveDate')?.value).toBeInstanceOf(Date);
    });

    it('form is invalid initially because amount is null', () => {
      expect(component.form.invalid).toBeTrue();
      expect(component.form.get('amount')?.hasError('required')).toBeTrue();
    });
  });

  describe('validation', () => {
    it('marks amount invalid if negative', () => {
      component.form.patchValue({ amount: -100 });
      expect(component.form.get('amount')?.hasError('min')).toBeTrue();
      expect(component.form.invalid).toBeTrue();
    });

    it('marks amount valid for 0 or positive numbers', () => {
      component.form.patchValue({ amount: 0 });
      expect(component.form.get('amount')?.valid).toBeTrue();

      component.form.patchValue({ amount: 120000 });
      expect(component.form.get('amount')?.valid).toBeTrue();
    });

    it('validates 3-letter currency code pattern', () => {
      component.form.patchValue({ currency: 'US' });
      expect(component.form.get('currency')?.hasError('pattern')).toBeTrue();

      component.form.patchValue({ currency: 'USDD' });
      expect(component.form.get('currency')?.hasError('pattern')).toBeTrue();

      component.form.patchValue({ currency: 'EUR' });
      expect(component.form.get('currency')?.valid).toBeTrue();
    });

    it('requires changedBy field with max length 255', () => {
      component.form.patchValue({ changedBy: '' });
      expect(component.form.get('changedBy')?.hasError('required')).toBeTrue();

      component.form.patchValue({ changedBy: 'a'.repeat(256) });
      expect(component.form.get('changedBy')?.hasError('maxlength')).toBeTrue();
    });
  });

  describe('save()', () => {
    it('does not submit when form is invalid', () => {
      component.form.patchValue({ amount: null });
      component.save();
      expect(compensationServiceSpy.record).not.toHaveBeenCalled();
      expect(component.saving).toBeFalse();
    });

    it('formats date and uppercase currency, calls record(), and closes dialog on success', () => {
      compensationServiceSpy.record.and.returnValue(of(MOCK_COMPENSATION));
      const testDate = new Date('2026-03-15T00:00:00Z');

      component.form.setValue({
        amount: 95000,
        currency: 'EUR',
        effectiveDate: testDate,
        changedBy: 'admin@acme.com'
      });

      component.save();

      expect(component.saving).toBeTrue();
      expect(compensationServiceSpy.record).toHaveBeenCalledWith('emp-1', {
        amount: 95000,
        currency: 'EUR',
        effectiveDate: testDate.toISOString().split('T')[0],
        changedBy: 'admin@acme.com'
      });
      expect(snackBarSpy.open).toHaveBeenCalledWith('Compensation recorded', 'OK', jasmine.any(Object));
      expect(dialogRefSpy.close).toHaveBeenCalledWith(true);
    });

    it('shows error snackbar and resets saving flag when service fails', () => {
      compensationServiceSpy.record.and.returnValue(throwError(() => new Error('Server error')));

      component.form.setValue({
        amount: 95000,
        currency: 'USD',
        effectiveDate: new Date('2026-03-15'),
        changedBy: 'admin@acme.com'
      });

      component.save();

      expect(snackBarSpy.open).toHaveBeenCalledWith(
        'Failed to record compensation',
        'Dismiss',
        jasmine.any(Object)
      );
      expect(dialogRefSpy.close).not.toHaveBeenCalled();
      expect(component.saving).toBeFalse();
    });
  });
});
