import {
  ChangeDetectionStrategy,
  Component,
  inject,
  signal,
} from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import {
  Router,
  RouterLink,
} from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from '../../services/auth.service';
import { registerErrorMessage } from '../../utils/auth-errors';
import { passwordsMatchValidator } from '../../validators/passwords-match.validator';

@Component({
  selector: 'app-register',
  imports: [
    ReactiveFormsModule,
    RouterLink,
  ],
  templateUrl: './register.html',
  styleUrl: './register.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Register {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.formBuilder.nonNullable.group(
    {
      email: [
        '',
        [
          Validators.required,
          Validators.email,
          Validators.maxLength(320),
        ],
      ],
      password: [
        '',
        [
          Validators.required,
          Validators.minLength(8),
          Validators.maxLength(72),
        ],
      ],
      confirmPassword: [
        '',
        [
          Validators.required,
        ],
      ],
    },
    {
      validators: passwordsMatchValidator,
    },
  );

  submit(): void {
    if (this.form.invalid || this.isSubmitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const { email, password } =
      this.form.getRawValue();

    this.authService
      .registerAndLogin({
        email,
        password,
      })
      .pipe(
        finalize(() => {
          this.isSubmitting.set(false);
        }),
      )
      .subscribe({
        next: () => {
          void this.router.navigateByUrl(
            '/dashboard',
          );
        },
        error: (error: unknown) => {
          this.errorMessage.set(
            registerErrorMessage(error),
          );
        },
      });
  }
}