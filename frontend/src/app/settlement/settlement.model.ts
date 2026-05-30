export interface PersonSettlementDTO {
  id: string;
  name: string;
  totalPaid: number;
  shouldPay: number;
  balance: number;
}

export interface SettlementResponse {
  month: string;
  totalExpense: number;
  personA: PersonSettlementDTO;
  personB: PersonSettlementDTO;
  debtor: 'PERSON_A' | 'PERSON_B' | null;
  creditor: 'PERSON_A' | 'PERSON_B' | null;
  amountOwed: number | null;
  settled: boolean;
  pendingProportional: boolean;
  pendingMessage: string | null;
}
