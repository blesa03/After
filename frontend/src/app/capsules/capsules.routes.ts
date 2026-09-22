import { Routes } from '@angular/router';

export const CAPSULE_ROUTES: Routes = [
  {
    path: 'create',
    loadComponent: () =>
      import(
        './pages/create-capsule/create-capsule'
      ).then((m) => m.CreateCapsule),
  },
  {
    path: ':id',
    loadComponent: () =>
      import(
        './pages/capsule-detail/capsule-detail'
      ).then((m) => m.CapsuleDetailPage),
  },
];