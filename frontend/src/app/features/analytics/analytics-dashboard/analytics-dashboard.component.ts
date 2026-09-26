import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { Subject, takeUntil } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AnalyticsService } from '../../../core/services/analytics.service';
import { DepartmentStats } from '../../../core/models/analytics.model';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { CurrencyDisplayComponent } from '../../../shared/components/currency-display/currency-display.component';

@Component({
  selector: 'app-analytics-dashboard',
  standalone: true,
  imports: [
    ReactiveFormsModule, DecimalPipe,
    MatCardModule, MatTableModule, MatInputModule, MatButtonModule,
    MatIconModule, MatFormFieldModule, MatProgressSpinnerModule,
    PageHeaderComponent, EmptyStateComponent, CurrencyDisplayComponent,
  ],
  templateUrl: './analytics-dashboard.component.html',
  styleUrl: './analytics-dashboard.component.scss'
})
export class AnalyticsDashboardComponent implements OnInit, OnDestroy {
  stats: DepartmentStats[] = [];
  loading = false;

  departmentControl = new FormControl('');
  countryControl = new FormControl('');

  displayedColumns = ['department', 'headcount', 'avgSalary', 'medianSalary', 'minSalary', 'maxSalary'];

  private destroy$ = new Subject<void>();

  constructor(
    private analyticsService: AnalyticsService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void { this.load(); }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  load(): void {
    this.loading = true;
    this.analyticsService.getDepartmentStats({
      department: this.departmentControl.value || undefined,
      country: this.countryControl.value || undefined,
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: data => { this.stats = data; this.loading = false; },
      error: () => {
        this.snackBar.open('Failed to load analytics', 'Dismiss', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  get totalHeadcount(): number {
    return this.stats.reduce((sum, d) => sum + d.headcount, 0);
  }

  get overallAvgSalary(): number {
    if (!this.stats.length) return 0;
    const totalWeighted = this.stats.reduce((sum, d) => sum + d.avgSalary * d.headcount, 0);
    return totalWeighted / this.totalHeadcount;
  }
}
