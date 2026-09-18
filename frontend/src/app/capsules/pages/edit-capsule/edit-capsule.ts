import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CapsuleService } from '../../services/capsule.service';
import { UpdateCapsuleRequest } from '../../models/capsule.models';

@Component({
  selector: 'app-edit-capsule',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './edit-capsule.html',
  styleUrls: ['./edit-capsule.scss']
})
export class EditCapsule implements OnInit {
  private fb = inject(FormBuilder);
  private capsuleService = inject(CapsuleService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  capsuleId = signal<string>('');
  isLoading = signal<boolean>(true);
  isSubmitting = signal<boolean>(false);
  errorMessage = signal<string>('');

  editForm = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    description: ['', [Validators.maxLength(255)]],
    date: ['', [Validators.required]],
    time: ['', [Validators.required]],
    timezone: [Intl.DateTimeFormat().resolvedOptions().timeZone, [Validators.required]]
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.capsuleId.set(id);
      this.loadCapsule(id);
    } else {
      this.router.navigate(['/dashboard']);
    }
  }

  loadCapsule(id: string): void {
    this.capsuleService.getCapsule(id).subscribe({
      next: (capsule: any) => {
        const openDate = new Date(capsule.opensAt);
        const dateStr = openDate.toISOString().split('T')[0];
        const timeStr = openDate.toTimeString().substring(0, 5);

        this.editForm.patchValue({
          title: capsule.title,
          description: capsule.description || '',
          date: dateStr,
          time: timeStr,
          timezone: capsule.timezone
        });
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Error al cargar la cápsula');
        this.isLoading.set(false);
      }
    });
  }

  onSubmit(): void {
    this.errorMessage.set('');

    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    const formValues = this.editForm.getRawValue();
    const opensAt = new Date(`${formValues.date}T${formValues.time}:00`).toISOString();

    if (new Date(opensAt) <= new Date()) {
      this.errorMessage.set('La fecha de apertura debe ser futura');
      return;
    }

    this.isSubmitting.set(true);

    const request: UpdateCapsuleRequest = {
      title: formValues.title,
      description: formValues.description,
      opensAt: opensAt,
      timezone: formValues.timezone
    };

    this.capsuleService.updateCapsule(this.capsuleId(), request).subscribe({
      next: () => {
        this.router.navigate(['/dashboard']);
      },
      error: () => {
        this.errorMessage.set('Error al actualizar la cápsula');
        this.isSubmitting.set(false);
      }
    });
  }
}