import {
  ChangeDetectionStrategy,
  Component,
  inject,
  signal,
} from '@angular/core';
import {
  Router,
  RouterLink,
  RouterOutlet,
} from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from '../../../auth/services/auth.service';

@Component({
  selector: 'app-layout',
  imports: [
    RouterLink,
    RouterOutlet,
  ],
  templateUrl: './app-layout.html',
  styleUrl: './app-layout.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppLayout {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly currentUser =
    this.authService.currentUser;

  readonly isLoggingOut = signal(false);

  logout(): void {
    if (this.isLoggingOut()) {
      return;
    }

    this.isLoggingOut.set(true);

    this.authService
      .logout()
      .pipe(
        finalize(() => {
          this.isLoggingOut.set(false);
          void this.router.navigateByUrl('/login');
        }),
      )
      .subscribe({
        error: () => {
          // Local session is cleared by AuthService even if
          // the server request fails.
        },
      });
  }
}