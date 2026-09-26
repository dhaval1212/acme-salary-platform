export type EmployeeStatus = 'ACTIVE' | 'INACTIVE';

export interface Employee {
  id: string;
  fullName: string;
  email: string;
  department: string;
  jobTitle: string;
  country: string;
  status: EmployeeStatus;
  createdAt: string;
  updatedAt: string;
}

export interface EmployeeRequest {
  fullName: string;
  email: string;
  department: string;
  jobTitle: string;
  country: string;
}

export interface EmployeeFilter {
  department?: string;
  country?: string;
  status?: EmployeeStatus;
  search?: string;
  page?: number;
  pageSize?: number;
}
