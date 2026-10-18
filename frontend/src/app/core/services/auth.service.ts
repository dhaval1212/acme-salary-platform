import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { LoginRequest, LoginResponse, AuthUserProfile } from '../models/auth.model';
import { ApiResponse } from '../models/api-response.model';

/**
 * Central authentication service.
 *
 * Token storage: in-memory (private BehaviorSubject) only.
 * This avoids XSS-accessible localStorage. The trade-off is that a page
 * reload clears the session and the user must log in again — which is
 * acceptable for a short-lived (1-hour) access token on an HR tool.
 *
 * Passwords, tokens, and signing secrets are never logged.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly loginUrl  = `${environment.apiBaseUrl}/auth/login`;
  private readonly logoutUrl = `${environment.apiBaseUrl}/auth/logout`;

  // Private — token is never exposed directly; only isAuthenticated() and
  // getToken() provide controlled access.
  private readonly tokenSubject   = new BehaviorSubject<string | null>(null);
  private readonly currentUserSubject = new BehaviorSubject<AuthUserProfile | null>(null);

  /** Observable of the current user profile (null when not authenticated). */
  readonly currentUser$ = this.currentUserSubject.asObservable();

  /** Observable boolean authentication state. */
  readonly isAuthenticated$ = new BehaviorSubject<boolean>(false);

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  /**
   * Submits login credentials to the backend.
   * On success, stores the token in memory and updates auth state.
   * Never logs the password or token value.
   */
  login(request: LoginRequest): Observable<ApiResponse<LoginResponse>> {
    return this.http.post<ApiResponse<LoginResponse>>(this.loginUrl, request).pipe(
      tap(response => {
        if (response.data) {
          this.setSession(response.data);
        }
      })
    );
  }

  /**
   * Clears all in-memory auth state and optionally notifies backend.
   * After calling this, getToken() returns null and isAuthenticated() returns false.
   */
  logout(notifyBackend = true): void {
    const token = this.tokenSubject.getValue();
    if (notifyBackend && token) {
      this.http.post(this.logoutUrl, {}).subscribe({
        next: () => {},
        error: () => {}
      });
    }
    this.tokenSubject.next(null);
    this.currentUserSubject.next(null);
    this.isAuthenticated$.next(false);
    this.router.navigate(['/login']);
  }

  /** Returns the current bearer token, or null if not authenticated. */
  getToken(): string | null {
    return this.tokenSubject.getValue();
  }

  /** Returns the current user profile, or null if not authenticated. */
  getCurrentUser(): AuthUserProfile | null {
    return this.currentUserSubject.getValue();
  }

  /** True if a token is currently held in memory. */
  isAuthenticated(): boolean {
    return this.tokenSubject.getValue() !== null;
  }

  // ── Private ──────────────────────────────────────────────────────────────

  private setSession(loginResponse: LoginResponse): void {
    this.tokenSubject.next(loginResponse.token);
    this.currentUserSubject.next(loginResponse.user);
    this.isAuthenticated$.next(true);
  }
}
