export type BudgetStatus = 'OK' | 'WARNING' | 'EXCEEDED';

export interface BudgetResponse {
  id: string;
  categoryId: string;
  categoryName: string;
  categoryColor: string;
  month: string;          // "2026-05-01"
  limitAmount: number;
  spentAmount: number;
  remainingAmount: number; // limitAmount - spentAmount (pode ser negativo)
  percentage: number;      // (spentAmount / limitAmount) * 100
  status: BudgetStatus;
  version: number;
}

export interface BudgetRequest {
  categoryId: string;
  month: string;          // "2026-05-01" — sempre dia 1
  limitAmount: number;
}
