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