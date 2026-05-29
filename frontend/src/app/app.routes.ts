import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/auth/login', pathMatch: 'full' },
  {
    path: 'auth',
    loadChildren: () => import('./auth/auth.routes').then(m => m.authRoutes),
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./dashboard/dashboard.component').then(m => m.DashboardComponent),
  },
  {
    path: 'transactions',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./transactions/transactions.routes').then(m => m.transactionsRoutes),
  },
  {
    path: 'cards',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./cards/cards.routes').then(m => m.cardsRoutes),
  },
  {
    path: 'budget',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./budget/budget.routes').then(m => m.budgetRoutes),
  },
  { path: '**', redirectTo: '/auth/login' },
];
