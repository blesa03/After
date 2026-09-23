import {
  ComponentFixture,
  TestBed,
} from '@angular/core/testing';
import {
  ActivatedRoute,
  Router,
  convertToParamMap,
} from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { AuthService } from '../../services/auth.service';
import { Register } from './register';

describe('Register', () => {
  let component:
    Register;

  let fixture:
    ComponentFixture<Register>;

  let authServiceSpy: {
    registerAndLogin:
      ReturnType<typeof vi.fn>;
  };

  let routerSpy: {
    navigateByUrl:
      ReturnType<typeof vi.fn>;
  };

  async function configure(
    returnUrl:
      string | null = null,
  ): Promise<void> {
    authServiceSpy = {
      registerAndLogin:
        vi.fn(),
    };

    routerSpy = {
      navigateByUrl:
        vi.fn(),
    };

    authServiceSpy
      .registerAndLogin
      .mockReturnValue(
        of(void 0),
      );

    await TestBed
      .configureTestingModule({
        imports: [
          Register,
        ],

        providers: [
          {
            provide:
              AuthService,

            useValue:
              authServiceSpy,
          },

          {
            provide:
              Router,

            useValue:
              routerSpy,
          },

          {
            provide:
              ActivatedRoute,

            useValue: {
              snapshot: {
                queryParamMap:
                  convertToParamMap(
                    returnUrl
                      ? {
                          returnUrl,
                        }
                      : {},
                  ),
              },
            },
          },
        ],
      })
      .compileComponents();

    fixture =
      TestBed.createComponent(
        Register,
      );

    component =
      fixture.componentInstance;

    fixture.detectChanges();
  }

  afterEach(() => {
    TestBed.resetTestingModule();
  });

  it('should register and navigate to the dashboard by default', async () => {
    await configure();

    component.form.setValue({
      email:
        'user@example.com',

      password:
        'password123',

      confirmPassword:
        'password123',
    });

    component.submit();

    expect(
      authServiceSpy
        .registerAndLogin,
    ).toHaveBeenCalledWith({
      email:
        'user@example.com',

      password:
        'password123',
    });

    expect(
      routerSpy.navigateByUrl,
    ).toHaveBeenCalledWith(
      '/dashboard',
    );
  });

  it('should return to the invitation after registration', async () => {
    await configure(
      '/invitations/test-token',
    );

    component.form.setValue({
      email:
        'user@example.com',

      password:
        'password123',

      confirmPassword:
        'password123',
    });

    component.submit();

    expect(
      routerSpy.navigateByUrl,
    ).toHaveBeenCalledWith(
      '/invitations/test-token',
    );
  });

  it('should reject an external return URL', async () => {
    await configure(
      'https://evil.example.com',
    );

    component.form.setValue({
      email:
        'user@example.com',

      password:
        'password123',

      confirmPassword:
        'password123',
    });

    component.submit();

    expect(
      routerSpy.navigateByUrl,
    ).toHaveBeenCalledWith(
      '/dashboard',
    );
  });

  it('should reject a protocol-relative return URL', async () => {
    await configure(
      '//evil.example.com',
    );

    component.form.setValue({
      email:
        'user@example.com',

      password:
        'password123',

      confirmPassword:
        'password123',
    });

    component.submit();

    expect(
      routerSpy.navigateByUrl,
    ).toHaveBeenCalledWith(
      '/dashboard',
    );
  });

  it('should not submit when passwords do not match', async () => {
    await configure();

    component.form.setValue({
      email:
        'user@example.com',

      password:
        'password123',

      confirmPassword:
        'different123',
    });

    component.submit();

    expect(
      authServiceSpy
        .registerAndLogin,
    ).not.toHaveBeenCalled();
  });
});