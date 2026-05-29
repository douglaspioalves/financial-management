export interface MonthSummary {
  totalIncome: number;
  totalExpense: number;
  balance: number;
}

export interface CategoryExpense {
  categoryId: string;
  categoryName: string;
  categoryColor: string;
  total: number;
  percentage: number;
}

export interface RecentTransaction {
  id: string;
  date: string;        // "yyyy-MM-dd"
  description: string;
  amount: number;
  type: 'EXPENSE' | 'INCOME';
  categoryName: string;
  paidByPersonName: string;
}

export interface DashboardResponse {
  month: string;                         // "2026-05"
  totalIncome: number;
  totalExpense: number;
  balance: number;
  previousMonth: MonthSummary;
  incomeVariation: number | null;        // % vs mês anterior, null se sem dados
  expenseVariation: number | null;
  expenseByCategory: CategoryExpense[];
  recentTransactions: RecentTransaction[];
}
