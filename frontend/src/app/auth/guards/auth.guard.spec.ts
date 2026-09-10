import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';

import { AuthService } from '../services/auth.service';
import { authGuard } from './auth.guard';

describe('authGuard', () => {
  let authenticated = false;
  let router: Router;

  beforeEach(() => {
    authenticated = false;

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            isAuthenticated: () => authenticated,
          },
        },
      ],
    });

    router = TestBed.inject(Router);
  });

  it('should allow authenticated users', () => {
    authenticated = true;

    const result = TestBed.runInInjectionContext(
      () =>
        authGuard(
          {} as ActivatedRouteSnapshot,
          {
            url: '/dashboard',
          } as RouterStateSnapshot,
        ),
    );

    expect(result).toBe(true);
  });

  it('should redirect unauthenticated users to login', () => {
    const result = TestBed.runInInjectionContext(
      () =>
        authGuard(
          {} as ActivatedRouteSnapshot,
          {
            url: '/dashboard',
          } as RouterStateSnapshot,
        ),
    );

    expect(result instanceof UrlTree).toBe(true);

    const tree = result as UrlTree;

    expect(router.serializeUrl(tree)).toContain(
      '/login',
    );

    expect(tree.queryParams['returnUrl']).toBe(
      '/dashboard',
    );
  });
});