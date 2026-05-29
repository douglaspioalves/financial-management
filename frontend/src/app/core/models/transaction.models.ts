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

export interface CardSummary {
  id: string;
  name: string;
  closingDay: number;
  dueDay: number;
}

export interface InstallmentResponse {
  id: string;
  number: number;
  amount: number;
  referenceMonth: string; // "yyyy-MM-dd" — sempre dia 1 do mês de referência
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
  card?: CardSummary | null;
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
