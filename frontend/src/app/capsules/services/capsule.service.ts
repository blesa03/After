import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreateCapsuleRequest, CapsuleSummary, UpdateCapsuleRequest } from '../models/capsule.models';
import { API_BASE_URL } from '../../core/config/api.config';

@Injectable({
  providedIn: 'root'
})
export class CapsuleService {
  private http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  createCapsule(payload: CreateCapsuleRequest): Observable<CapsuleSummary> {
    return this.http.post<CapsuleSummary>(`${this.apiBaseUrl}/capsules`, payload);
  }

  getCapsules(): Observable<CapsuleSummary[]> {
    return this.http.get<CapsuleSummary[]>(`${this.apiBaseUrl}/capsules`);
  }

  getCapsule(capsuleId: string): Observable<any> {
    return this.http.get<any>(`${this.apiBaseUrl}/capsules/${capsuleId}`);
  }

  updateCapsule(capsuleId: string, request: UpdateCapsuleRequest): Observable<CapsuleSummary> {
    return this.http.put<CapsuleSummary>(`${this.apiBaseUrl}/capsules/${capsuleId}`, request);
  }
}