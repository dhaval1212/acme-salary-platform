import { Routes } from '@angular/router';

export const employeeRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./employee-list/employee-list.component').then(m => m.EmployeeListComponent)
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./employee-detail/employee-detail.component').then(m => m.EmployeeDetailComponent)
  }
];
