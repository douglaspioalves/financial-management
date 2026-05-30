import { Routes } from '@angular/router';

export const recurringRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./recurring-list/recurring-list.component').then(
        (m) => m.RecurringListComponent
      ),
  },
];
