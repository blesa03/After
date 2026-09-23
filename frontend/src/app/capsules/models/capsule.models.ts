export type CapsuleType =
  | 'PERSONAL'
  | 'SHARED';

export type CapsuleStatus =
  | 'COLLECTING'
  | 'SEALED'
  | 'OPENED';

export type CapsuleRole =
  | 'OWNER'
  | 'CONTRIBUTOR';

export type ContributionType =
  | 'TEXT'
  | 'IMAGE'
  | 'AUDIO'
  | 'VIDEO';

export interface CapsuleSummary {
  id: string;
  title: string;
  type: CapsuleType;
  status: CapsuleStatus;
  opensAt: string;
  timezone: string;
  role: CapsuleRole;
}

export interface CapsuleDetail {
  id: string;
  title: string;
  description: string | null;
  type: CapsuleType;
  status: CapsuleStatus;
  opensAt: string;
  timezone: string;
  sealedAt: string | null;
  openedAt: string | null;
  createdAt: string;
  updatedAt: string;
  role: CapsuleRole;
}

export interface CreateCapsuleRequest {
  title: string;
  description: string;
  type: CapsuleType;
  opensAt: string;
  timezone: string;
}

export interface UpdateCapsuleRequest {
  title: string;
  description: string | null;
  opensAt: string;
  timezone: string;
}

export interface CapsuleParticipant {
  email: string;
  role: CapsuleRole;
  joinedAt: string;
}

export interface InvitationCreated {
  token: string;
}

export interface InvitationPreview {
  capsuleTitle: string;
  ownerEmail: string;
  opensAt: string;
}

export interface InvitationAccepted {
  capsuleId: string;
  role: 'CONTRIBUTOR';
}

export interface Contribution {
  id: string;
  capsuleId: string;
  authorUserId: string;
  type: ContributionType;
  textContent: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface TextContributionRequest {
  textContent: string;
}