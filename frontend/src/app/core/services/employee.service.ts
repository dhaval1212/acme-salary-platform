import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiService } from './api.service';
import { Employee, EmployeeRequest, EmployeeFilter } from '../models/employee.model';
import { ApiResponse, PagedResponse } from '../models/api-response.model';

@Injectable({ providedIn: 'root' })
export class EmployeeService {
  constructor(private api: ApiService) {}

  list(filter: EmployeeFilter = {}): Observable<PagedResponse<Employee>> {
    return this.api
      .get<ApiResponse<Employee[]>>('/employees', {
        department: filter.department,
        country: filter.country,
        status: filter.status,
        search: filter.search,
        page: filter.page ?? 0,
        pageSize: filter.pageSize ?? 20,
      })
      .pipe(map(res => ({ data: res.data, meta: res.meta! })));
  }

  getById(id: string): Observable<Employee> {
    return this.api
      .get<ApiResponse<Employee>>(`/employees/${id}`)
      .pipe(map(res => res.data));
  }

  create(request: EmployeeRequest): Observable<Employee> {
    return this.api
      .post<ApiResponse<Employee>>('/employees', request)
      .pipe(map(res => res.data));
  }

  update(id: string, request: EmployeeRequest): Observable<Employee> {
    return this.api
      .put<ApiResponse<Employee>>(`/employees/${id}`, request)
      .pipe(map(res => res.data));
  }

  deactivate(id: string): Observable<Employee> {
    return this.api
      .patch<ApiResponse<Employee>>(`/employees/${id}/deactivate`)
      .pipe(map(res => res.data));
  }
}
