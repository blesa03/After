import {
  ChangeDetectionStrategy,
  Component,
  inject,
} from '@angular/core';

import { AuthService } from '../../../auth/services/auth.service';

@Component({
  selector: 'app-dashboard',
  imports: [],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Dashboard {
  readonly currentUser =
    inject(AuthService).currentUser;
}