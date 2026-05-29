import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DashboardResponse } from '../models/dashboard.models';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly baseUrl = '/api/dashboard';

  constructor(private http: HttpClient) {}

  getDashboard(month: string): Observable<DashboardResponse> {
    const params = new HttpParams().set('month', month);
    return this.http.get<DashboardResponse>(this.baseUrl, { params });
  }
}
