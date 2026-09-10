import {
  HttpClient,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { API_BASE_URL } from '../../core/config/api.config';
import { AuthService } from '../services/auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let httpTesting: HttpTestingController;
  let authService: AuthService;

  const user = {
    id: '8f421bd7-8d46-4f04-a213-1da035a79fd6',
    email: 'test@example.com',
    createdAt: '2026-09-10T18:00:00Z',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: API_BASE_URL,
          useValue: '/api',
        },
        provideHttpClient(
          withInterceptors([authInterceptor]),
        ),
        provideHttpClientTesting(),
      ],
    });

    httpClient = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(
      HttpTestingController,
    );
    authService = TestBed.inject(AuthService);

    authenticate();
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should attach the access token to protected API requests', () => {
    httpClient.get('/api/example').subscribe();

    const request =
      httpTesting.expectOne('/api/example');

    expect(
      request.request.headers.get(
        'Authorization',
      ),
    ).toBe('Bearer access-token-1');

    request.flush({});
  });

  it('should refresh and retry after an expired access token', () => {
    httpClient.get('/api/example').subscribe();

    const firstRequest =
      httpTesting.expectOne('/api/example');

    expect(
      firstRequest.request.headers.get(
        'Authorization',
      ),
    ).toBe('Bearer access-token-1');

    firstRequest.flush(
      {},
      {
        status: 401,
        statusText: 'Unauthorized',
      },
    );

    const refreshRequest =
      httpTesting.expectOne('/api/auth/refresh');

    expect(
      refreshRequest.request.headers.has(
        'Authorization',
      ),
    ).toBe(false);

    expect(
      refreshRequest.request.withCredentials,
    ).toBe(true);

    refreshRequest.flush({
      accessToken: 'access-token-2',
      tokenType: 'Bearer',
      expiresIn: 900,
    });

    const retriedRequest =
      httpTesting.expectOne('/api/example');

    expect(
      retriedRequest.request.headers.get(
        'Authorization',
      ),
    ).toBe('Bearer access-token-2');

    retriedRequest.flush({});
  });

  function authenticate(): void {
    authService
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

    const meRequest =
      httpTesting.expectOne('/api/auth/me');

    expect(
      meRequest.request.headers.get(
        'Authorization',
      ),
    ).toBe('Bearer access-token-1');

    meRequest.flush(user);
  }
});