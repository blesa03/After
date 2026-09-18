import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EditCapsule } from './edit-capsule';
import { ReactiveFormsModule } from '@angular/forms';
import { CapsuleService } from '../../services/capsule.service';
import { Router, ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

describe('EditCapsule', () => {
  let component: EditCapsule;
  let fixture: ComponentFixture<EditCapsule>;
  let capsuleServiceSpy: any;
  let routerSpy: any;
  let routeStub: any;

  beforeEach(async () => {
    capsuleServiceSpy = {
      getCapsule: vi.fn(),
      updateCapsule: vi.fn()
    };
    
    routerSpy = {
      navigate: vi.fn()
    };
    
    routeStub = {
      snapshot: {
        paramMap: {
          get: vi.fn().mockReturnValue('123')
        }
      }
    };

    capsuleServiceSpy.getCapsule.mockReturnValue(of({
      id: '123',
      title: 'Cápsula Original',
      description: 'Descripción Original',
      opensAt: '2050-01-01T10:00:00Z',
      timezone: 'UTC'
    }));

    await TestBed.configureTestingModule({
      imports: [EditCapsule, ReactiveFormsModule],
      providers: [
        { provide: CapsuleService, useValue: capsuleServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: routeStub }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(EditCapsule);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deberia inicializar el componente y cargar los datos de la capsula', () => {
    expect(component).toBeTruthy();
    expect(capsuleServiceSpy.getCapsule).toHaveBeenCalledWith('123');
    expect(component.editForm.get('title')?.value).toBe('Cápsula Original');
  });

  it('deberia mostrar error si la fecha modificada esta en el pasado', () => {
    component.editForm.patchValue({
      title: 'Título Válido',
      date: '2000-01-01',
      time: '12:00',
      timezone: 'UTC'
    });
    
    component.onSubmit();
    
    expect(component.errorMessage()).toBe('La fecha de apertura debe ser futura');
    expect(capsuleServiceSpy.updateCapsule).not.toHaveBeenCalled();
  });

  it('deberia llamar al servicio updateCapsule y navegar al dashboard si el formulario es valido', () => {
    const futureDate = new Date();
    futureDate.setFullYear(futureDate.getFullYear() + 1);
    const dateStr = futureDate.toISOString().split('T')[0];

    component.editForm.patchValue({
      title: 'Título Actualizado',
      description: 'Descripción Actualizada',
      date: dateStr,
      time: '15:30',
      timezone: 'Europe/Madrid'
    });

    capsuleServiceSpy.updateCapsule.mockReturnValue(of({}));
    
    component.onSubmit();

    expect(capsuleServiceSpy.updateCapsule).toHaveBeenCalled();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('deberia mostrar mensaje exacto de error si el servicio de actualizacion falla', () => {
    const futureDate = new Date();
    futureDate.setFullYear(futureDate.getFullYear() + 1);
    
    component.editForm.patchValue({
      title: 'Fallo',
      date: futureDate.toISOString().split('T')[0],
      time: '10:00',
      timezone: 'UTC'
    });

    capsuleServiceSpy.updateCapsule.mockReturnValue(throwError(() => new Error('Network error')));
    
    component.onSubmit();

    expect(component.errorMessage()).toBe('Error al actualizar la cápsula');
    expect(component.isSubmitting()).toBe(false);
  });
});