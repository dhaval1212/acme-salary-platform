import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { Subject, takeUntil } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
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

export interface CountryOption {
  code: string;
  name: string;
  currency: string;
}

export const COUNTRIES: CountryOption[] = [
  { code: 'US', name: 'United States', currency: 'USD' },
  { code: 'GB', name: 'United Kingdom', currency: 'GBP' },
  { code: 'DE', name: 'Germany', currency: 'EUR' },
  { code: 'FR', name: 'France', currency: 'EUR' },
  { code: 'CA', name: 'Canada', currency: 'CAD' },
  { code: 'IN', name: 'India', currency: 'INR' },
  { code: 'AU', name: 'Australia', currency: 'AUD' },
  { code: 'SG', name: 'Singapore', currency: 'SGD' }
];

@Component({
  selector: 'app-analytics-dashboard',
  standalone: true,
  imports: [
    ReactiveFormsModule, DecimalPipe,
    MatCardModule, MatTableModule, MatInputModule, MatSelectModule, MatButtonModule,
    MatIconModule, MatFormFieldModule, MatProgressSpinnerModule,
    PageHeaderComponent, EmptyStateComponent, CurrencyDisplayComponent,
  ],
  templateUrl: './analytics-dashboard.component.html',
  styleUrl: './analytics-dashboard.component.scss'
})
export class AnalyticsDashboardComponent implements OnInit, OnDestroy {
  stats: DepartmentStats[] = [];
  loading = false;

  countries = COUNTRIES;

  departmentControl = new FormControl('');
  countryControl = new FormControl('United States');

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

  get displayedColumns(): string[] {
    if (this.countryControl.value) {
      return ['department', 'currency', 'headcount', 'avgSalary', 'medianSalary', 'minSalary', 'maxSalary'];
    }
    return ['department', 'country', 'currency', 'headcount', 'avgSalary', 'medianSalary', 'minSalary', 'maxSalary'];
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

  get hasMultipleCurrencies(): boolean {
    const currencies = new Set(this.stats.map(d => d.currency).filter(Boolean));
    return currencies.size > 1;
  }

  get activeCurrency(): string {
    if (this.countryControl.value) {
      const match = this.countries.find(c => c.name === this.countryControl.value);
      if (match) return match.currency;
    }
    return this.stats[0]?.currency || 'USD';
  }

  get overallAvgSalary(): number {
    if (!this.stats.length || this.hasMultipleCurrencies) return 0;
    const totalWeighted = this.stats.reduce((sum, d) => sum + d.avgSalary * d.headcount, 0);
    return Math.round(totalWeighted / this.totalHeadcount);
  }
}
