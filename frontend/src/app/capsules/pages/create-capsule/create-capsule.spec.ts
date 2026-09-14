import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CreateCapsule } from './create-capsule';
import { ReactiveFormsModule } from '@angular/forms';
import { CapsuleService } from '../../services/capsule.service';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';


declare const jasmine: any;

describe('CreateCapsule', () => {
  let component: CreateCapsule;
  let fixture: ComponentFixture<CreateCapsule>;
  
  let capsuleServiceSpy: any;
  let routerSpy: any;

  beforeEach(async () => {
    capsuleServiceSpy = jasmine.createSpyObj('CapsuleService', ['createCapsule']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [CreateCapsule, ReactiveFormsModule],
      providers: [
        { provide: CapsuleService, useValue: capsuleServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CreateCapsule);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should invalidate form if date is in the past', () => {
    component.capsuleForm.patchValue({
      title: 'Viaje a Roma',
      date: '2000-01-01',
      time: '12:00'
    });
    
    component.capsuleForm.updateValueAndValidity();
    

    expect(component.capsuleForm.hasError('notFuture')).toBe(true);
    expect(component.capsuleForm.invalid).toBe(true);
  });

  it('should call createCapsule service and navigate on valid submit', () => {
    const futureDate = new Date();
    futureDate.setFullYear(futureDate.getFullYear() + 1);
    const dateStr = futureDate.toISOString().split('T')[0];

    component.capsuleForm.patchValue({
      title: 'Cápsula válida',
      type: 'PERSONAL',
      date: dateStr,
      time: '15:30',
      timezone: 'Europe/Madrid'
    });

    capsuleServiceSpy.createCapsule.and.returnValue(of({ id: 'capsule-123' }));

    component.onSubmit();

    expect(capsuleServiceSpy.createCapsule).toHaveBeenCalled();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/capsules', 'capsule-123']);
    expect(component.isSubmitting).toBe(true);
  });

  it('should show error message if service fails', () => {
    const futureDate = new Date();
    futureDate.setFullYear(futureDate.getFullYear() + 1);
    
    component.capsuleForm.patchValue({
      title: 'Cápsula fallida',
      date: futureDate.toISOString().split('T')[0],
      time: '10:00'
    });

    capsuleServiceSpy.createCapsule.and.returnValue(throwError(() => new Error('API Error')));

    component.onSubmit();

    
    expect(component.isSubmitting).toBe(false);
    expect(component.errorMessage).toContain('Hubo un error al crear la cápsula');
  });
});