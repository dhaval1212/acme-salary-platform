import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { AnalyticsService } from './analytics.service';
import { ApiService } from './api.service';
import { DepartmentStats } from '../models/analytics.model';

const MOCK_STATS: DepartmentStats[] = [
  { department: 'Engineering', country: 'United States', currency: 'USD', headcount: 10, avgSalary: 100000, medianSalary: 98000, minSalary: 75000, maxSalary: 130000 },
  { department: 'HR',          country: 'United States', currency: 'USD', headcount: 3,  avgSalary: 65000,  medianSalary: 63000, minSalary: 55000, maxSalary: 70000  }
];

describe('AnalyticsService', () => {
  let service: AnalyticsService;
  let apiSpy: jasmine.SpyObj<ApiService>;

  beforeEach(() => {
    apiSpy = jasmine.createSpyObj<ApiService>('ApiService', ['get']);

    TestBed.configureTestingModule({
      providers: [AnalyticsService, { provide: ApiService, useValue: apiSpy }]
    });
    service = TestBed.inject(AnalyticsService);
  });

  describe('getDepartmentStats()', () => {
    it('returns the stats array from the response', (done) => {
      apiSpy.get.and.returnValue(of({ data: MOCK_STATS }));
      service.getDepartmentStats().subscribe(stats => {
        expect(stats.length).toBe(2);
        expect(stats[0].department).toBe('Engineering');
        expect(stats[0].headcount).toBe(10);
        done();
      });
    });

    it('passes department filter to ApiService.get', () => {
      apiSpy.get.and.returnValue(of({ data: [] }));
      service.getDepartmentStats({ department: 'Engineering' }).subscribe();
      const callArgs = apiSpy.get.calls.mostRecent().args[1] as Record<string, unknown>;
      expect(callArgs['department']).toBe('Engineering');
    });

    it('passes country filter to ApiService.get', () => {
      apiSpy.get.and.returnValue(of({ data: [] }));
      service.getDepartmentStats({ country: 'US' }).subscribe();
      const callArgs = apiSpy.get.calls.mostRecent().args[1] as Record<string, unknown>;
      expect(callArgs['country']).toBe('US');
    });

    it('calls /analytics/departments endpoint', () => {
      apiSpy.get.and.returnValue(of({ data: [] }));
      service.getDepartmentStats().subscribe();
      expect(apiSpy.get.calls.mostRecent().args[0]).toBe('/analytics/departments');
    });

    it('returns empty array when no stats available', (done) => {
      apiSpy.get.and.returnValue(of({ data: [] }));
      service.getDepartmentStats().subscribe(stats => {
        expect(stats).toEqual([]);
        done();
      });
    });
  });
});
