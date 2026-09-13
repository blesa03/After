import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CapsuleSummary, CreateCapsuleRequest } from '../models/capsule.models';

@Injectable({ providedIn: 'root' })
export class CapsuleService {
  private http = inject(HttpClient);
  
  // Asumimos que Angular usa proxy.conf.json para redirigir /api al backend
  private apiUrl = '/api/capsules'; 

  getCapsules(): Observable<CapsuleSummary[]> {
    return this.http.get<CapsuleSummary[]>(this.apiUrl);
  }

  createCapsule(data: CreateCapsuleRequest): Observable<{ id: string }> {
    return this.http.post<{ id: string }>(this.apiUrl, data);
  }
}