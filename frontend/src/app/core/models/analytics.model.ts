export interface DepartmentStats {
  department: string;
  country?: string;
  currency?: string;
  headcount: number;
  avgSalary: number;
  medianSalary: number;
  minSalary: number;
  maxSalary: number;
}

export interface AnalyticsFilter {
  department?: string;
  country?: string;
}
