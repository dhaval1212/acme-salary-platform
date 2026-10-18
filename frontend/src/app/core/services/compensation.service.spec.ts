import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { CompensationService } from './compensation.service';
import { ApiService } from './api.service';
import { Compensation } from '../models/compensation.model';

const MOCK_COMP: Compensation = {
  id: 'comp-1',
  employeeId: 'emp-1',
  amount: 90000,
  currency: 'USD',
  effectiveDate: '2026-01-01',
  changedBy: 'hr@acme.com',
  createdAt: '2026-01-01T00:00:00Z'
};

describe('CompensationService', () => {
  let service: CompensationService;
  let apiSpy: jasmine.SpyObj<ApiService>;

  beforeEach(() => {
    apiSpy = jasmine.createSpyObj<ApiService>('ApiService', ['get', 'post']);

    TestBed.configureTestingModule({
      providers: [CompensationService, { provide: ApiService, useValue: apiSpy }]
    });
    service = TestBed.inject(CompensationService);
  });

  describe('getCurrent()', () => {
    it('calls the correct endpoint and returns current compensation', (done) => {
      apiSpy.get.and.returnValue(of({ data: MOCK_COMP }));
      service.getCurrent('emp-1').subscribe(comp => {
        expect(comp.amount).toBe(90000);
        expect(comp.currency).toBe('USD');
        done();
      });
      expect(apiSpy.get).toHaveBeenCalledWith('/employees/emp-1/compensation');
    });
  });

  describe('getHistory()', () => {
    it('calls the history endpoint and returns array', (done) => {
      apiSpy.get.and.returnValue(of({ data: [MOCK_COMP] }));
      service.getHistory('emp-1').subscribe(history => {
        expect(history.length).toBe(1);
        expect(history[0].effectiveDate).toBe('2026-01-01');
        done();
      });
      expect(apiSpy.get).toHaveBeenCalledWith('/employees/emp-1/compensation/history');
    });

    it('returns an empty array when there is no history', (done) => {
      apiSpy.get.and.returnValue(of({ data: [] }));
      service.getHistory('emp-1').subscribe(history => {
        expect(history.length).toBe(0);
        done();
      });
    });
  });

  describe('record()', () => {
    it('posts to the correct endpoint and returns recorded compensation', (done) => {
      apiSpy.post.and.returnValue(of({ data: MOCK_COMP }));
      const request = { amount: 90000, currency: 'USD', effectiveDate: '2026-01-01', changedBy: 'hr@acme.com' };
      service.record('emp-1', request).subscribe(comp => {
        expect(comp.id).toBe('comp-1');
        done();
      });
      expect(apiSpy.post).toHaveBeenCalledWith('/employees/emp-1/compensation', request);
    });
  });
});
