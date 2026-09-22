import { TestBed } from '@angular/core/testing';
import {
  provideRouter,
  Router,
} from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';

import { AuthService } from './auth/services/auth.service';
import { CapsuleService } from './capsules/services/capsule.service';
import { routes } from './app.routes';

describe('Application routes', () => {
  let harness:
    RouterTestingHarness;

  let router:
    Router;

  const authStub = {
    isAuthenticated: () => true,

    currentUser: () => ({
      id:
        '8f421bd7-8d46-4f04-a213-1da035a79fd6',

      email:
        'test@example.com',

      createdAt:
        '2026-09-10T18:00:00Z',
    }),

    login:
      () => of(void 0),

    registerAndLogin:
      () => of(void 0),

    logout:
      () => of(void 0),
  };

  const capsuleServiceStub = {
    getCapsules:
      () => of([]),

    getCapsule:
      () =>
        of({
          id:
            'capsule-123',

          title:
            'Test capsule',

          description:
            'Test description',

          type:
            'PERSONAL' as const,

          status:
            'COLLECTING' as const,

          opensAt:
            new Date(
              Date.now() +
                86_400_000,
            ).toISOString(),

          timezone:
            'Europe/Madrid',

          sealedAt:
            null,

          openedAt:
            null,

          createdAt:
            '2026-09-20T10:00:00Z',

          updatedAt:
            '2026-09-20T10:00:00Z',

          role:
            'OWNER' as const,
        }),
  };

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),

        {
          provide:
            AuthService,

          useValue:
            authStub,
        },

        {
          provide:
            CapsuleService,

          useValue:
            capsuleServiceStub,
        },
      ],
    });

    harness =
      await RouterTestingHarness.create();

    router =
      TestBed.inject(
        Router,
      );
  });

  it('should redirect the root route to the dashboard', async () => {
    await harness.navigateByUrl(
      '/',
    );

    expect(
      router.url,
    ).toBe(
      '/dashboard',
    );

    expect(
      harness
        .routeNativeElement
        ?.textContent,
    ).toContain(
      'Mis Cápsulas',
    );
  });

  it('should render the login page', async () => {
    await harness.navigateByUrl(
      '/login',
    );

    expect(
      router.url,
    ).toBe(
      '/login',
    );

    expect(
      harness
        .routeNativeElement
        ?.textContent,
    ).toContain(
      'Sign in',
    );
  });

  it('should render the registration page', async () => {
    await harness.navigateByUrl(
      '/register',
    );

    expect(
      router.url,
    ).toBe(
      '/register',
    );

    expect(
      harness
        .routeNativeElement
        ?.textContent,
    ).toContain(
      'Create account',
    );
  });

  it('should render the dashboard inside the application layout', async () => {
    await harness.navigateByUrl(
      '/dashboard',
    );

    expect(
      router.url,
    ).toBe(
      '/dashboard',
    );

    expect(
      harness
        .routeNativeElement
        ?.textContent,
    ).toContain(
      'After',
    );

    expect(
      harness
        .routeNativeElement
        ?.textContent,
    ).toContain(
      'Mis Cápsulas',
    );
  });

  it('should render the create capsule page', async () => {
    await harness.navigateByUrl(
      '/capsules/create',
    );

    expect(
      router.url,
    ).toBe(
      '/capsules/create',
    );

    expect(
      harness
        .routeNativeElement
        ?.textContent,
    ).toContain(
      'Crear cápsula',
    );
  });

  it('should render the capsule detail page', async () => {
    await harness.navigateByUrl(
      '/capsules/capsule-123',
    );

    expect(
      router.url,
    ).toBe(
      '/capsules/capsule-123',
    );

    expect(
      harness
        .routeNativeElement
        ?.textContent,
    ).toContain(
      'Test capsule',
    );
  });

  it('should render the not found page for an unknown route', async () => {
    await harness.navigateByUrl(
      '/this-route-does-not-exist',
    );

    expect(
      harness
        .routeNativeElement
        ?.textContent,
    ).toContain(
      '404',
    );

    expect(
      harness
        .routeNativeElement
        ?.textContent,
    ).toContain(
      'Page not found',
    );
  });
});