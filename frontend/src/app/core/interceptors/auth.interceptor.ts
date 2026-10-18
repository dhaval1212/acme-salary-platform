import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { environment } from '../../../environments/environment';

/**
 * HTTP interceptor that:
 * 1. Attaches the bearer token to requests targeting the API base URL.
 *    The token is NOT attached to third-party origins.
 * 2. On 401 Unauthorized: clears auth state and redirects to login.
 * 3. On 403 Forbidden: passes the error through WITHOUT logging out —
 *    the user is authenticated but lacks permission for this resource.
 */
export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
) => {
  // Bypass auth endpoints: user does not have or need a token for login/logout,
  // and 401 on login indicates invalid credentials rather than an expired session
  if (req.url.includes('/auth/login') || req.url.includes('/auth/logout')) {
    return next(req);
  }

  const authService = inject(AuthService);
  const token = authService.getToken();

  // Only attach token to requests going to our own API — never third parties
  const isApiRequest = req.url.startsWith(environment.apiBaseUrl) ||
                       req.url.startsWith('/api/');

  const authReq = (token && isApiRequest)
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && isApiRequest) {
        // Token expired or invalid — clear state and redirect without notifying backend
        authService.logout(false);
      }
      // 403: authenticated but forbidden — do NOT log out
      // Let the component handle the 403 error display
      return throwError(() => error);
    })
  );
};
