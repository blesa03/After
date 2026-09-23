import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import {
  ActivatedRoute,
  Router,
  RouterLink,
} from '@angular/router';
import {
  HttpErrorResponse,
} from '@angular/common/http';
import { finalize } from 'rxjs';

import { AuthService } from '../../../auth/services/auth.service';
import {
  InvitationPreview,
} from '../../models/capsule.models';
import { CapsuleService } from '../../services/capsule.service';

@Component({
  selector: 'app-invitation',
  imports: [
    DatePipe,
    RouterLink,
  ],
  templateUrl: './invitation.html',
  styleUrl: './invitation.scss',
  changeDetection:
    ChangeDetectionStrategy.OnPush,
})
export class InvitationPage
  implements OnInit {

  private readonly route =
    inject(ActivatedRoute);

  private readonly router =
    inject(Router);

  private readonly capsuleService =
    inject(CapsuleService);

  readonly authService =
    inject(AuthService);

  readonly invitation =
    signal<InvitationPreview | null>(
      null,
    );

  readonly token =
    signal('');

  readonly isLoading =
    signal(true);

  readonly isAccepting =
    signal(false);

  readonly errorMessage =
    signal<string | null>(
      null,
    );

  ngOnInit(): void {
    const token =
      this.route.snapshot.paramMap.get(
        'token',
      );

    if (!token) {
      this.isLoading.set(false);
      this.errorMessage.set(
        'La invitación no es válida.',
      );

      return;
    }

    this.token.set(token);

    this.capsuleService
      .getInvitation(token)
      .pipe(
        finalize(() => {
          this.isLoading.set(false);
        }),
      )
      .subscribe({
        next: (invitation) => {
          this.invitation.set(
            invitation,
          );
        },

        error: (
          error: HttpErrorResponse,
        ) => {
          this.handleLoadError(
            error,
          );
        },
      });
  }

  accept(): void {
    if (
      !this.authService
        .isAuthenticated() ||
      this.isAccepting()
    ) {
      return;
    }

    this.isAccepting.set(true);
    this.errorMessage.set(null);

    this.capsuleService
      .acceptInvitation(
        this.token(),
      )
      .pipe(
        finalize(() => {
          this.isAccepting.set(
            false,
          );
        }),
      )
      .subscribe({
        next: (response) => {
          void this.router.navigate([
            '/capsules',
            response.capsuleId,
          ]);
        },

        error: (
          error: HttpErrorResponse,
        ) => {
          if (error.status === 400) {
            this.errorMessage.set(
              'No se puede aceptar esta invitación. Puede que ya se haya utilizado, la cápsula esté sellada o ya seas miembro.',
            );

            return;
          }

          this.errorMessage.set(
            'No se ha podido aceptar la invitación.',
          );
        },
      });
  }

  loginUrl(): string {
    return (
      '/login?returnUrl=' +
      encodeURIComponent(
        this.router.url,
      )
    );
  }

  registerUrl(): string {
    return (
      '/register?returnUrl=' +
      encodeURIComponent(
        this.router.url,
      )
    );
  }

  private handleLoadError(
    error: HttpErrorResponse,
  ): void {
    if (error.status === 404) {
      this.errorMessage.set(
        'Esta invitación no existe o el enlace no es válido.',
      );

      return;
    }

    if (error.status === 400) {
      this.errorMessage.set(
        'Esta invitación ya no está disponible. Puede que haya sido utilizada, revocada o que la cápsula ya esté sellada.',
      );

      return;
    }

    this.errorMessage.set(
      'No se ha podido cargar la invitación.',
    );
  }
}