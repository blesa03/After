import { Routes } from '@angular/router';

import { AUTH_ROUTES } from './auth/auth.routes';
import { authGuard } from './auth/guards/auth.guard';

export const routes: Routes = [
  ...AUTH_ROUTES,

  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import(
        './shared/layout/app-layout/app-layout'
      ).then((m) => m.AppLayout),
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard',
      },
      {
        path: 'dashboard',
        loadComponent: () =>
          import(
            './dashboard/pages/dashboard/dashboard'
          ).then((m) => m.Dashboard),
      },
      
       
      {
        path: 'capsules',
        loadChildren: () =>
          import(
            './capsules/capsules.routes'
          ).then((m) => m.CAPSULE_ROUTES),
      },
    ],
  },

  {
    path: '**',
    loadComponent: () =>
      import(
        './errors/pages/not-found/not-found'
      ).then((m) => m.NotFound),
  },
];