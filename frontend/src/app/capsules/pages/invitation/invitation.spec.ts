import {
  ComponentFixture,
  TestBed,
} from '@angular/core/testing';
import {
  ActivatedRoute,
  Router,
  convertToParamMap,
} from '@angular/router';
import {
  HttpErrorResponse,
} from '@angular/common/http';
import {
  of,
  throwError,
} from 'rxjs';
import { vi } from 'vitest';

import { AuthService } from '../../../auth/services/auth.service';
import { CapsuleService } from '../../services/capsule.service';
import { InvitationPage } from './invitation';

describe('InvitationPage', () => {
  let component:
    InvitationPage;

  let fixture:
    ComponentFixture<InvitationPage>;

  let capsuleServiceSpy: {
    getInvitation:
      ReturnType<typeof vi.fn>;

    acceptInvitation:
      ReturnType<typeof vi.fn>;
  };

  let routerSpy: {
    navigate:
      ReturnType<typeof vi.fn>;

    url: string;
  };

  let authenticated = false;

  const invitationPreview = {
    capsuleTitle:
      'Shared memories',

    ownerEmail:
      'owner@example.com',

    opensAt:
      '2026-12-01T18:00:00Z',
  };

  beforeEach(async () => {
    authenticated = false;

    capsuleServiceSpy = {
      getInvitation:
        vi.fn(),

      acceptInvitation:
        vi.fn(),
    };

    routerSpy = {
      navigate:
        vi.fn(),

      url:
        '/invitations/test-token',
    };

    capsuleServiceSpy
      .getInvitation
      .mockReturnValue(
        of(invitationPreview),
      );

    await TestBed
      .configureTestingModule({
        imports: [
          InvitationPage,
        ],

        providers: [
          {
            provide:
              CapsuleService,

            useValue:
              capsuleServiceSpy,
          },

          {
            provide:
              AuthService,

            useValue: {
              isAuthenticated:
                () =>
                  authenticated,
            },
          },

          {
            provide:
              ActivatedRoute,

            useValue: {
              snapshot: {
                paramMap:
                  convertToParamMap({
                    token:
                      'test-token',
                  }),
              },
            },
          },

          {
            provide:
              Router,

            useValue:
              routerSpy,
          },
        ],
      })
      .compileComponents();

    fixture =
      TestBed.createComponent(
        InvitationPage,
      );

    component =
      fixture.componentInstance;
  });

  function render(): void {
    fixture.detectChanges();
  }

  function httpError(
    status: number,
  ): HttpErrorResponse {
    return new HttpErrorResponse({
      status,
      statusText:
        'Test error',
    });
  }

  it('should load the invitation preview', () => {
    render();

    expect(
      capsuleServiceSpy
        .getInvitation,
    ).toHaveBeenCalledWith(
      'test-token',
    );

    expect(
      component.invitation(),
    ).toEqual(
      invitationPreview,
    );

    expect(
      component.token(),
    ).toBe(
      'test-token',
    );
  });

  it('should render invitation information', () => {
    render();

    const text =
      fixture.nativeElement
        .textContent;

    expect(text).toContain(
      'Shared memories',
    );

    expect(text).toContain(
      'owner@example.com',
    );
  });

  it('should stop loading after the invitation loads', () => {
    render();

    expect(
      component.isLoading(),
    ).toBe(false);
  });

  it('should show authentication options to an unauthenticated user', () => {
    authenticated = false;

    render();

    const text =
      fixture.nativeElement
        .textContent;

    expect(text).toContain(
      'Iniciar sesión',
    );

    expect(text).toContain(
      'Crear cuenta',
    );
  });

  it('should build the login URL with the invitation return URL', () => {
    render();

    expect(
      component.loginUrl(),
    ).toBe(
      '/login?returnUrl=' +
        encodeURIComponent(
          '/invitations/test-token',
        ),
    );
  });

  it('should build the register URL with the invitation return URL', () => {
    render();

    expect(
      component.registerUrl(),
    ).toBe(
      '/register?returnUrl=' +
        encodeURIComponent(
          '/invitations/test-token',
        ),
    );
  });

  it('should show an error for an invalid invitation', () => {
    capsuleServiceSpy
      .getInvitation
      .mockReturnValue(
        throwError(
          () =>
            httpError(404),
        ),
      );

    render();

    expect(
      component.errorMessage(),
    ).toBe(
      'Esta invitación no existe o el enlace no es válido.',
    );

    expect(
      component.isLoading(),
    ).toBe(false);
  });

  it('should show an error when the invitation is no longer available', () => {
    capsuleServiceSpy
      .getInvitation
      .mockReturnValue(
        throwError(
          () =>
            httpError(400),
        ),
      );

    render();

    expect(
      component.errorMessage(),
    ).toBe(
      'Esta invitación ya no está disponible. Puede que haya sido utilizada, revocada o que la cápsula ya esté sellada.',
    );

    expect(
      component.isLoading(),
    ).toBe(false);
  });

  it('should show a generic error when loading the invitation fails', () => {
    capsuleServiceSpy
      .getInvitation
      .mockReturnValue(
        throwError(
          () =>
            httpError(500),
        ),
      );

    render();

    expect(
      component.errorMessage(),
    ).toBe(
      'No se ha podido cargar la invitación.',
    );

    expect(
      component.isLoading(),
    ).toBe(false);
  });

  it('should not accept the invitation when the user is unauthenticated', () => {
    authenticated = false;

    render();

    component.accept();

    expect(
      capsuleServiceSpy
        .acceptInvitation,
    ).not.toHaveBeenCalled();
  });

  it('should allow an authenticated user to accept the invitation', () => {
    authenticated = true;

    capsuleServiceSpy
      .acceptInvitation
      .mockReturnValue(
        of({
          capsuleId:
            'capsule-123',

          role:
            'CONTRIBUTOR',
        }),
      );

    render();

    component.accept();

    expect(
      capsuleServiceSpy
        .acceptInvitation,
    ).toHaveBeenCalledWith(
      'test-token',
    );
  });

  it('should navigate directly to the capsule after accepting', () => {
    authenticated = true;

    capsuleServiceSpy
      .acceptInvitation
      .mockReturnValue(
        of({
          capsuleId:
            'capsule-123',

          role:
            'CONTRIBUTOR',
        }),
      );

    render();

    component.accept();

    expect(
      routerSpy.navigate,
    ).toHaveBeenCalledWith([
      '/capsules',
      'capsule-123',
    ]);

    expect(
      component.isAccepting(),
    ).toBe(false);
  });

  it('should show an error when the invitation cannot be accepted', () => {
    authenticated = true;

    capsuleServiceSpy
      .acceptInvitation
      .mockReturnValue(
        throwError(
          () =>
            httpError(400),
        ),
      );

    render();

    component.accept();

    expect(
      component.errorMessage(),
    ).toBe(
      'No se puede aceptar esta invitación. Puede que ya se haya utilizado, la cápsula esté sellada o ya seas miembro.',
    );

    expect(
      component.isAccepting(),
    ).toBe(false);
  });

  it('should show a generic error when accepting fails', () => {
    authenticated = true;

    capsuleServiceSpy
      .acceptInvitation
      .mockReturnValue(
        throwError(
          () =>
            httpError(500),
        ),
      );

    render();

    component.accept();

    expect(
      component.errorMessage(),
    ).toBe(
      'No se ha podido aceptar la invitación.',
    );

    expect(
      component.isAccepting(),
    ).toBe(false);
  });

  it('should clear a previous error before accepting', () => {
    authenticated = true;

    capsuleServiceSpy
      .acceptInvitation
      .mockReturnValue(
        of({
          capsuleId:
            'capsule-123',

          role:
            'CONTRIBUTOR',
        }),
      );

    render();

    component.errorMessage.set(
      'Previous error',
    );

    component.accept();

    expect(
      component.errorMessage(),
    ).toBeNull();
  });
});