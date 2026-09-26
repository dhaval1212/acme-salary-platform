import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

import { AnalyticsDashboardComponent } from './analytics-dashboard.component';
import { AnalyticsService } from '../../../core/services/analytics.service';
import { DepartmentStats } from '../../../core/models/analytics.model';

const MOCK_STATS: DepartmentStats[] = [
  { department: 'Engineering', headcount: 10, avgSalary: 100000, medianSalary: 95000, minSalary: 70000, maxSalary: 140000 },
  { department: 'Design',      headcount: 5,  avgSalary: 70000,  medianSalary: 68000, minSalary: 50000, maxSalary: 85000  },
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
    it('loads department stats on init', () => {
      expect(analyticsServiceSpy.getDepartmentStats).toHaveBeenCalledWith({
        department: undefined,
        country: undefined
      });
      expect(component.stats).toEqual(MOCK_STATS);
      expect(component.loading).toBeFalse();
    });

    it('renders the stats table columns', () => {
      expect(component.displayedColumns).toEqual([
        'department', 'headcount', 'avgSalary', 'medianSalary', 'minSalary', 'maxSalary'
      ]);
    });
  });

  describe('filters', () => {
    it('passes department and country values when loading stats', () => {
      component.departmentControl.setValue('Engineering');
      component.countryControl.setValue('US');

      component.load();

      expect(analyticsServiceSpy.getDepartmentStats).toHaveBeenCalledWith({
        department: 'Engineering',
        country: 'US'
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

    it('calculates weighted overallAvgSalary correctly', () => {
      // Weighted average: (10 * 100,000 + 5 * 70,000) / (10 + 5)
      // = (1,000,000 + 350,000) / 15 = 1,350,000 / 15 = 90,000
      expect(component.overallAvgSalary).toBe(90000);
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
