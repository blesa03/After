import {
  ComponentFixture,
  TestBed,
} from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { CapsuleService } from '../../services/capsule.service';
import { CreateCapsule } from './create-capsule';

describe('CreateCapsule', () => {
  let component: CreateCapsule;
  let fixture: ComponentFixture<CreateCapsule>;

  let capsuleServiceSpy: {
    createCapsule: ReturnType<typeof vi.fn>;
  };

  let routerSpy: {
    navigate: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    capsuleServiceSpy = {
      createCapsule: vi.fn(),
    };

    routerSpy = {
      navigate: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [CreateCapsule],
      providers: [
        {
          provide: CapsuleService,
          useValue: capsuleServiceSpy,
        },
        {
          provide: Router,
          useValue: routerSpy,
        },
      ],
    }).compileComponents();

    fixture =
      TestBed.createComponent(CreateCapsule);
    component = fixture.componentInstance;

    fixture.detectChanges();
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should invalidate form if date is in the past', () => {
    component.capsuleForm.patchValue({
      title: 'Viaje',
      date: '2000-01-01',
      time: '12:00',
    });

    component.capsuleForm.updateValueAndValidity();

    expect(
      component.capsuleForm.hasError('notFuture'),
    ).toBe(true);
  });

  it('should call createCapsule service and navigate on valid submit', () => {
    const futureDate = new Date();

    futureDate.setFullYear(
      futureDate.getFullYear() + 1,
    );

    const date =
      futureDate.toISOString().split('T')[0];

    component.capsuleForm.patchValue({
      title: 'Válida',
      type: 'PERSONAL',
      date,
      time: '15:30',
      timezone: 'Europe/Madrid',
    });

    capsuleServiceSpy.createCapsule.mockReturnValue(
      of({ id: '123' }),
    );

    component.onSubmit();

    expect(
      capsuleServiceSpy.createCapsule,
    ).toHaveBeenCalledWith({
      title: 'Válida',
      description: '',
      type: 'PERSONAL',
      opensAt: new Date(
        `${date}T15:30`,
      ).toISOString(),
      timezone: 'Europe/Madrid',
    });

    expect(routerSpy.navigate).toHaveBeenCalledWith([
      '/capsules',
      '123',
    ]);

    expect(component.isSubmitting()).toBe(false);
  });

  it('should show error message if service fails', () => {
    const futureDate = new Date();

    futureDate.setFullYear(
      futureDate.getFullYear() + 1,
    );

    component.capsuleForm.patchValue({
      title: 'Fallida',
      date:
        futureDate
          .toISOString()
          .split('T')[0],
      time: '10:00',
    });

    capsuleServiceSpy.createCapsule.mockReturnValue(
      throwError(() => new Error('Error')),
    );

    component.onSubmit();

    expect(component.errorMessage()).toContain(
      'Hubo un error',
    );

    expect(component.isSubmitting()).toBe(false);
  });
});