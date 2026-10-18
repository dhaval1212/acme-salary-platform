export interface DepartmentStats {
  department: string;
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
