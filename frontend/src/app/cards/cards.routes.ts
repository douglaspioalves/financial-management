import { Routes } from '@angular/router';

export const cardsRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./card-list/card-list.component').then(
        (m) => m.CardListComponent
      ),
  },
  {
    path: 'new',
    loadComponent: () =>
      import('./card-form/card-form.component').then(
        (m) => m.CardFormComponent
      ),
  },
  {
    path: 'edit/:id',
    loadComponent: () =>
      import('./card-form/card-form.component').then(
        (m) => m.CardFormComponent
      ),
  },
];
