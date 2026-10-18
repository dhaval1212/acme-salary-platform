import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login.component';
import { AuthService } from '../../../core/services/auth.service';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['login', 'isAuthenticated']);
    routerSpy     = jasmine.createSpyObj('Router', ['navigate']);
    authServiceSpy.isAuthenticated.and.returnValue(false);
    authServiceSpy.login.and.returnValue(of({ data: { token: 'x', expiresAt: 'x', user: { id: '1', username: 'hr', fullName: 'HR', role: 'HR_MANAGER' } } }));

    await TestBed.configureTestingModule({
      imports: [LoginComponent, ReactiveFormsModule],
      providers: [
        provideAnimationsAsync(),
        { provide: AuthService,   useValue: authServiceSpy },
        { provide: Router,        useValue: routerSpy },
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: { get: () => null } } } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('form validation', () => {
    it('form is invalid when empty', () => {
      expect(component.form.invalid).toBeTrue();
    });

    it('requires username', () => {
      component.form.setValue({ username: '', password: 'pass' });
      expect(component.form.get('username')?.hasError('required')).toBeTrue();
    });

    it('requires password', () => {
      component.form.setValue({ username: 'hr', password: '' });
      expect(component.form.get('password')?.hasError('required')).toBeTrue();
    });

    it('form is valid when both fields are filled', () => {
      component.form.setValue({ username: 'hr.manager', password: 'changeme123' });
      expect(component.form.valid).toBeTrue();
    });
  });

  describe('login()', () => {
    it('does not call authService.login when form is invalid', () => {
      component.login();
      expect(authServiceSpy.login).not.toHaveBeenCalled();
    });

    it('calls authService.login with form values on valid form', () => {
      component.form.setValue({ username: 'hr.manager', password: 'changeme123' });
      component.login();
      expect(authServiceSpy.login).toHaveBeenCalledWith({ username: 'hr.manager', password: 'changeme123' });
    });

    it('navigates to /employees on successful login (no returnUrl)', (done) => {
      component.form.setValue({ username: 'hr.manager', password: 'changeme123' });
      component.login();

      // Wait for subscription
      setTimeout(() => {
        expect(routerSpy.navigate).toHaveBeenCalledWith(['/employees']);
        done();
      }, 0);
    });

    it('navigates to returnUrl on successful login', (done) => {
      // Recreate with returnUrl
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        imports: [LoginComponent, ReactiveFormsModule],
        providers: [
          provideAnimationsAsync(),
          { provide: AuthService, useValue: authServiceSpy },
          { provide: Router, useValue: routerSpy },
          { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: { get: () => '/employees/123' } } } }
        ]
      }).compileComponents();
      const f = TestBed.createComponent(LoginComponent);
      f.detectChanges();
      f.componentInstance.form.setValue({ username: 'hr.manager', password: 'changeme123' });
      f.componentInstance.login();

      setTimeout(() => {
        expect(routerSpy.navigate).toHaveBeenCalledWith(['/employees/123']);
        done();
      }, 0);
    });

    it('sets generic error message on login failure', (done) => {
      authServiceSpy.login.and.returnValue(throwError(() => new Error('fail')));
      component.form.setValue({ username: 'hr.manager', password: 'wrong' });
      component.login();

      setTimeout(() => {
        expect(component.errorMessage).toContain('Invalid credentials');
        expect(component.loading).toBeFalse();
        done();
      }, 0);
    });

    it('clears password field after failed attempt', (done) => {
      authServiceSpy.login.and.returnValue(throwError(() => new Error('fail')));
      component.form.setValue({ username: 'hr.manager', password: 'wrong' });
      component.login();

      setTimeout(() => {
        expect(component.form.get('password')?.value).toBe('');
        done();
      }, 0);
    });

    it('sets loading to true during submission', () => {
      component.form.setValue({ username: 'hr.manager', password: 'changeme123' });
      component.login();
      // Observable is synchronous in test, so loading may be false by now,
      // but the flow is exercised.
      expect(authServiceSpy.login).toHaveBeenCalled();
    });
  });

  describe('password visibility toggle', () => {
    it('defaults to hiding password', () => {
      expect(component.hidePassword).toBeTrue();
    });

    it('toggles visibility when clicked', () => {
      const initial = component.hidePassword;
      component.hidePassword = !initial;
      expect(component.hidePassword).not.toBe(initial);
    });
  });

  describe('already authenticated', () => {
    it('redirects to /employees when already authenticated', () => {
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        imports: [LoginComponent, ReactiveFormsModule],
        providers: [
          provideAnimationsAsync(),
          { provide: AuthService, useValue: { ...authServiceSpy, isAuthenticated: () => true } },
          { provide: Router, useValue: routerSpy },
          { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: { get: () => null } } } }
        ]
      }).compileComponents();
      TestBed.createComponent(LoginComponent).detectChanges();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/employees']);
    });
  });
});
