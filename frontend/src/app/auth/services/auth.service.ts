import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import {
  catchError,
  finalize,
  map,
  Observable,
  of,
  shareReplay,
  switchMap,
  tap,
  throwError,
} from 'rxjs';

import { API_BASE_URL } from '../../core/config/api.config';
import {
  AccessTokenResponse,
  AuthUser,
  LoginCredentials,
  RegisterCredentials,
} from '../models/auth.models';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  private readonly accessTokenState = signal<string | null>(null);
  private readonly currentUserState = signal<AuthUser | null>(null);

  private refreshInFlight$: Observable<string> | null = null;

  readonly accessToken = this.accessTokenState.asReadonly();
  readonly currentUser = this.currentUserState.asReadonly();

  readonly isAuthenticated = computed(
    () =>
      this.accessTokenState() !== null &&
      this.currentUserState() !== null,
  );

  register(credentials: RegisterCredentials): Observable<AuthUser> {
    return this.http.post<AuthUser>(
      `${this.apiBaseUrl}/auth/register`,
      credentials,
    );
  }

  registerAndLogin(
    credentials: RegisterCredentials,
  ): Observable<void> {
    return this.register(credentials).pipe(
      switchMap(() => this.login(credentials)),
    );
  }

  login(credentials: LoginCredentials): Observable<void> {
    return this.http
      .post<AccessTokenResponse>(
        `${this.apiBaseUrl}/auth/login`,
        credentials,
        {
          withCredentials: true,
        },
      )
      .pipe(
        tap((response) => {
          this.accessTokenState.set(response.accessToken);
        }),
        switchMap(() => this.loadCurrentUser()),
        map(() => void 0),
        catchError((error: unknown) => {
          this.clearSession();
          return throwError(() => error);
        }),
      );
  }

  restoreSession(): Observable<void> {
    return this.refreshAccessToken().pipe(
      switchMap(() => this.loadCurrentUser()),
      map(() => void 0),
      catchError(() => {
        this.clearSession();
        return of(void 0);
      }),
    );
  }

  refreshAccessToken(): Observable<string> {
    if (this.refreshInFlight$ !== null) {
      return this.refreshInFlight$;
    }

    const request$ = this.http
      .post<AccessTokenResponse>(
        `${this.apiBaseUrl}/auth/refresh`,
        null,
        {
          withCredentials: true,
        },
      )
      .pipe(
        tap((response) => {
          this.accessTokenState.set(response.accessToken);
        }),
        map((response) => response.accessToken),
        catchError((error: unknown) => {
          this.clearSession();
          return throwError(() => error);
        }),
        finalize(() => {
          this.refreshInFlight$ = null;
        }),
        shareReplay({
          bufferSize: 1,
          refCount: false,
        }),
      );

    this.refreshInFlight$ = request$;

    return request$;
  }

  logout(): Observable<void> {
    return this.http
      .post<void>(
        `${this.apiBaseUrl}/auth/logout`,
        null,
        {
          withCredentials: true,
        },
      )
      .pipe(
        finalize(() => {
          this.clearSession();
        }),
      );
  }

  private loadCurrentUser(): Observable<AuthUser> {
    return this.http
      .get<AuthUser>(`${this.apiBaseUrl}/auth/me`)
      .pipe(
        tap((user) => {
          this.currentUserState.set(user);
        }),
      );
  }

  private clearSession(): void {
    this.accessTokenState.set(null);
    this.currentUserState.set(null);
  }
}