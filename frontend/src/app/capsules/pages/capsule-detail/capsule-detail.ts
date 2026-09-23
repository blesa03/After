import {
  ChangeDetectionStrategy,
  Component,
  computed,
  HostListener,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import {
  ActivatedRoute,
  Router,
} from '@angular/router';
import { finalize } from 'rxjs';

import {
  CapsuleDetail,
  CapsuleParticipant,
  CapsuleRole,
  CapsuleStatus,
  CapsuleType,
  Contribution,
  UpdateCapsuleRequest,
} from '../../models/capsule.models';
import { CapsuleService } from '../../services/capsule.service';

function toDateInputValue(
  date: Date,
): string {
  const year = date.getFullYear();

  const month = String(
    date.getMonth() + 1,
  ).padStart(2, '0');

  const day = String(
    date.getDate(),
  ).padStart(2, '0');

  return `${year}-${month}-${day}`;
}

function toTimeInputValue(
  date: Date,
): string {
  const hours = String(
    date.getHours(),
  ).padStart(2, '0');

  const minutes = String(
    date.getMinutes(),
  ).padStart(2, '0');

  return `${hours}:${minutes}`;
}

function combineDateAndTime(
  date: string,
  time: string,
): Date {
  return new Date(
    `${date}T${time}`,
  );
}

function getSupportedTimezones():
  string[] {
  const intlWithSupportedValues =
    Intl as typeof Intl & {
      supportedValuesOf?: (
        key: 'timeZone',
      ) => string[];
    };

  if (
    typeof intlWithSupportedValues
      .supportedValuesOf ===
    'function'
  ) {
    return intlWithSupportedValues
      .supportedValuesOf(
        'timeZone',
      );
  }

  return [
    'Europe/Madrid',
    'Europe/London',
    'Europe/Paris',
    'America/New_York',
    'America/Chicago',
    'America/Denver',
    'America/Los_Angeles',
    'America/Sao_Paulo',
    'Asia/Tokyo',
    'Asia/Shanghai',
    'Asia/Kolkata',
    'Australia/Sydney',
  ];
}

const futureDateValidator:
  ValidatorFn = (
    control: AbstractControl,
  ): ValidationErrors | null => {
    const date =
      control.get('date')?.value;

    const time =
      control.get('time')?.value;

    if (!date || !time) {
      return null;
    }

    const selectedDate =
      combineDateAndTime(
        date,
        time,
      );

    if (
      Number.isNaN(
        selectedDate.getTime(),
      ) ||
      selectedDate.getTime() <=
        Date.now()
    ) {
      return {
        notFuture: true,
      };
    }

    return null;
  };

@Component({
  selector: 'app-capsule-detail',
  imports: [
    DatePipe,
    ReactiveFormsModule,
  ],
  templateUrl:
    './capsule-detail.html',
  styleUrl:
    './capsule-detail.scss',
  changeDetection:
    ChangeDetectionStrategy.OnPush,
})
export class CapsuleDetailPage
  implements OnInit {

  private readonly capsuleService =
    inject(CapsuleService);

  private readonly route =
    inject(ActivatedRoute);

  private readonly router =
    inject(Router);

  private readonly formBuilder =
    inject(FormBuilder);

  readonly capsule =
    signal<CapsuleDetail | null>(
      null,
    );

  readonly participants =
    signal<CapsuleParticipant[]>([]);

  readonly contributions =
    signal<Contribution[]>([]);

  readonly isLoading =
    signal(true);

  readonly loadError =
    signal<string | null>(
      null,
    );

  readonly isEditing =
    signal(false);

  readonly isSaving =
    signal(false);

  readonly saveError =
    signal<string | null>(
      null,
    );

  readonly participantsLoading =
    signal(false);

  readonly invitationLoading =
    signal(false);

  readonly invitationUrl =
    signal<string | null>(
      null,
    );

  readonly invitationError =
    signal<string | null>(
      null,
    );

  readonly invitationCopied =
    signal(false);

  readonly contributionsLoading =
    signal(false);

  readonly contributionSaving =
    signal(false);

  readonly contributionSaved =
    signal(false);

  readonly contributionError =
    signal<string | null>(
      null,
    );

  readonly editingContributionId =
    signal<string | null>(
      null,
    );

  readonly deletingContributionId =
    signal<string | null>(
      null,
    );

  readonly timezones =
    getSupportedTimezones();

  readonly canEdit =
    computed(() => {
      const currentCapsule =
        this.capsule();

      return (
        currentCapsule?.role ===
          'OWNER' &&
        currentCapsule.status ===
          'COLLECTING'
      );
    });

  readonly canInvite =
    computed(() => {
      const currentCapsule =
        this.capsule();

      return (
        currentCapsule?.type ===
          'SHARED' &&
        currentCapsule.role ===
          'OWNER' &&
        currentCapsule.status ===
          'COLLECTING'
      );
    });

  readonly canManageContributions =
    computed(() => {
      return (
        this.capsule()?.status ===
        'COLLECTING'
      );
    });

  readonly editForm =
    this.formBuilder.nonNullable.group(
      {
        title: [
          '',
          [
            Validators.required,
            Validators.maxLength(
              255,
            ),
          ],
        ],

        description: [''],

        date: [
          '',
          Validators.required,
        ],

        time: [
          '',
          Validators.required,
        ],

        timezone: [
          '',
          Validators.required,
        ],
      },
      {
        validators:
          futureDateValidator,
      },
    );

  readonly contributionForm =
    this.formBuilder.nonNullable.group({
      textContent: [
        '',
        [
          Validators.required,
        ],
      ],
    });

  readonly contributionEditForm =
    this.formBuilder.nonNullable.group({
      textContent: [
        '',
        [
          Validators.required,
        ],
      ],
    });

  get minDate(): string {
    return toDateInputValue(
      new Date(),
    );
  }

  ngOnInit(): void {
    const capsuleId =
      this.route.snapshot
        .paramMap
        .get('id');

    if (!capsuleId) {
      this.isLoading.set(false);

      this.loadError.set(
        'No se ha podido identificar la cápsula.',
      );

      return;
    }

    this.loadCapsule(
      capsuleId,
    );
  }

  @HostListener(
    'document:keydown.escape',
  )
  onEscapePressed(): void {
    if (
      this.isEditing() &&
      !this.isSaving()
    ) {
      this.cancelEditing();
    }
  }

  startEditing(): void {
    const currentCapsule =
      this.capsule();

    if (
      !currentCapsule ||
      !this.canEdit()
    ) {
      return;
    }

    const openingDate =
      new Date(
        currentCapsule.opensAt,
      );

    this.editForm.reset({
      title:
        currentCapsule.title,

      description:
        currentCapsule
          .description ?? '',

      date:
        toDateInputValue(
          openingDate,
        ),

      time:
        toTimeInputValue(
          openingDate,
        ),

      timezone:
        currentCapsule.timezone,
    });

    this.saveError.set(null);
    this.isEditing.set(true);
  }

  cancelEditing(): void {
    if (this.isSaving()) {
      return;
    }

    this.saveError.set(null);
    this.isEditing.set(false);
  }

  onEditDateChange(): void {
    this.editForm
      .updateValueAndValidity();
  }

  onEditTimeChange(): void {
    const date =
      this.editForm.controls
        .date.value;

    const time =
      this.editForm.controls
        .time.value;

    if (!date || !time) {
      return;
    }

    const today =
      toDateInputValue(
        new Date(),
      );

    if (date !== today) {
      this.editForm
        .updateValueAndValidity();

      return;
    }

    const selectedDate =
      combineDateAndTime(
        date,
        time,
      );

    if (
      selectedDate.getTime() >
      Date.now()
    ) {
      this.editForm
        .updateValueAndValidity();

      return;
    }

    const tomorrow =
      new Date();

    tomorrow.setDate(
      tomorrow.getDate() + 1,
    );

    this.editForm.controls
      .date
      .setValue(
        toDateInputValue(
          tomorrow,
        ),
      );

    this.editForm
      .updateValueAndValidity();
  }

  saveChanges(): void {
    const currentCapsule =
      this.capsule();

    if (
      !currentCapsule ||
      !this.canEdit() ||
      this.isSaving()
    ) {
      return;
    }

    if (
      this.editForm.invalid
    ) {
      this.editForm
        .markAllAsTouched();

      return;
    }

    const values =
      this.editForm
        .getRawValue();

    const opensAt =
      combineDateAndTime(
        values.date,
        values.time,
      );

    if (
      Number.isNaN(
        opensAt.getTime(),
      ) ||
      opensAt.getTime() <=
        Date.now()
    ) {
      this.editForm
        .setErrors({
          notFuture: true,
        });

      return;
    }

    const payload:
      UpdateCapsuleRequest = {
        title:
          values.title.trim(),

        description:
          values.description
            .trim() || null,

        opensAt:
          opensAt.toISOString(),

        timezone:
          values.timezone,
      };

    this.isSaving.set(true);
    this.saveError.set(null);

    this.capsuleService
      .updateCapsule(
        currentCapsule.id,
        payload,
      )
      .pipe(
        finalize(() => {
          this.isSaving.set(
            false,
          );
        }),
      )
      .subscribe({
        next:
          (updatedCapsule) => {
            this.capsule.set(
              updatedCapsule,
            );

            this.isEditing.set(
              false,
            );
          },

        error: () => {
          this.saveError.set(
            'No se han podido guardar los cambios. Inténtalo de nuevo.',
          );
        },
      });
  }

  generateInvitation(): void {
    const currentCapsule =
      this.capsule();

    if (
      !currentCapsule ||
      !this.canInvite() ||
      this.invitationLoading()
    ) {
      return;
    }

    this.invitationLoading.set(
      true,
    );

    this.invitationError.set(
      null,
    );

    this.invitationCopied.set(
      false,
    );

    this.capsuleService
      .createInvitation(
        currentCapsule.id,
      )
      .pipe(
        finalize(() => {
          this.invitationLoading.set(
            false,
          );
        }),
      )
      .subscribe({
        next: (response) => {
          this.invitationUrl.set(
            `${window.location.origin}/invitations/${response.token}`,
          );
        },

        error: () => {
          this.invitationError.set(
            'No se ha podido generar la invitación.',
          );
        },
      });
  }

  async copyInvitation():
    Promise<void> {
    const url =
      this.invitationUrl();

    if (!url) {
      return;
    }

    try {
      await navigator.clipboard
        .writeText(url);

      this.invitationCopied.set(
        true,
      );
    } catch {
      this.invitationError.set(
        'No se ha podido copiar el enlace.',
      );
    }
  }

  createContribution(): void {
    const currentCapsule =
      this.capsule();

    if (
      !currentCapsule ||
      !this.canManageContributions() ||
      this.contributionSaving()
    ) {
      return;
    }

    if (
      this.contributionForm.invalid
    ) {
      this.contributionForm
        .markAllAsTouched();

      return;
    }

    const textContent =
      this.contributionForm.controls
        .textContent.value.trim();

    if (!textContent) {
      this.contributionForm.controls
        .textContent
        .setErrors({
          required: true,
        });

      return;
    }

    this.contributionSaving.set(
      true,
    );

    this.contributionError.set(
      null,
    );

    this.contributionSaved.set(
      false,
    );

    this.capsuleService
      .createTextContribution(
        currentCapsule.id,
        {
          textContent,
        },
      )
      .pipe(
        finalize(() => {
          this.contributionSaving.set(
            false,
          );
        }),
      )
      .subscribe({
        next: (contribution) => {
          this.contributions.update(
            (current) => [
              ...current,
              contribution,
            ],
          );

          this.contributionForm.reset({
            textContent: '',
          });

          this.contributionSaved.set(
            true,
          );
        },

        error: () => {
          this.contributionError.set(
            'No se ha podido guardar el mensaje.',
          );
        },
      });
  }

  startContributionEditing(
    contribution: Contribution,
  ): void {
    if (
      !this.canManageContributions() ||
      this.contributionSaving()
    ) {
      return;
    }

    this.editingContributionId.set(
      contribution.id,
    );

    this.contributionEditForm.reset({
      textContent:
        contribution.textContent ?? '',
    });

    this.contributionError.set(
      null,
    );

    this.contributionSaved.set(
      false,
    );
  }

  cancelContributionEditing():
    void {
    if (
      this.contributionSaving()
    ) {
      return;
    }

    this.editingContributionId.set(
      null,
    );

    this.contributionEditForm.reset({
      textContent: '',
    });
  }

  saveContribution(
    contributionId: string,
  ): void {
    const currentCapsule =
      this.capsule();

    if (
      !currentCapsule ||
      !this.canManageContributions() ||
      this.contributionSaving()
    ) {
      return;
    }

    if (
      this.contributionEditForm.invalid
    ) {
      this.contributionEditForm
        .markAllAsTouched();

      return;
    }

    const textContent =
      this.contributionEditForm
        .controls
        .textContent.value.trim();

    if (!textContent) {
      this.contributionEditForm
        .controls
        .textContent
        .setErrors({
          required: true,
        });

      return;
    }

    this.contributionSaving.set(
      true,
    );

    this.contributionError.set(
      null,
    );

    this.contributionSaved.set(
      false,
    );

    this.capsuleService
      .updateTextContribution(
        currentCapsule.id,
        contributionId,
        {
          textContent,
        },
      )
      .pipe(
        finalize(() => {
          this.contributionSaving.set(
            false,
          );
        }),
      )
      .subscribe({
        next: (updated) => {
          this.contributions.update(
            (current) =>
              current.map(
                (contribution) =>
                  contribution.id ===
                  updated.id
                    ? updated
                    : contribution,
              ),
          );

          this.editingContributionId.set(
            null,
          );

          this.contributionSaved.set(
            true,
          );
        },

        error: () => {
          this.contributionError.set(
            'No se ha podido actualizar el mensaje.',
          );
        },
      });
  }

  deleteContribution(
    contributionId: string,
  ): void {
    const currentCapsule =
      this.capsule();

    if (
      !currentCapsule ||
      !this.canManageContributions() ||
      this.deletingContributionId()
    ) {
      return;
    }

    this.deletingContributionId.set(
      contributionId,
    );

    this.contributionError.set(
      null,
    );

    this.contributionSaved.set(
      false,
    );

    this.capsuleService
      .deleteContribution(
        currentCapsule.id,
        contributionId,
      )
      .pipe(
        finalize(() => {
          this.deletingContributionId.set(
            null,
          );
        }),
      )
      .subscribe({
        next: () => {
          this.contributions.update(
            (current) =>
              current.filter(
                (contribution) =>
                  contribution.id !==
                  contributionId,
              ),
          );

          if (
            this.editingContributionId() ===
            contributionId
          ) {
            this.cancelContributionEditing();
          }
        },

        error: () => {
          this.contributionError.set(
            'No se ha podido eliminar el mensaje.',
          );
        },
      });
  }

  backToDashboard(): void {
    void this.router.navigate([
      '/dashboard',
    ]);
  }

  getCountdown(
    opensAt: string,
  ): string {
    const difference =
      new Date(
        opensAt,
      ).getTime() -
      Date.now();

    if (difference <= 0) {
      return 'Lista para abrir';
    }

    const totalMinutes =
      Math.floor(
        difference / 60_000,
      );

    const days =
      Math.floor(
        totalMinutes / 1_440,
      );

    const hours =
      Math.floor(
        (
          totalMinutes %
          1_440
        ) / 60,
      );

    const minutes =
      totalMinutes % 60;

    if (days > 0) {
      return `${days}d ${hours}h`;
    }

    if (hours > 0) {
      return `${hours}h ${minutes}m`;
    }

    return `${minutes}m`;
  }

  typeLabel(
    type: CapsuleType,
  ): string {
    return type === 'PERSONAL'
      ? 'Personal'
      : 'Compartida';
  }

  roleLabel(
    role: CapsuleRole,
  ): string {
    return role === 'OWNER'
      ? 'Propietario'
      : 'Colaborador';
  }

  statusLabel(
    status: CapsuleStatus,
  ): string {
    switch (status) {
      case 'COLLECTING':
        return 'Recopilando';

      case 'SEALED':
        return 'Sellada';

      case 'OPENED':
        return 'Abierta';
    }
  }

  private loadCapsule(
    capsuleId: string,
  ): void {
    this.isLoading.set(true);
    this.loadError.set(null);

    this.capsuleService
      .getCapsule(capsuleId)
      .pipe(
        finalize(() => {
          this.isLoading.set(
            false,
          );
        }),
      )
      .subscribe({
        next: (capsule) => {
          this.capsule.set(
            capsule,
          );

          this.loadContributions(
            capsule.id,
          );

          if (
            capsule.type ===
            'SHARED'
          ) {
            this.loadParticipants(
              capsule.id,
            );
          } else {
            this.participants.set(
              [],
            );
          }
        },

        error: () => {
          this.loadError.set(
            'No se ha podido cargar la cápsula.',
          );
        },
      });
  }

  private loadParticipants(
    capsuleId: string,
  ): void {
    this.participantsLoading.set(
      true,
    );

    this.capsuleService
      .getParticipants(
        capsuleId,
      )
      .pipe(
        finalize(() => {
          this.participantsLoading.set(
            false,
          );
        }),
      )
      .subscribe({
        next: (participants) => {
          this.participants.set(
            participants,
          );
        },

        error: () => {
          this.participants.set(
            [],
          );
        },
      });
  }

  private loadContributions(
    capsuleId: string,
  ): void {
    this.contributionsLoading.set(
      true,
    );

    this.contributionError.set(
      null,
    );

    this.capsuleService
      .getTextContributions(
        capsuleId,
      )
      .pipe(
        finalize(() => {
          this.contributionsLoading.set(
            false,
          );
        }),
      )
      .subscribe({
        next: (contributions) => {
          this.contributions.set(
            contributions,
          );
        },

        error: () => {
          this.contributions.set([]);

          this.contributionError.set(
            'No se han podido cargar tus mensajes.',
          );
        },
      });
  }
}