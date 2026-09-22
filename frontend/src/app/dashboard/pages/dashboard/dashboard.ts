import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';

import { CapsuleSummary } from '../../../capsules/models/capsule.models';
import { CapsuleService } from '../../../capsules/services/capsule.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe],
})
export class Dashboard implements OnInit {
  private readonly capsuleService =
    inject(CapsuleService);

  private readonly router =
    inject(Router);

  readonly capsules =
    signal<CapsuleSummary[]>([]);

  readonly state =
    signal<
      'loading' |
      'content' |
      'empty' |
      'error'
    >('loading');

  ngOnInit(): void {
    this.capsuleService
      .getCapsules()
      .subscribe({
        next: (
          data: CapsuleSummary[],
        ) => {
          this.capsules.set(data);

          this.state.set(
            data.length > 0
              ? 'content'
              : 'empty',
          );
        },
        error: () => {
          this.state.set('error');
        },
      });
  }

  goToDetail(id: string): void {
    void this.router.navigate([
      '/capsules',
      id,
    ]);
  }

  goToCreate(): void {
    void this.router.navigate([
      '/capsules/create',
    ]);
  }
}