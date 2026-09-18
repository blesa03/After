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

export const futureDateValidator: ValidatorFn = (
  control: AbstractControl,
): ValidationErrors | null => {
  const date = control.get('date')?.value;
  const time = control.get('time')?.value;

  if (!date || !time) {
    return null;
  }

  const selectedDate = new Date(`${date}T${time}`);

  if (selectedDate.getTime() <= Date.now()) {
    return { notFuture: true };
  }

  return null;
};

@Component({
  selector: 'app-create-capsule',
  imports: [ReactiveFormsModule],
  templateUrl: './create-capsule.html',
  styleUrl: './create-capsule.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateCapsule {
  private readonly formBuilder = inject(FormBuilder);
  private readonly capsuleService = inject(CapsuleService);
  private readonly router = inject(Router);

  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly capsuleForm = this.formBuilder.nonNullable.group(
    {
      title: [
        '',
        [
          Validators.required,
          Validators.maxLength(255),
        ],
      ],
      description: [''],
      type: [
        'PERSONAL' as 'PERSONAL' | 'SHARED',
        Validators.required,
      ],
      date: ['', Validators.required],
      time: ['', Validators.required],
      timezone: [
        Intl.DateTimeFormat().resolvedOptions().timeZone,
        Validators.required,
      ],
    },
    {
      validators: futureDateValidator,
    },
  );

  onSubmit(): void {
    
    if (
      this.capsuleForm.invalid ||
      this.isSubmitting()
    ) {
      this.capsuleForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const formValues =
      this.capsuleForm.getRawValue();

    const requestPayload: CreateCapsuleRequest = {
      title: formValues.title,
      description: formValues.description,
      type: formValues.type,
      opensAt: new Date(
        `${formValues.date}T${formValues.time}`,
      ).toISOString(),
      timezone: formValues.timezone,
    };

    this.capsuleService
      .createCapsule(requestPayload)
      .pipe(
        finalize(() => {
          this.isSubmitting.set(false);
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