import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { CompensationService } from '../../../core/services/compensation.service';

export interface CompensationFormDialogData {
  employeeId: string;
  employeeName: string;
}

@Component({
  selector: 'app-compensation-form-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule, MatButtonModule, MatInputModule, MatFormFieldModule,
    MatDatepickerModule, MatNativeDateModule, MatProgressSpinnerModule,
  ],
  templateUrl: './compensation-form-dialog.component.html',
})
export class CompensationFormDialogComponent implements OnInit {
  form!: FormGroup;
  saving = false;

  constructor(
    private fb: FormBuilder,
    private compensationService: CompensationService,
    private snackBar: MatSnackBar,
    public dialogRef: MatDialogRef<CompensationFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CompensationFormDialogData
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      amount:        [null, [Validators.required, Validators.min(0)]],
      currency:      ['USD', [Validators.required, Validators.pattern(/^[A-Z]{3}$/)]],
      effectiveDate: [new Date(), Validators.required],
      changedBy:     ['hr-manager@acme.com', [Validators.required, Validators.maxLength(255)]],
    });
  }

  save(): void {
    if (this.form.invalid) return;
    this.saving = true;
    const raw = this.form.value;
    const request = {
      ...raw,
      currency: (raw.currency as string).toUpperCase(),
      effectiveDate: this.formatDate(raw.effectiveDate as Date),
    };
    this.compensationService.record(this.data.employeeId, request).subscribe({
      next: () => {
        this.snackBar.open('Compensation recorded', 'OK', { duration: 3000 });
        this.dialogRef.close(true);
      },
      error: () => {
        this.snackBar.open('Failed to record compensation', 'Dismiss', { duration: 3000 });
        this.saving = false;
      }
    });
  }

  private formatDate(date: Date): string {
    return date.toISOString().split('T')[0];
  }
}
