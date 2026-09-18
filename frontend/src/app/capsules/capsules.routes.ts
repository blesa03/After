import { Routes } from '@angular/router';

export const CAPSULE_ROUTES: Routes = [
  {
    path: 'create',
    loadComponent: () =>
      import('./pages/create-capsule/create-capsule').then((m) => m.CreateCapsule),
  },
  {
    path: 'edit/:id',
    loadComponent: () =>
      import('./pages/edit-capsule/edit-capsule').then((m) => m.EditCapsule),
  },
];