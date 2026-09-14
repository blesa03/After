export interface CapsuleSummary {
  id: string;
  title: string;
  type: 'PERSONAL' | 'SHARED';
  status: string;
  opensAt: string;
  timezone: string;
  role: string;
}

export interface CreateCapsuleRequest {
  title: string;
  description: string;
  type: 'PERSONAL' | 'SHARED';
  opensAt: string; // <-- Cambia openDate por opensAt aquí
  timezone: string;
}