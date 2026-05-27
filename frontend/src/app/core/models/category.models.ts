export type CategoryType = 'EXPENSE' | 'INCOME' | 'BOTH';

export interface Category {
  id: string;
  name: string;
  type: CategoryType;
  color: string;
}

export interface CreateCategoryRequest {
  name: string;
  type: CategoryType;
  color: string;
}
