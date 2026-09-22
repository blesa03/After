import {
  ChangeDetectionStrategy,
  Component,
  inject,
  signal,
} from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { CreateCapsuleRequest } from '../../models/capsule.models';
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

function combineDateAndTime(
  date: string,
  time: string,
): Date {
  return new Date(
    `${date}T${time}`,
  );
}

export const futureDateValidator:
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
  selector: 'app-create-capsule',
  imports: [
    ReactiveFormsModule,
  ],
  templateUrl:
    './create-capsule.html',
  styleUrl:
    './create-capsule.scss',
  changeDetection:
    ChangeDetectionStrategy.OnPush,
})
export class CreateCapsule {
  private readonly formBuilder =
    inject(FormBuilder);

  private readonly capsuleService =
    inject(CapsuleService);

  private readonly router =
    inject(Router);

  readonly isSubmitting =
    signal(false);

  readonly errorMessage =
    signal<string | null>(null);

  readonly capsuleForm =
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

        type: [
          'PERSONAL' as
            | 'PERSONAL'
            | 'SHARED',
          Validators.required,
        ],

        date: [
          '',
          Validators.required,
        ],

        time: [
          '',
          Validators.required,
        ],

        timezone: [
          Intl.DateTimeFormat()
            .resolvedOptions()
            .timeZone,
          Validators.required,
        ],
      },
      {
        validators:
          futureDateValidator,
      },
    );

  get minDate(): string {
    return toDateInputValue(
      new Date(),
    );
  }

  onTimeChange(): void {
    const date =
      this.capsuleForm.controls
        .date.value;

    const time =
      this.capsuleForm.controls
        .time.value;

    if (!date || !time) {
      return;
    }

    const today =
      toDateInputValue(
        new Date(),
      );

    if (date !== today) {
      this.capsuleForm
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
      this.capsuleForm
        .updateValueAndValidity();

      return;
    }

    const tomorrow =
      new Date();

    tomorrow.setDate(
      tomorrow.getDate() + 1,
    );

    this.capsuleForm.controls
      .date
      .setValue(
        toDateInputValue(
          tomorrow,
        ),
      );

    this.capsuleForm
      .updateValueAndValidity();
  }

  onDateChange(): void {
    this.capsuleForm
      .updateValueAndValidity();
  }

  onSubmit(): void {
    if (
      this.capsuleForm.invalid ||
      this.isSubmitting()
    ) {
      this.capsuleForm
        .markAllAsTouched();

      return;
    }

    const values =
      this.capsuleForm
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
      this.capsuleForm
        .setErrors({
          notFuture: true,
        });

      return;
    }

    const payload:
      CreateCapsuleRequest = {
        title:
          values.title.trim(),

        description:
          values.description.trim(),

        type:
          values.type,

        opensAt:
          opensAt.toISOString(),

        timezone:
          values.timezone.trim(),
      };

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    this.capsuleService
      .createCapsule(payload)
      .pipe(
        finalize(() => {
          this.isSubmitting.set(
            false,
          );
        }),
      )
      .subscribe({
        next: (response) => {
          void this.router.navigate([
            '/capsules',
            response.id,
          ]);
        },

        error: () => {
          this.errorMessage.set(
            'Hubo un error al crear la cápsula. Inténtalo de nuevo.',
          );
        },
      });
  }
}