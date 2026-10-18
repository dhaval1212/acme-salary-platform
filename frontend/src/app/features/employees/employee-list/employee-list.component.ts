import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatFormFieldModule } from '@angular/material/form-field';

import { EmployeeService } from '../../../core/services/employee.service';
import { Employee, EmployeeFilter, EmployeeStatus } from '../../../core/models/employee.model';
import { PageMeta } from '../../../core/models/api-response.model';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { EmployeeFormDialogComponent } from '../employee-form-dialog/employee-form-dialog.component';

@Component({
  selector: 'app-employee-list',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatTableModule, MatPaginatorModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatCardModule, MatDialogModule,
    MatProgressSpinnerModule, MatTooltipModule, MatSnackBarModule, MatFormFieldModule,
    PageHeaderComponent, StatusBadgeComponent, EmptyStateComponent,
  ],
  templateUrl: './employee-list.component.html',
  styleUrl: './employee-list.component.scss'
})
export class EmployeeListComponent implements OnInit, OnDestroy {
  employees: Employee[] = [];
  pageMeta: PageMeta = { page: 0, pageSize: 20, totalElements: 0, totalPages: 0 };
  loading = false;

  displayedColumns = ['fullName', 'email', 'department', 'jobTitle', 'country', 'status', 'actions'];

  searchControl = new FormControl('');
  departmentControl = new FormControl('');
  countryControl = new FormControl('');
  statusControl = new FormControl<EmployeeStatus | ''>('');

  private destroy$ = new Subject<void>();
  private currentFilter: EmployeeFilter = { page: 0, pageSize: 20 };

  constructor(
    private employeeService: EmployeeService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadEmployees();
    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(v => this.applyFilter({ search: v ?? '', page: 0 }));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadEmployees(): void {
    this.loading = true;
    this.employeeService.list(this.currentFilter).pipe(takeUntil(this.destroy$)).subscribe({
      next: res => {
        this.employees = res.data;
        this.pageMeta = res.meta;
        this.loading = false;
      },
      error: () => {
        this.snackBar.open('Failed to load employees', 'Dismiss', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  applyFilter(partial: Partial<EmployeeFilter>): void {
    this.currentFilter = { ...this.currentFilter, ...partial };
    this.loadEmployees();
  }

  onFilterChange(): void {
    this.applyFilter({
      department: this.departmentControl.value ?? '',
      country: this.countryControl.value ?? '',
      status: (this.statusControl.value as EmployeeStatus) || undefined,
      page: 0,
    });
  }

  onPageChange(event: PageEvent): void {
    this.applyFilter({ page: event.pageIndex, pageSize: event.pageSize });
  }

  openCreateDialog(): void {
    const ref = this.dialog.open(EmployeeFormDialogComponent, {
      width: '520px',
      data: { employee: null }
    });
    ref.afterClosed().subscribe(result => { if (result) this.loadEmployees(); });
  }

  openEditDialog(employee: Employee, event: Event): void {
    event.stopPropagation();
    const ref = this.dialog.open(EmployeeFormDialogComponent, {
      width: '520px',
      data: { employee }
    });
    ref.afterClosed().subscribe(result => { if (result) this.loadEmployees(); });
  }

  viewDetail(id: string): void {
    this.router.navigate(['/employees', id]);
  }

  deactivate(employee: Employee, event: Event): void {
    event.stopPropagation();
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Deactivate Employee',
        message: `Deactivate ${employee.fullName}? Their compensation history will be preserved.`,
        confirmLabel: 'Deactivate',
      }
    });
    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.employeeService.deactivate(employee.id).pipe(takeUntil(this.destroy$)).subscribe({
        next: () => {
          this.snackBar.open(`${employee.fullName} deactivated`, 'OK', { duration: 3000 });
          this.loadEmployees();
        },
        error: () => this.snackBar.open('Failed to deactivate', 'Dismiss', { duration: 3000 })
      });
    });
  }
}
