import { Routes } from '@angular/router';

export const settlementRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./settlement.component').then((m) => m.SettlementComponent),
  },
];
