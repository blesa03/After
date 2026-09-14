import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CreateCapsuleRequest, CapsuleSummary } from '../models/capsule.models';
import { API_BASE_URL } from '../../core/config/api.config';

@Injectable({
  providedIn: 'root'
})
export class CapsuleService {
  private http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL); 

  createCapsule(payload: CreateCapsuleRequest) {
    // Usamos la variable inyectada
    return this.http.post<CapsuleSummary>(`${this.apiBaseUrl}/capsules`, payload);
  }

  getCapsules() {
    return this.http.get<CapsuleSummary[]>(`${this.apiBaseUrl}/capsules`);
  }
}