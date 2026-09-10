import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import {
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import {
  API_BASE_URL,
  apiConfig,
} from './core/config/api.config';
import { authInterceptor } from './auth/interceptors/auth.interceptor';
import { AuthService } from './auth/services/auth.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),

    provideHttpClient(
      withInterceptors([authInterceptor]),
    ),

    {
      provide: API_BASE_URL,
      useValue: apiConfig.baseUrl,
    },

    provideAppInitializer(() =>
      inject(AuthService).restoreSession(),
    ),
  ],
};