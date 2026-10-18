import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiService } from './api.service';
import { Compensation, CompensationRequest } from '../models/compensation.model';
import { ApiResponse } from '../models/api-response.model';

@Injectable({ providedIn: 'root' })
export class CompensationService {
  constructor(private api: ApiService) {}

  getCurrent(employeeId: string): Observable<Compensation> {
    return this.api
      .get<ApiResponse<Compensation>>(`/employees/${employeeId}/compensation`)
      .pipe(map(res => res.data));
  }

  getHistory(employeeId: string): Observable<Compensation[]> {
    return this.api
      .get<ApiResponse<Compensation[]>>(`/employees/${employeeId}/compensation/history`)
      .pipe(map(res => res.data));
  }

  record(employeeId: string, request: CompensationRequest): Observable<Compensation> {
    return this.api
      .post<ApiResponse<Compensation>>(`/employees/${employeeId}/compensation`, request)
      .pipe(map(res => res.data));
  }
}
