import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AnonInitComponent } from './anon-init.component';

describe('AnonInitComponent', () => {
  let component: AnonInitComponent;
  let fixture: ComponentFixture<AnonInitComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AnonInitComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(AnonInitComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
