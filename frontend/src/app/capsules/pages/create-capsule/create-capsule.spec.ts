import {
  ComponentFixture,
  TestBed,
} from '@angular/core/testing';
import { Router } from '@angular/router';
import {
  of,
  throwError,
} from 'rxjs';
import { vi } from 'vitest';

import { CapsuleService } from '../../services/capsule.service';
import { CreateCapsule } from './create-capsule';

describe('CreateCapsule', () => {
  let component:
    CreateCapsule;

  let fixture:
    ComponentFixture<CreateCapsule>;

  let capsuleServiceSpy: {
    createCapsule:
      ReturnType<typeof vi.fn>;
  };

  let routerSpy: {
    navigate:
      ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    capsuleServiceSpy = {
      createCapsule:
        vi.fn(),
    };

    routerSpy = {
      navigate:
        vi.fn(),
    };

    await TestBed
      .configureTestingModule({
        imports: [
          CreateCapsule,
        ],
        providers: [
          {
            provide:
              CapsuleService,
            useValue:
              capsuleServiceSpy,
          },
          {
            provide:
              Router,
            useValue:
              routerSpy,
          },
        ],
      })
      .compileComponents();

    fixture =
      TestBed.createComponent(
        CreateCapsule,
      );

    component =
      fixture.componentInstance;

    fixture.detectChanges();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  function toDateInputValue(
    date: Date,
  ): string {
    const year =
      date.getFullYear();

    const month =
      String(
        date.getMonth() + 1,
      ).padStart(2, '0');

    const day =
      String(
        date.getDate(),
      ).padStart(2, '0');

    return `${year}-${month}-${day}`;
  }

  it('should create the component', () => {
    expect(
      component,
    ).toBeTruthy();
  });

  it('should expose today as minimum date', () => {
    expect(
      component.minDate,
    ).toBe(
      toDateInputValue(
        new Date(),
      ),
    );
  });

  it('should invalidate a past date and time', () => {
    component.capsuleForm.patchValue({
      title:
        'Viaje',
      date:
        '2000-01-01',
      time:
        '12:00',
    });

    component.capsuleForm
      .updateValueAndValidity();

    expect(
      component.capsuleForm
        .hasError(
          'notFuture',
        ),
    ).toBe(true);
  });

  it('should move the date to tomorrow when a past time is selected for today', () => {
    vi.useFakeTimers();

    vi.setSystemTime(
      new Date(
        2026,
        8,
        23,
        12,
        0,
        0,
      ),
    );

    component.capsuleForm.patchValue({
      date:
        '2026-09-23',
      time:
        '10:00',
    });

    component.onTimeChange();

    expect(
      component.capsuleForm.controls
        .date.value,
    ).toBe(
      '2026-09-24',
    );

    expect(
      component.capsuleForm
        .hasError(
          'notFuture',
        ),
    ).toBe(false);
  });

  it('should keep today when a future time is selected', () => {
    vi.useFakeTimers();

    vi.setSystemTime(
      new Date(
        2026,
        8,
        23,
        12,
        0,
        0,
      ),
    );

    component.capsuleForm.patchValue({
      date:
        '2026-09-23',
      time:
        '14:00',
    });

    component.onTimeChange();

    expect(
      component.capsuleForm.controls
        .date.value,
    ).toBe(
      '2026-09-23',
    );

    expect(
      component.capsuleForm
        .hasError(
          'notFuture',
        ),
    ).toBe(false);
  });

  it('should not change a future date when time changes', () => {
    vi.useFakeTimers();

    vi.setSystemTime(
      new Date(
        2026,
        8,
        23,
        12,
        0,
        0,
      ),
    );

    component.capsuleForm.patchValue({
      date:
        '2026-09-25',
      time:
        '01:00',
    });

    component.onTimeChange();

    expect(
      component.capsuleForm.controls
        .date.value,
    ).toBe(
      '2026-09-25',
    );
  });

  it('should call createCapsule and navigate on valid submit', () => {
    const futureDate =
      new Date();

    futureDate.setDate(
      futureDate.getDate() + 1,
    );

    const date =
      toDateInputValue(
        futureDate,
      );

    const time =
      '15:30';

    component.capsuleForm.patchValue({
      title:
        'Válida',

      description:
        'Descripción',

      type:
        'PERSONAL',

      date,

      time,

      timezone:
        'Europe/Madrid',
    });

    capsuleServiceSpy
      .createCapsule
      .mockReturnValue(
        of({
          id: '123',
        }),
      );

    component.onSubmit();

    expect(
      capsuleServiceSpy
        .createCapsule,
    ).toHaveBeenCalledWith({
      title:
        'Válida',

      description:
        'Descripción',

      type:
        'PERSONAL',

      opensAt:
        new Date(
          `${date}T${time}`,
        ).toISOString(),

      timezone:
        'Europe/Madrid',
    });

    expect(
      routerSpy.navigate,
    ).toHaveBeenCalledWith([
      '/capsules',
      '123',
    ]);

    expect(
      component.isSubmitting(),
    ).toBe(false);
  });

  it('should not submit when the opening date is invalid', () => {
    component.capsuleForm.patchValue({
      title:
        'Inválida',

      date:
        '2000-01-01',

      time:
        '12:00',

      timezone:
        'Europe/Madrid',
    });

    component.onSubmit();

    expect(
      capsuleServiceSpy
        .createCapsule,
    ).not.toHaveBeenCalled();
  });

  it('should show an error message when creation fails', () => {
    const futureDate =
      new Date();

    futureDate.setDate(
      futureDate.getDate() + 1,
    );

    component.capsuleForm.patchValue({
      title:
        'Fallida',

      date:
        toDateInputValue(
          futureDate,
        ),

      time:
        '12:00',

      timezone:
        'Europe/Madrid',
    });

    capsuleServiceSpy
      .createCapsule
      .mockReturnValue(
        throwError(
          () =>
            new Error(
              'Creation failed',
            ),
        ),
      );

    component.onSubmit();

    expect(
      component.errorMessage(),
    ).toContain(
      'Hubo un error',
    );

    expect(
      component.isSubmitting(),
    ).toBe(false);
  });
});