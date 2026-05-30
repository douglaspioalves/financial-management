import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ExportService {
  private readonly baseUrl = '/api/export';

  constructor(private http: HttpClient) {}

  exportFile(month: string, format: 'csv' | 'xlsx'): Observable<Blob> {
    const params = new HttpParams().set('month', month).set('format', format);
    return this.http.get(this.baseUrl, {
      params,
      responseType: 'blob',
    });
  }

  triggerDownload(blob: Blob, month: string, format: 'csv' | 'xlsx'): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `gastos-${month}.${format}`;
    a.click();
    URL.revokeObjectURL(url);
  }
}
