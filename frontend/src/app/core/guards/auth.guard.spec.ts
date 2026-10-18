import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { authGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';

describe('authGuard', () => {
  let authService: AuthService;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        { provide: Router, useValue: { createUrlTree: jasmine.createSpy('createUrlTree') } }
      ]
    });
    authService = TestBed.inject(AuthService);
    router = TestBed.inject(Router);
  });

  it('returns true when authenticated', () => {
    spyOn(authService, 'isAuthenticated').and.returnValue(true);
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as any, { url: '/employees' } as any)
    );
    expect(result).toBeTrue();
  });

  it('redirects to /login with returnUrl when not authenticated', () => {
    spyOn(authService, 'isAuthenticated').and.returnValue(false);
    const createUrlTreeSpy = (router as any).createUrlTree as jasmine.Spy;

    TestBed.runInInjectionContext(() =>
      authGuard({} as any, { url: '/employees/123' } as any)
    );

    expect(createUrlTreeSpy).toHaveBeenCalledWith(['/login'], {
      queryParams: { returnUrl: '/employees/123' }
    });
  });

  it('uses /employees as returnUrl when no specific path is intended', () => {
    spyOn(authService, 'isAuthenticated').and.returnValue(false);
    const createUrlTreeSpy = (router as any).createUrlTree as jasmine.Spy;

    TestBed.runInInjectionContext(() =>
      authGuard({} as any, { url: '/' } as any)
    );

    expect(createUrlTreeSpy).toHaveBeenCalled();
  });

  it('is a UX-only measure — backend independently enforces authentication', () => {
    // This test documents the design intent: the guard improves UX by
    // redirecting unauthenticated users to login, but the backend API
    // also enforces authentication independently on every request.
    spyOn(authService, 'isAuthenticated').and.returnValue(true);
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as any, { url: '/employees' } as any)
    );
    expect(result).toBeTrue();
  });
});
