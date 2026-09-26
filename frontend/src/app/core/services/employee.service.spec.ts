import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { EmployeeService } from './employee.service';
import { ApiService } from './api.service';
import { Employee } from '../models/employee.model';
import { ApiResponse } from '../models/api-response.model';

const MOCK_EMPLOYEE: Employee = {
  id: 'emp-1',
  fullName: 'Alice Smith',
  email: 'alice@acme.com',
  department: 'Engineering',
  jobTitle: 'Software Engineer',
  country: 'US',
  status: 'ACTIVE',
  createdAt: '2025-01-01T00:00:00Z',
  updatedAt: '2025-01-01T00:00:00Z'
};

const PAGE_META = { page: 0, pageSize: 20, totalElements: 1, totalPages: 1 };

describe('EmployeeService', () => {
  let service: EmployeeService;
  let apiSpy: jasmine.SpyObj<ApiService>;

  beforeEach(() => {
    apiSpy = jasmine.createSpyObj<ApiService>('ApiService', ['get', 'post', 'put', 'patch']);

    TestBed.configureTestingModule({
      providers: [EmployeeService, { provide: ApiService, useValue: apiSpy }]
    });
    service = TestBed.inject(EmployeeService);
  });

  describe('list()', () => {
    it('returns paged response mapped from ApiResponse', (done) => {
      const apiRes: ApiResponse<Employee[]> = { data: [MOCK_EMPLOYEE], meta: PAGE_META };
      apiSpy.get.and.returnValue(of(apiRes));

      service.list({ page: 0, pageSize: 20 }).subscribe(result => {
        expect(result.data.length).toBe(1);
        expect(result.data[0].fullName).toBe('Alice Smith');
        expect(result.meta.totalElements).toBe(1);
        done();
      });
    });

    it('passes filter params to ApiService.get', () => {
      apiSpy.get.and.returnValue(of({ data: [], meta: PAGE_META }));
      service.list({ department: 'Engineering', country: 'US', status: 'ACTIVE', search: 'alice' }).subscribe();
      const callArgs = apiSpy.get.calls.mostRecent().args[1] as Record<string, unknown>;
      expect(callArgs['department']).toBe('Engineering');
      expect(callArgs['country']).toBe('US');
      expect(callArgs['status']).toBe('ACTIVE');
      expect(callArgs['search']).toBe('alice');
    });

    it('defaults page to 0 and pageSize to 20 when not provided', () => {
      apiSpy.get.and.returnValue(of({ data: [], meta: PAGE_META }));
      service.list().subscribe();
      const callArgs = apiSpy.get.calls.mostRecent().args[1] as Record<string, unknown>;
      expect(callArgs['page']).toBe(0);
      expect(callArgs['pageSize']).toBe(20);
    });
  });

  describe('getById()', () => {
    it('returns the employee from the response data', (done) => {
      apiSpy.get.and.returnValue(of({ data: MOCK_EMPLOYEE }));
      service.getById('emp-1').subscribe(emp => {
        expect(emp.id).toBe('emp-1');
        expect(emp.email).toBe('alice@acme.com');
        done();
      });
    });

    it('calls the correct endpoint', () => {
      apiSpy.get.and.returnValue(of({ data: MOCK_EMPLOYEE }));
      service.getById('emp-1').subscribe();
      expect(apiSpy.get).toHaveBeenCalledWith('/employees/emp-1');
    });
  });

  describe('create()', () => {
    it('posts to /employees and returns the created employee', (done) => {
      apiSpy.post.and.returnValue(of({ data: MOCK_EMPLOYEE }));
      const request = { fullName: 'Alice Smith', email: 'alice@acme.com', department: 'Eng', jobTitle: 'SWE', country: 'US' };
      service.create(request).subscribe(emp => {
        expect(emp.id).toBe('emp-1');
        done();
      });
      expect(apiSpy.post).toHaveBeenCalledWith('/employees', request);
    });
  });

  describe('update()', () => {
    it('puts to /employees/:id and returns updated employee', (done) => {
      apiSpy.put.and.returnValue(of({ data: MOCK_EMPLOYEE }));
      const request = { fullName: 'Alice Updated', email: 'alice@acme.com', department: 'Eng', jobTitle: 'SWE', country: 'US' };
      service.update('emp-1', request).subscribe(emp => {
        expect(emp.id).toBe('emp-1');
        done();
      });
      expect(apiSpy.put).toHaveBeenCalledWith('/employees/emp-1', request);
    });
  });

  describe('deactivate()', () => {
    it('patches /employees/:id/deactivate and returns updated employee', (done) => {
      const inactive = { ...MOCK_EMPLOYEE, status: 'INACTIVE' as const };
      apiSpy.patch.and.returnValue(of({ data: inactive }));
      service.deactivate('emp-1').subscribe(emp => {
        expect(emp.status).toBe('INACTIVE');
        done();
      });
      expect(apiSpy.patch).toHaveBeenCalledWith('/employees/emp-1/deactivate');
    });
  });
});
