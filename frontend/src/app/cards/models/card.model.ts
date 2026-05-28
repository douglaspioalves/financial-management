export interface Card {
  id: string;
  name: string;
  ownerPersonId: string;
  ownerPersonName: string;
  closingDay: number;
  dueDay: number;
  version: number;
}

export interface CardRequest {
  name: string;
  ownerPersonId: string;
  closingDay: number;
  dueDay: number;
}
