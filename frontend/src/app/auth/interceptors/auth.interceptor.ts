import {
  HttpErrorResponse,
  HttpInterceptorFn,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import {
  catchError,
  switchMap,
  throwError,
} from 'rxjs';

import { API_BASE_URL } from '../../core/config/api.config';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const apiBaseUrl = inject(API_BASE_URL);

  const isApiRequest = request.url.startsWith(apiBaseUrl);

  const isAuthLifecycleRequest = [
    `${apiBaseUrl}/auth/register`,
    `${apiBaseUrl}/auth/login`,
    `${apiBaseUrl}/auth/refresh`,
    `${apiBaseUrl}/auth/logout`,
  ].some((url) => request.url.startsWith(url));

  const accessToken = authService.accessToken();

  const authenticatedRequest =
    isApiRequest &&
    !isAuthLifecycleRequest &&
    accessToken !== null
      ? request.clone({
          setHeaders: {
            Authorization: `Bearer ${accessToken}`,
          },
        })
      : request;

  return next(authenticatedRequest).pipe(
    catchError((error: unknown) => {
      const shouldRefresh =
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        isApiRequest &&
        !isAuthLifecycleRequest &&
        accessToken !== null;

      if (!shouldRefresh) {
        return throwError(() => error);
      }

      return authService.refreshAccessToken().pipe(
        switchMap((newAccessToken) =>
          next(
            request.clone({
              setHeaders: {
                Authorization: `Bearer ${newAccessToken}`,
              },
            }),
          ),
        ),
        catchError((refreshError: unknown) => {
          const returnUrl = router.url;

          void router.navigate(['/login'], {
            queryParams:
              returnUrl !== '/login'
                ? { returnUrl }
                : undefined,
          });

          return throwError(() => refreshError);
        }),
      );
    }),
  );
};