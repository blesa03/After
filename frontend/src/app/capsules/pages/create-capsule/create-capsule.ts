import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { CapsuleService } from '../../services/capsule.service';
import { CreateCapsuleRequest } from '../../models/capsule.models';
import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export const futureDateValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const date = control.get('date')?.value;
  const time = control.get('time')?.value;

  if (date && time) {
    const selectedDate = new Date(`${date}T${time}`);
    if (selectedDate.getTime() <= new Date().getTime()) {
      return { notFuture: true }; 
    }
  }
  return null;
};

@Component({
  selector: 'app-create-capsule',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  styleUrl: './create-capsule.scss',
  templateUrl: './create-capsule.html',
})
export class CreateCapsule {
  private fb = inject(FormBuilder);
  private capsuleService = inject(CapsuleService);
  private router = inject(Router);

  isSubmitting = false;
  errorMessage = '';

  capsuleForm = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(255)]], 
    description: [''], 
    type: ['PERSONAL', Validators.required],
    date: ['', Validators.required],
    time: ['', Validators.required],
    timezone: [Intl.DateTimeFormat().resolvedOptions().timeZone, Validators.required]
  }, { validators: futureDateValidator }); 

  onSubmit() {

    console.log("¿El formulario es válido?:", this.capsuleForm.valid);
  console.log("Valores que lee Angular:", this.capsuleForm.value);
    if (this.capsuleForm.invalid) {
      this.capsuleForm.markAllAsTouched();
      return;
    }

    const formValues = this.capsuleForm.value;
    const openDateISO = new Date(`${formValues.date}T${formValues.time}`).toISOString();
    
    const requestPayload = {
      title: formValues.title!,
      description: formValues.description!,
      type: formValues.type! as 'PERSONAL' | 'SHARED',
      opensAt: openDateISO, 
      timezone: formValues.timezone!
    };

    this.capsuleService.createCapsule(requestPayload).subscribe({
      next: (response) => {
        this.router.navigate(['/capsules', response.id]);
      },
      error: (err) => {
        console.error('Error al crear la cápsula', err);
      }
    });
  }
}