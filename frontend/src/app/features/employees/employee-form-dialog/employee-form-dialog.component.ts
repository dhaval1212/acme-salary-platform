import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { EmployeeService } from '../../../core/services/employee.service';
import { Employee } from '../../../core/models/employee.model';

export interface EmployeeFormDialogData {
  employee: Employee | null;
}

@Component({
  selector: 'app-employee-form-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule, MatButtonModule, MatInputModule,
    MatFormFieldModule, MatProgressSpinnerModule,
  ],
  templateUrl: './employee-form-dialog.component.html',
})
export class EmployeeFormDialogComponent implements OnInit {
  form!: FormGroup;
  saving = false;
  isEdit: boolean;

  constructor(
    private fb: FormBuilder,
    private employeeService: EmployeeService,
    private snackBar: MatSnackBar,
    public dialogRef: MatDialogRef<EmployeeFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: EmployeeFormDialogData
  ) {
    this.isEdit = !!data.employee;
  }

  ngOnInit(): void {
    this.form = this.fb.group({
      fullName:   [this.data.employee?.fullName   ?? '', [Validators.required, Validators.maxLength(255)]],
      email:      [this.data.employee?.email      ?? '', [Validators.required, Validators.email, Validators.maxLength(255)]],
      department: [this.data.employee?.department ?? '', [Validators.required, Validators.maxLength(100)]],
      jobTitle:   [this.data.employee?.jobTitle   ?? '', [Validators.required, Validators.maxLength(100)]],
      country:    [this.data.employee?.country    ?? '', [Validators.required, Validators.maxLength(100)]],
    });
  }

  save(): void {
    if (this.form.invalid) return;
    this.saving = true;
    const request = this.form.value;
    const op = this.isEdit
      ? this.employeeService.update(this.data.employee!.id, request)
      : this.employeeService.create(request);

    op.subscribe({
      next: () => {
        this.snackBar.open(this.isEdit ? 'Employee updated' : 'Employee created', 'OK', { duration: 3000 });
        this.dialogRef.close(true);
      },
      error: () => {
        this.snackBar.open('Failed to save employee', 'Dismiss', { duration: 3000 });
        this.saving = false;
      }
    });
  }
}
