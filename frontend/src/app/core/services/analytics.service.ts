import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiService } from './api.service';
import { DepartmentStats, AnalyticsFilter } from '../models/analytics.model';
import { ApiResponse } from '../models/api-response.model';

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  constructor(private api: ApiService) {}

  getDepartmentStats(filter: AnalyticsFilter = {}): Observable<DepartmentStats[]> {
    return this.api.get<ApiResponse<DepartmentStats[]>>('/analytics/departments', {
      department: filter.department,
      country: filter.country
    }).pipe(
      map(res => res.data)
    );
  }
}
