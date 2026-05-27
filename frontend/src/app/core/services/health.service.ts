import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';

export interface HealthResponse {
  status: string;
}

@Injectable({
  providedIn: 'root'
})
export class HealthService {

  constructor(private http: HttpClient) {}

  check(): Observable<HealthResponse | null> {
    return this.http.get<HealthResponse>('/api/health').pipe(
      catchError(() => of(null))
    );
  }
}
