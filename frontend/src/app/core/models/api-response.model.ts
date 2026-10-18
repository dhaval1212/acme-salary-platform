export interface ApiError {
  field: string;
  message: string;
}

export interface PageMeta {
  page: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
}

export interface ApiResponse<T> {
  data: T;
  meta?: PageMeta;
  errors?: ApiError[];
}

export interface PagedResponse<T> {
  data: T[];
  meta: PageMeta;
}
