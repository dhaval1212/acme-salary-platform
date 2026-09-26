import { Routes } from '@angular/router';
import { ShellComponent } from './layout/shell/shell.component';

export const routes: Routes = [
  {
    path: '',
    component: ShellComponent,
    children: [
      { path: '', redirectTo: 'employees', pathMatch: 'full' },
      {
        path: 'employees',
        loadChildren: () =>
          import('./features/employees/employees.routes').then(m => m.employeeRoutes)
      },
      {
        path: 'analytics',
        loadChildren: () =>
          import('./features/analytics/analytics.routes').then(m => m.analyticsRoutes)
      },
    ]
  },
  { path: '**', redirectTo: 'employees' }
];
