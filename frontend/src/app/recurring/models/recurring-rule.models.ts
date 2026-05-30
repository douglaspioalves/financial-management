export type RecurringFrequency = 'MONTHLY' | 'WEEKLY' | 'YEARLY';
export type TransactionType = 'EXPENSE' | 'INCOME';
export type PaymentMethod = 'CASH' | 'DEBIT' | 'CREDIT' | 'PIX' | 'TRANSFER';
export type SplitRule = 'PERSON_A' | 'PERSON_B' | 'FIFTY_FIFTY' | 'PROPORTIONAL';

export interface RecurringRuleResponse {
  id: string;
  type: TransactionType;
  amount: number;
  description: string;
  categoryId: string;
  categoryName: string;
  paidByPersonId: string;
  paidByPersonName: string;
  paymentMethod: PaymentMethod;
  splitRule: SplitRule;
  frequency: RecurringFrequency;
  nextDate: string;
  active: boolean;
  version: number;
}

export interface CreateRecurringRuleRequest {
  type: TransactionType;
  amount: number;
  description: string;
  categoryId: string;
  paidByPersonId: string;
  paymentMethod: PaymentMethod;
  splitRule: SplitRule;
  frequency: RecurringFrequency;
  nextDate: string;
}
