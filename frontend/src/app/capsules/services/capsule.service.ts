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
  CapsuleParticipant,
  InvitationAccepted,
  InvitationCreated,
  InvitationPreview,
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

  getParticipants(
    capsuleId: string,
  ) {
    return this.http.get<CapsuleParticipant[]>(
      `${this.apiBaseUrl}/capsules/${capsuleId}/participants`,
    );
  }

  createInvitation(
    capsuleId: string,
  ) {
    return this.http.post<InvitationCreated>(
      `${this.apiBaseUrl}/capsules/${capsuleId}/invitations`,
      null,
    );
  }

  getInvitation(
    token: string,
  ) {
    return this.http.get<InvitationPreview>(
      `${this.apiBaseUrl}/invitations/${encodeURIComponent(token)}`,
    );
  }

  acceptInvitation(
    token: string,
  ) {
    return this.http.post<InvitationAccepted>(
      `${this.apiBaseUrl}/invitations/${encodeURIComponent(token)}/accept`,
      null,
    );
  }
}