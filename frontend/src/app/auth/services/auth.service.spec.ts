import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { API_BASE_URL } from '../../core/config/api.config';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpTesting: HttpTestingController;

  const user = {
    id: '8f421bd7-8d46-4f04-a213-1da035a79fd6',
    email: 'test@example.com',
    createdAt: '2026-09-10T18:00:00Z',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: API_BASE_URL,
          useValue: '/api',
        },
      ],
    });

    service = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(
      HttpTestingController,
    );
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should authenticate and keep the access token in memory', () => {
    service
      .login({
        email: 'test@example.com',
        password: 'password123',
      })
      .subscribe();

    const loginRequest =
      httpTesting.expectOne('/api/auth/login');

    expect(loginRequest.request.method).toBe('POST');
    expect(loginRequest.request.withCredentials).toBe(true);

    loginRequest.flush({
      accessToken: 'access-token-1',
      tokenType: 'Bearer',
      expiresIn: 900,
    });

    const meRequest =
      httpTesting.expectOne('/api/auth/me');

    meRequest.flush(user);

    expect(service.accessToken()).toBe(
      'access-token-1',
    );
    expect(service.currentUser()).toEqual(user);
    expect(service.isAuthenticated()).toBe(true);
  });

  it('should register and then authenticate the user', () => {
    service
      .registerAndLogin({
        email: 'test@example.com',
        password: 'password123',
      })
      .subscribe();

    const registerRequest =
      httpTesting.expectOne('/api/auth/register');

    expect(registerRequest.request.method).toBe('POST');

    registerRequest.flush(user);

    const loginRequest =
      httpTesting.expectOne('/api/auth/login');

    loginRequest.flush({
      accessToken: 'access-token-1',
      tokenType: 'Bearer',
      expiresIn: 900,
    });

    httpTesting
      .expectOne('/api/auth/me')
      .flush(user);

    expect(service.isAuthenticated()).toBe(true);
  });

  it('should restore a session through the refresh cookie', () => {
    service.restoreSession().subscribe();

    const refreshRequest =
      httpTesting.expectOne('/api/auth/refresh');

    expect(refreshRequest.request.method).toBe('POST');
    expect(
      refreshRequest.request.withCredentials,
    ).toBe(true);

    refreshRequest.flush({
      accessToken: 'restored-access-token',
      tokenType: 'Bearer',
      expiresIn: 900,
    });

    httpTesting
      .expectOne('/api/auth/me')
      .flush(user);

    expect(service.accessToken()).toBe(
      'restored-access-token',
    );
    expect(service.currentUser()).toEqual(user);
    expect(service.isAuthenticated()).toBe(true);
  });

  it('should remain unauthenticated when session restoration fails', () => {
    service.restoreSession().subscribe();

    const refreshRequest =
      httpTesting.expectOne('/api/auth/refresh');

    refreshRequest.flush(
      {},
      {
        status: 401,
        statusText: 'Unauthorized',
      },
    );

    expect(service.accessToken()).toBeNull();
    expect(service.currentUser()).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
  });

  it('should clear the SPA session on logout', () => {
    service
      .login({
        email: 'test@example.com',
        password: 'password123',
      })
      .subscribe();

    httpTesting
      .expectOne('/api/auth/login')
      .flush({
        accessToken: 'access-token-1',
        tokenType: 'Bearer',
        expiresIn: 900,
      });

    httpTesting
      .expectOne('/api/auth/me')
      .flush(user);

    service.logout().subscribe();

    const logoutRequest =
      httpTesting.expectOne('/api/auth/logout');

    expect(logoutRequest.request.withCredentials)
      .toBe(true);

    logoutRequest.flush(null);

    expect(service.accessToken()).toBeNull();
    expect(service.currentUser()).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
  });

  it('should reuse an in-flight refresh request', () => {
    service.refreshAccessToken().subscribe();
    service.refreshAccessToken().subscribe();

    const requests =
      httpTesting.match('/api/auth/refresh');

    expect(requests).toHaveLength(1);

    requests[0].flush({
      accessToken: 'new-access-token',
      tokenType: 'Bearer',
      expiresIn: 900,
    });

    expect(service.accessToken()).toBe(
      'new-access-token',
    );
  });
});