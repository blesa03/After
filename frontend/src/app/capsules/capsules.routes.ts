import { Routes } from '@angular/router';

export const CAPSULE_ROUTES: Routes = [
  {
    path: 'create',
    loadComponent: () =>
      import('./pages/create-capsule/create-capsule').then((m) => m.CreateCapsule),
  },
];