import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

import { AnalyticsDashboardComponent } from './analytics-dashboard.component';
import { AnalyticsService } from '../../../core/services/analytics.service';
import { DepartmentStats } from '../../../core/models/analytics.model';

const MOCK_STATS: DepartmentStats[] = [
  { department: 'Engineering', country: 'United States', currency: 'USD', headcount: 10, avgSalary: 100000, medianSalary: 95000, minSalary: 70000, maxSalary: 140000 },
  { department: 'Design',      country: 'United States', currency: 'USD', headcount: 5,  avgSalary: 70000,  medianSalary: 68000, minSalary: 50000, maxSalary: 85000  },
];

describe('AnalyticsDashboardComponent', () => {
  let fixture: ComponentFixture<AnalyticsDashboardComponent>;
  let component: AnalyticsDashboardComponent;
  let analyticsServiceSpy: jasmine.SpyObj<AnalyticsService>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    analyticsServiceSpy = jasmine.createSpyObj('AnalyticsService', ['getDepartmentStats']);
    snackBarSpy         = jasmine.createSpyObj('MatSnackBar', ['open']);

    analyticsServiceSpy.getDepartmentStats.and.returnValue(of(MOCK_STATS));

    await TestBed.configureTestingModule({
      imports: [AnalyticsDashboardComponent],
      providers: [
        provideAnimationsAsync(),
        { provide: AnalyticsService, useValue: analyticsServiceSpy },
        { provide: MatSnackBar,     useValue: snackBarSpy },
      ]
    }).compileComponents();

    fixture   = TestBed.createComponent(AnalyticsDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('initialisation', () => {
    it('loads department stats on init with default United States country', () => {
      expect(analyticsServiceSpy.getDepartmentStats).toHaveBeenCalledWith({
        department: undefined,
        country: 'United States'
      });
      expect(component.stats).toEqual(MOCK_STATS);
      expect(component.loading).toBeFalse();
    });

    it('renders the stats table columns for a selected country', () => {
      expect(component.displayedColumns).toEqual([
        'department', 'currency', 'headcount', 'avgSalary', 'medianSalary', 'minSalary', 'maxSalary'
      ]);
    });

    it('renders country column when country selection is cleared for country-wise view', () => {
      component.countryControl.setValue('');
      expect(component.displayedColumns).toEqual([
        'department', 'country', 'currency', 'headcount', 'avgSalary', 'medianSalary', 'minSalary', 'maxSalary'
      ]);
    });
  });

  describe('filters', () => {
    it('passes department and country values when loading stats', () => {
      component.departmentControl.setValue('Engineering');
      component.countryControl.setValue('United Kingdom');

      component.load();

      expect(analyticsServiceSpy.getDepartmentStats).toHaveBeenCalledWith({
        department: 'Engineering',
        country: 'United Kingdom'
      });
    });

    it('passes undefined when controls are empty strings', () => {
      component.departmentControl.setValue('');
      component.countryControl.setValue('');

      component.load();

      expect(analyticsServiceSpy.getDepartmentStats).toHaveBeenCalledWith({
        department: undefined,
        country: undefined
      });
    });

    it('determines activeCurrency from selected country or stats', () => {
      component.countryControl.setValue('United Kingdom');
      expect(component.activeCurrency).toBe('GBP');

      component.countryControl.setValue('India');
      expect(component.activeCurrency).toBe('INR');

      component.countryControl.setValue('');
      expect(component.activeCurrency).toBe('USD');
    });
  });

  describe('error handling', () => {
    it('shows snack bar and sets loading to false on service failure', () => {
      analyticsServiceSpy.getDepartmentStats.and.returnValue(throwError(() => new Error('Server error')));

      component.load();

      expect(snackBarSpy.open).toHaveBeenCalledWith(
        'Failed to load analytics',
        'Dismiss',
        jasmine.any(Object)
      );
      expect(component.loading).toBeFalse();
    });
  });

  describe('computed properties (business metrics)', () => {
    it('calculates totalHeadcount by summing headcount across all departments', () => {
      // 10 + 5 = 15
      expect(component.totalHeadcount).toBe(15);
    });

    it('returns totalHeadcount 0 when stats array is empty', () => {
      component.stats = [];
      expect(component.totalHeadcount).toBe(0);
    });

    it('calculates weighted overallAvgSalary correctly for single currency', () => {
      // Weighted average: (10 * 100,000 + 5 * 70,000) / (10 + 5) = 90,000
      expect(component.hasMultipleCurrencies).toBeFalse();
      expect(component.overallAvgSalary).toBe(90000);
    });

    it('never accumulates multi-currency data into overall average and flags hasMultipleCurrencies', () => {
      component.stats = [
        { department: 'Engineering', country: 'United States', currency: 'USD', headcount: 10, avgSalary: 100000, medianSalary: 95000, minSalary: 70000, maxSalary: 140000 },
        { department: 'Engineering', country: 'India', currency: 'INR', headcount: 10, avgSalary: 2500000, medianSalary: 2300000, minSalary: 800000, maxSalary: 6000000 },
      ];
      expect(component.hasMultipleCurrencies).toBeTrue();
      expect(component.overallAvgSalary).toBe(0);
    });

    it('returns overallAvgSalary 0 when stats array is empty to prevent division by zero', () => {
      component.stats = [];
      expect(component.overallAvgSalary).toBe(0);
    });
  });

  describe('lifecycle', () => {
    it('completes destroy$ subject on ngOnDestroy', () => {
      expect(() => component.ngOnDestroy()).not.toThrow();
    });
  });
});
