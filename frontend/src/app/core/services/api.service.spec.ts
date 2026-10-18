import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ApiService } from './api.service';

describe('ApiService', () => {
  let service: ApiService;
  let httpMock: HttpTestingController;

  // environment.apiBaseUrl is '/api/v1' in the test environment
  const BASE = '/api/v1';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ApiService, provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  describe('get()', () => {
    it('calls the correct URL', () => {
      service.get('/employees').subscribe();
      const req = httpMock.expectOne(`${BASE}/employees`);
      expect(req.request.method).toBe('GET');
      req.flush({});
    });

    it('appends defined params as query string', () => {
      service.get('/employees', { page: 0, pageSize: 20, department: 'Engineering' }).subscribe();
      const req = httpMock.expectOne(r => r.url === `${BASE}/employees`);
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('pageSize')).toBe('20');
      expect(req.request.params.get('department')).toBe('Engineering');
      req.flush({});
    });

    it('omits undefined and empty-string params', () => {
      service.get('/employees', { department: undefined, search: '' }).subscribe();
      const req = httpMock.expectOne(`${BASE}/employees`);
      expect(req.request.params.has('department')).toBeFalse();
      expect(req.request.params.has('search')).toBeFalse();
      req.flush({});
    });
  });

  describe('post()', () => {
    it('sends a POST with the correct body', () => {
      const body = { fullName: 'Alice' };
      service.post('/employees', body).subscribe();
      const req = httpMock.expectOne(`${BASE}/employees`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(body);
      req.flush({});
    });
  });

  describe('put()', () => {
    it('sends a PUT with the correct body', () => {
      const body = { fullName: 'Alice Updated' };
      service.put('/employees/1', body).subscribe();
      const req = httpMock.expectOne(`${BASE}/employees/1`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(body);
      req.flush({});
    });
  });

  describe('patch()', () => {
    it('sends a PATCH to the correct URL', () => {
      service.patch('/employees/1/deactivate').subscribe();
      const req = httpMock.expectOne(`${BASE}/employees/1/deactivate`);
      expect(req.request.method).toBe('PATCH');
      req.flush({});
    });

    it('sends an empty object when no body is provided', () => {
      service.patch('/employees/1/deactivate').subscribe();
      const req = httpMock.expectOne(`${BASE}/employees/1/deactivate`);
      expect(req.request.body).toEqual({});
      req.flush({});
    });
  });
});
