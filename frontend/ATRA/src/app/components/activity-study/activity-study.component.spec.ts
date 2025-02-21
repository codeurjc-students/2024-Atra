import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ActivityStudyComponent } from './activity-study.component';

describe('ActivityComponent', () => {
  let component: ActivityStudyComponent;
  let fixture: ComponentFixture<ActivityStudyComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ActivityStudyComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ActivityStudyComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
