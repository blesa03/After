import { Component, OnInit, inject, ChangeDetectionStrategy } from '@angular/core';
import { Router } from '@angular/router';
import { CapsuleService } from '../../../capsules/services/capsule.service';
import { CapsuleSummary } from '../../../capsules/models/capsule.models';
import { DatePipe } from '@angular/common';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe]
})
export class Dashboard implements OnInit {
  private capsuleService = inject(CapsuleService);
  private router = inject(Router);

  capsules: CapsuleSummary[] = [];
  state: 'loading' | 'content' | 'empty' | 'error' = 'loading';

  ngOnInit() {
    this.capsuleService.getCapsules().subscribe({
      next: (data: CapsuleSummary[]) => {
        this.capsules = data;
        this.state = data.length > 0 ? 'content' : 'empty';
      },
      error: () => this.state = 'error'
    });
  }

  goToDetail(id: string) {
    this.router.navigate(['/capsules', id]);
  }

  goToCreate() {
    this.router.navigate(['/capsules/create']);
  }
}