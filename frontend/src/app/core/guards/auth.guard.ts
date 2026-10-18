import { inject } from '@angular/core';
import { CanActivateFn, Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Route guard that prevents unauthenticated navigation to protected routes.
 *
 * On failure: redirects to /login with returnUrl query param so the user
 * can return to their intended destination after authenticating.
 *
 * IMPORTANT: This guard is a UX measure only. The backend independently
 * enforces authentication on every API request via the JWT filter.
 */
export const authGuard: CanActivateFn = (
  _route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
) => {
  const authService = inject(AuthService);
  const router      = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  // Preserve the intended URL so we can redirect back after login
  return router.createUrlTree(['/login'], {
    queryParams: { returnUrl: state.url }
  });
};
