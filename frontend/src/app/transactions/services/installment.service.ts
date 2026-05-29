import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { InstallmentResponse } from '../../core/models/transaction.models';

@Injectable({ providedIn: 'root' })
export class InstallmentService {
  private readonly baseUrl = '/api/transactions';

  constructor(private http: HttpClient) {}

  getInstallments(transactionId: string): Observable<InstallmentResponse[]> {
    return this.http.get<InstallmentResponse[]>(
      `${this.baseUrl}/${transactionId}/installments`
    );
  }
}
