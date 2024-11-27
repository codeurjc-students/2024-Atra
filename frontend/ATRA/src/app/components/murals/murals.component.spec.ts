import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MuralsComponent } from './murals.component';

describe('MuralsComponent', () => {
  let component: MuralsComponent;
  let fixture: ComponentFixture<MuralsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MuralsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MuralsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
