export interface CapsuleSummary {
  id: string;
  title: string;
  type: 'PERSONAL' | 'SHARED';
  status: string;
  openDate: string;
  role: string;
  timeRemaining?: string; 
}

export interface CreateCapsuleRequest {
  title: string;
  description: string;
  type: 'PERSONAL' | 'SHARED';
  opensAt: string; // <-- Cambia openDate por opensAt aquí
  timezone: string;
}