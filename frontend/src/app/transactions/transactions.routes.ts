import { Routes } from '@angular/router';

export const transactionsRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./transaction-list/transaction-list.component').then(
        (m) => m.TransactionListComponent
      ),
  },
];
