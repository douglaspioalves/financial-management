export type TransactionType = 'EXPENSE' | 'INCOME';
export type PaymentMethod = 'CASH' | 'DEBIT' | 'CREDIT' | 'PIX' | 'TRANSFER';
export type SplitRule = 'PERSON_A' | 'PERSON_B' | 'FIFTY_FIFTY' | 'PROPORTIONAL';

export interface CategorySummary {
  id: string;
  name: string;
  type: string;
  color: string;
}

export interface PersonSummary {
  id: string;
  name: string;
  color: string;
}

export interface Transaction {
  id: string;
  type: TransactionType;
  amount: number;
  date: string; // yyyy-MM-dd
  description?: string;
  category: CategorySummary;
  paidByPerson: PersonSummary;
  paymentMethod: PaymentMethod;
  cardId?: string;
  splitRule: SplitRule;
  installmentsTotal: number;
  createdAt: string;
  version: number;
}

export interface CreateTransactionRequest {
  type: TransactionType;
  amount: number;
  date: string;
  description?: string;
  categoryId: string;
  paidByPersonId: string;
  paymentMethod: PaymentMethod;
  cardId?: string;
  splitRule: SplitRule;
  installmentsTotal: number;
}

export interface UpdateTransactionRequest {
  type: TransactionType;
  amount: number;
  date: string;
  description?: string;
  categoryId: string;
  paidByPersonId: string;
  paymentMethod: PaymentMethod;
  cardId?: string;
  splitRule: SplitRule;
  installmentsTotal: number;
  version: number;
}
