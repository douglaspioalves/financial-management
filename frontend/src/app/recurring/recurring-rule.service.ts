import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RecurringRuleResponse,
  CreateRecurringRuleRequest,
} from './models/recurring-rule.models';

@Injectable({ providedIn: 'root' })
export class RecurringRuleService {
  private readonly baseUrl = '/api/recurring-rules';

  constructor(private http: HttpClient) {}

  getAll(): Observable<RecurringRuleResponse[]> {
    return this.http.get<RecurringRuleResponse[]>(this.baseUrl);
  }

  create(request: CreateRecurringRuleRequest): Observable<RecurringRuleResponse> {
    return this.http.post<RecurringRuleResponse>(this.baseUrl, request);
  }

  deactivate(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
