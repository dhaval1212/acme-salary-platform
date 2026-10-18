import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { HttpClient, provideHttpClient, withInterceptors, HttpErrorResponse } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';
import { environment } from '../../../environments/environment';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authService: AuthService;
  let logoutSpy: jasmine.Spy;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService);
    logoutSpy = spyOn(authService, 'logout').and.callThrough();
  });

  afterEach(() => httpMock.verify());

  describe('token attachment', () => {
    it('attaches Bearer token to API requests when authenticated', () => {
      // Simulate logged-in state
      (authService as any).tokenSubject.next('valid-jwt-token');

      http.get(`${environment.apiBaseUrl}/employees`).subscribe();
      const req = httpMock.expectOne(r => r.url.includes('/employees'));
      expect(req.request.headers.get('Authorization')).toBe('Bearer valid-jwt-token');
      req.flush({});
    });

    it('does not attach token when not authenticated', () => {
      http.get(`${environment.apiBaseUrl}/employees`).subscribe();
      const req = httpMock.expectOne(r => r.url.includes('/employees'));
      expect(req.request.headers.get('Authorization')).toBeNull();
      req.flush({});
    });

    it('does not attach token to non-API requests', () => {
      (authService as any).tokenSubject.next('valid-jwt-token');

      http.get('https://example.com/external').subscribe();
      const req = httpMock.expectOne('https://example.com/external');
      expect(req.request.headers.get('Authorization')).toBeNull();
      req.flush({});
    });

    it('bypasses /auth/login without attaching token even when a token exists', () => {
      (authService as any).tokenSubject.next('existing-token');

      http.post(`${environment.apiBaseUrl}/auth/login`, {}).subscribe();
      const req = httpMock.expectOne(r => r.url.includes('/auth/login'));
      expect(req.request.headers.get('Authorization')).toBeNull();
      req.flush({});
    });

    it('bypasses /auth/logout without attaching token even when a token exists', () => {
      (authService as any).tokenSubject.next('existing-token');

      http.post(`${environment.apiBaseUrl}/auth/logout`, {}).subscribe();
      const req = httpMock.expectOne(r => r.url.includes('/auth/logout'));
      expect(req.request.headers.get('Authorization')).toBeNull();
      req.flush({});
    });
  });

  describe('401 handling', () => {
    it('calls logout on 401 from API endpoint', () => {
      (authService as any).tokenSubject.next('expired-token');

      http.get(`${environment.apiBaseUrl}/employees`).subscribe({
        error: () => {
          expect(logoutSpy).toHaveBeenCalled();
        }
      });
      const req = httpMock.expectOne(r => r.url.includes('/employees'));
      req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });
    });

    it('does NOT call logout on 403 from API endpoint', () => {
      (authService as any).tokenSubject.next('valid-token');

      http.get(`${environment.apiBaseUrl}/employees`).subscribe({
        error: () => {
          expect(logoutSpy).not.toHaveBeenCalled();
        }
      });
      const req = httpMock.expectOne(r => r.url.includes('/employees'));
      req.flush({ message: 'Forbidden' }, { status: 403, statusText: 'Forbidden' });
    });

    it('does not call logout on 401 from non-API endpoint', () => {
      (authService as any).tokenSubject.next('valid-token');

      http.get('https://external.com/api').subscribe({
        error: () => {
          expect(logoutSpy).not.toHaveBeenCalled();
        }
      });
      const req = httpMock.expectOne('https://external.com/api');
      req.flush({}, { status: 401, statusText: 'Unauthorized' });
    });

    it('does not call logout on 401 from /auth/login endpoint', () => {
      http.post(`${environment.apiBaseUrl}/auth/login`, {}).subscribe({
        error: () => {
          expect(logoutSpy).not.toHaveBeenCalled();
        }
      });
      const req = httpMock.expectOne(r => r.url.includes('/auth/login'));
      req.flush({ message: 'Invalid credentials' }, { status: 401, statusText: 'Unauthorized' });
    });

    it('does not call logout on 401 from /auth/logout endpoint', () => {
      http.post(`${environment.apiBaseUrl}/auth/logout`, {}).subscribe({
        error: () => {
          expect(logoutSpy).not.toHaveBeenCalled();
        }
      });
      const req = httpMock.expectOne(r => r.url.includes('/auth/logout'));
      req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });
    });

    it('re-throws the error so components can handle it', (done) => {
      (authService as any).tokenSubject.next('valid-token');

      http.get(`${environment.apiBaseUrl}/employees`).subscribe({
        error: (err: HttpErrorResponse) => {
          expect(err.status).toBe(403);
          done();
        }
      });
      const req = httpMock.expectOne(r => r.url.includes('/employees'));
      req.flush({ message: 'Forbidden' }, { status: 403, statusText: 'Forbidden' });
    });
  });
});
