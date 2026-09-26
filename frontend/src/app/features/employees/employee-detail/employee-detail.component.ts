import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Location, DatePipe } from '@angular/common';
import { Subject, takeUntil } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatDividerModule } from '@angular/material/divider';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { EmployeeService } from '../../../core/services/employee.service';
import { CompensationService } from '../../../core/services/compensation.service';
import { Employee } from '../../../core/models/employee.model';
import { Compensation } from '../../../core/models/compensation.model';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { CurrencyDisplayComponent } from '../../../shared/components/currency-display/currency-display.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { CompensationFormDialogComponent } from '../../compensation/compensation-form-dialog/compensation-form-dialog.component';

@Component({
  selector: 'app-employee-detail',
  standalone: true,
  imports: [
    DatePipe,
    MatCardModule, MatButtonModule, MatIconModule, MatTableModule,
    MatDividerModule, MatDialogModule, MatProgressSpinnerModule, MatSnackBarModule,
    PageHeaderComponent, StatusBadgeComponent, CurrencyDisplayComponent, EmptyStateComponent
  ],
  templateUrl: './employee-detail.component.html',
  styleUrl: './employee-detail.component.scss'
})
export class EmployeeDetailComponent implements OnInit, OnDestroy {
  employee?: Employee;
  currentCompensation?: Compensation;
  compensationHistory: Compensation[] = [];
  loading = true;
  historyColumns = ['effectiveDate', 'amount', 'currency', 'changedBy', 'createdAt'];
  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private location: Location,
    private employeeService: EmployeeService,
    private compensationService: CompensationService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.employeeService.getById(id).pipe(takeUntil(this.destroy$)).subscribe({
      next: emp => {
        this.employee = emp;
        this.loadCompensation(id);
      },
      error: () => {
        this.snackBar.open('Employee not found', 'Dismiss', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadCompensation(id: string): void {
    this.compensationService.getCurrent(id).pipe(takeUntil(this.destroy$)).subscribe({
      next: c => this.currentCompensation = c,
      error: () => {}
    });
    this.compensationService.getHistory(id).pipe(takeUntil(this.destroy$)).subscribe({
      next: h => { this.compensationHistory = h; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  openCompensationDialog(): void {
    if (!this.employee) return;
    const ref = this.dialog.open(CompensationFormDialogComponent, {
      width: '480px',
      data: { employeeId: this.employee.id, employeeName: this.employee.fullName }
    });
    ref.afterClosed().subscribe(result => {
      if (result && this.employee) this.loadCompensation(this.employee.id);
    });
  }

  goBack(): void { this.location.back(); }
}
