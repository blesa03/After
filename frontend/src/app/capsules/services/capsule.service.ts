import {
  inject,
  Injectable,
} from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { API_BASE_URL } from '../../core/config/api.config';
import {
  CapsuleDetail,
  CapsuleParticipant,
  CapsuleSummary,
  Contribution,
  CreateCapsuleRequest,
  InvitationAccepted,
  InvitationCreated,
  InvitationPreview,
  TextContributionRequest,
  UpdateCapsuleRequest,
} from '../models/capsule.models';

@Injectable({
  providedIn: 'root',
})
export class CapsuleService {
  private readonly http =
    inject(HttpClient);

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

  getCapsule(
    id: string,
  ) {
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
    return this.http.get<
      CapsuleParticipant[]
    >(
      `${this.apiBaseUrl}/capsules/${capsuleId}/participants`,
    );
  }

  createInvitation(
    capsuleId: string,
  ) {
    return this.http.post<
      InvitationCreated
    >(
      `${this.apiBaseUrl}/capsules/${capsuleId}/invitations`,
      null,
    );
  }

  getInvitation(
    token: string,
  ) {
    return this.http.get<
      InvitationPreview
    >(
      `${this.apiBaseUrl}/invitations/${encodeURIComponent(
        token,
      )}`,
    );
  }

  acceptInvitation(
    token: string,
  ) {
    return this.http.post<
      InvitationAccepted
    >(
      `${this.apiBaseUrl}/invitations/${encodeURIComponent(
        token,
      )}/accept`,
      null,
    );
  }

  getTextContributions(
    capsuleId: string,
  ) {
    return this.http.get<
      Contribution[]
    >(
      `${this.apiBaseUrl}/capsules/${capsuleId}/contributions/text`,
    );
  }

  createTextContribution(
    capsuleId: string,
    payload:
      TextContributionRequest,
  ) {
    return this.http.post<
      Contribution
    >(
      `${this.apiBaseUrl}/capsules/${capsuleId}/contributions/text`,
      payload,
    );
  }

  updateTextContribution(
    capsuleId: string,
    contributionId: string,
    payload:
      TextContributionRequest,
  ) {
    return this.http.put<
      Contribution
    >(
      `${this.apiBaseUrl}/capsules/${capsuleId}/contributions/${contributionId}/text`,
      payload,
    );
  }

  deleteContribution(
    capsuleId: string,
    contributionId: string,
  ) {
    return this.http.delete<void>(
      `${this.apiBaseUrl}/capsules/${capsuleId}/contributions/${contributionId}`,
    );
  }
}