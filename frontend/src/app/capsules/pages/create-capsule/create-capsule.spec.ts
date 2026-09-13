import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CreateCapsule } from './create-capsule';

describe('CreateCapsule', () => {
  let component: CreateCapsule;
  let fixture: ComponentFixture<CreateCapsule>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CreateCapsule],
    }).compileComponents();

    fixture = TestBed.createComponent(CreateCapsule);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
