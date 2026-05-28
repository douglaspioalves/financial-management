import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Transaction,
  CreateTransactionRequest,
  UpdateTransactionRequest,
} from '../models/transaction.models';

@Injectable({ providedIn: 'root' })
export class TransactionService {
  private readonly baseUrl = '/api/transactions';

  constructor(private http: HttpClient) {}

  getTransactions(month: string): Observable<Transaction[]> {
    const params = new HttpParams().set('month', month);
    return this.http.get<Transaction[]>(this.baseUrl, { params });
  }

  getTransaction(id: string): Observable<Transaction> {
    return this.http.get<Transaction>(`${this.baseUrl}/${id}`);
  }

  createTransaction(req: CreateTransactionRequest): Observable<Transaction> {
    return this.http.post<Transaction>(this.baseUrl, req);
  }

  updateTransaction(
    id: string,
    req: UpdateTransactionRequest
  ): Observable<Transaction> {
    return this.http.put<Transaction>(`${this.baseUrl}/${id}`, req);
  }

  deleteTransaction(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
