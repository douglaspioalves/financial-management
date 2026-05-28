import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Card, CardRequest } from '../models/card.model';

@Injectable({ providedIn: 'root' })
export class CardService {
  private readonly baseUrl = '/api/cards';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Card[]> {
    return this.http.get<Card[]>(this.baseUrl);
  }

  getById(id: string): Observable<Card> {
    return this.http.get<Card>(`${this.baseUrl}/${id}`);
  }

  create(req: CardRequest): Observable<Card> {
    return this.http.post<Card>(this.baseUrl, req);
  }

  update(id: string, req: CardRequest & { version: number }): Observable<Card> {
    return this.http.put<Card>(`${this.baseUrl}/${id}`, req);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
