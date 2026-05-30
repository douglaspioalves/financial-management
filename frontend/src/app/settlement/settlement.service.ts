import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SettlementResponse } from './settlement.model';

@Injectable({ providedIn: 'root' })
export class SettlementService {
  private readonly baseUrl = '/api/settlement';

  constructor(private http: HttpClient) {}

  getSettlement(month?: string): Observable<SettlementResponse> {
    let params = new HttpParams();
    if (month) {
      params = params.set('month', month);
    } else {
      const now = new Date();
      const currentMonth =
        now.getFullYear() +
        '-' +
        String(now.getMonth() + 1).padStart(2, '0');
      params = params.set('month', currentMonth);
    }
    return this.http.get<SettlementResponse>(this.baseUrl, { params });
  }
}
