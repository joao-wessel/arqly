import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthScope } from '../auth/auth.models';
import { AuthService } from '../auth/auth.service';

export const authGuard = (scope: AuthScope): CanActivateFn => () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isAuthenticated() && auth.scope() === scope) {
    return true;
  }
  return router.createUrlTree([scope === 'platform' ? '/login/admin' : '/login']);
};
