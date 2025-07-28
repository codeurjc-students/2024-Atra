import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ActivitySelectComponent } from './activity-select.component';

describe('ActivityComponent', () => {
  let component: ActivitySelectComponent;
  let fixture: ComponentFixture<ActivitySelectComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ActivitySelectComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ActivitySelectComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
