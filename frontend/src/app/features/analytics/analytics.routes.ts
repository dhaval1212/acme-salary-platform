import { Routes } from '@angular/router';

export const analyticsRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./analytics-dashboard/analytics-dashboard.component').then(
        m => m.AnalyticsDashboardComponent
      )
  }
];
