import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BudgetRequest, BudgetResponse } from '../models/budget.model';

@Injectable({ providedIn: 'root' })
export class BudgetService {
  private readonly baseUrl = '/api/budgets';

  constructor(private http: HttpClient) {}

  getByMonth(month: string): Observable<BudgetResponse[]> {
    const params = new HttpParams().set('month', month);
    return this.http.get<BudgetResponse[]>(this.baseUrl, { params });
  }

  create(req: BudgetRequest): Observable<BudgetResponse> {
    return this.http.post<BudgetResponse>(this.baseUrl, req);
  }

  update(id: string, req: BudgetRequest & { version: number }): Observable<BudgetResponse> {
    return this.http.put<BudgetResponse>(`${this.baseUrl}/${id}`, req);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
