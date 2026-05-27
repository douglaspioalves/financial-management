import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Person } from '../models/person.models';

@Injectable({ providedIn: 'root' })
export class PersonService {
  private readonly baseUrl = '/api/persons';

  constructor(private http: HttpClient) {}

  getPersons(): Observable<Person[]> {
    return this.http.get<Person[]>(this.baseUrl);
  }
}
