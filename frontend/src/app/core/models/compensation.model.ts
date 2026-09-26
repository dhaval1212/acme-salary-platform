export interface Compensation {
  id: string;
  employeeId: string;
  amount: number;
  currency: string;
  effectiveDate: string;
  changedBy: string;
  createdAt: string;
}

export interface CompensationRequest {
  amount: number;
  currency: string;
  effectiveDate: string;
  changedBy: string;
}
