import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule }  from '@angular/common';
import { CapsuleService } from '../../../capsules/services/capsule.service';
import { CapsuleSummary } from '../../../capsules/models/capsule.models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.scss']
})
export class DashboardComponent implements OnInit {
  private capsuleService = inject(CapsuleService);
  private router = inject(Router);

  capsules: CapsuleSummary[] = [];
  state: 'loading' | 'content' | 'empty' | 'error' = 'loading';

  ngOnInit() {
    this.capsuleService.getCapsules().subscribe({
      next: (data) => {
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