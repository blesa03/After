import {
  inject,
  Injectable,
} from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { API_BASE_URL } from '../../core/config/api.config';
import {
  CapsuleDetail,
  CapsuleSummary,
  CreateCapsuleRequest,
  UpdateCapsuleRequest,
} from '../models/capsule.models';

@Injectable({
  providedIn: 'root',
})
export class CapsuleService {
  private readonly http = inject(HttpClient);

  private readonly apiBaseUrl =
    inject(API_BASE_URL);

  createCapsule(
    payload: CreateCapsuleRequest,
  ) {
    return this.http.post<CapsuleDetail>(
      `${this.apiBaseUrl}/capsules`,
      payload,
    );
  }

  getCapsules() {
    return this.http.get<CapsuleSummary[]>(
      `${this.apiBaseUrl}/capsules`,
    );
  }

  getCapsule(id: string) {
    return this.http.get<CapsuleDetail>(
      `${this.apiBaseUrl}/capsules/${id}`,
    );
  }

  updateCapsule(
    id: string,
    payload: UpdateCapsuleRequest,
  ) {
    return this.http.put<CapsuleDetail>(
      `${this.apiBaseUrl}/capsules/${id}`,
      payload,
    );
  }
}