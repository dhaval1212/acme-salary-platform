import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { AuthService } from './auth.service';
import { LoginResponse } from '../models/auth.model';
import { ApiResponse } from '../models/api-response.model';

const MOCK_LOGIN_RESPONSE: ApiResponse<LoginResponse> = {
  data: {
    token: 'header.payload.signature',
    expiresAt: new Date(Date.now() + 3600000).toISOString(),
    user: { id: '00000000-0000-0000-0000-000000000001', username: 'hr.manager', fullName: 'HR Manager', role: 'HR_MANAGER' }
  }
};

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AuthService, provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  describe('initial state', () => {
    it('is not authenticated on initialisation', () => {
      expect(service.isAuthenticated()).toBeFalse();
    });

    it('returns null token initially', () => {
      expect(service.getToken()).toBeNull();
    });

    it('returns null user initially', () => {
      expect(service.getCurrentUser()).toBeNull();
    });
  });

  describe('login()', () => {
    it('sets authentication state on successful login', (done) => {
      service.login({ username: 'hr.manager', password: 'changeme123' }).subscribe(() => {
        expect(service.isAuthenticated()).toBeTrue();
        expect(service.getToken()).toBe('header.payload.signature');
        expect(service.getCurrentUser()?.username).toBe('hr.manager');
        done();
      });
      const req = httpMock.expectOne(r => r.url.includes('/auth/login'));
      req.flush(MOCK_LOGIN_RESPONSE);
    });

    it('posts to the login endpoint', () => {
      service.login({ username: 'hr.manager', password: 'pass' }).subscribe();
      const req = httpMock.expectOne(r => r.url.includes('/auth/login'));
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ username: 'hr.manager', password: 'pass' });
      req.flush(MOCK_LOGIN_RESPONSE);
    });

    it('does not change auth state on login failure', (done) => {
      service.login({ username: 'hr.manager', password: 'wrong' }).subscribe({
        error: () => {
          expect(service.isAuthenticated()).toBeFalse();
          expect(service.getToken()).toBeNull();
          done();
        }
      });
      const req = httpMock.expectOne(r => r.url.includes('/auth/login'));
      req.flush({ errors: [{ field: 'global', message: 'Invalid credentials' }] }, { status: 401, statusText: 'Unauthorized' });
    });

    it('updates isAuthenticated$ observable on login', (done) => {
      const states: boolean[] = [];
      service.isAuthenticated$.subscribe(v => states.push(v));

      service.login({ username: 'hr.manager', password: 'pass' }).subscribe(() => {
        expect(states).toContain(true);
        done();
      });

      const req = httpMock.expectOne(r => r.url.includes('/auth/login'));
      req.flush(MOCK_LOGIN_RESPONSE);
    });
  });

  describe('logout()', () => {
    it('clears all auth state', (done) => {
      service.login({ username: 'hr.manager', password: 'pass' }).subscribe(() => {
        service.logout();
        expect(service.isAuthenticated()).toBeFalse();
        expect(service.getToken()).toBeNull();
        expect(service.getCurrentUser()).toBeNull();
        done();
      });
      const req = httpMock.expectOne(r => r.url.includes('/auth/login'));
      req.flush(MOCK_LOGIN_RESPONSE);
    });
  });

  describe('currentUser$', () => {
    it('emits null initially', (done) => {
      service.currentUser$.subscribe(user => {
        expect(user).toBeNull();
        done();
      });
    });

    it('emits user profile after login', (done) => {
      service.login({ username: 'hr.manager', password: 'pass' }).subscribe();
      const req = httpMock.expectOne(r => r.url.includes('/auth/login'));
      req.flush(MOCK_LOGIN_RESPONSE);

      service.currentUser$.subscribe(user => {
        if (user) {
          expect(user.username).toBe('hr.manager');
          expect(user.role).toBe('HR_MANAGER');
          done();
        }
      });
    });
  });
});
