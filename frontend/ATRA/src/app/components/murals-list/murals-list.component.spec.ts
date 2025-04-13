import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MuralsListComponent } from './murals-list.component';

describe('MuralsComponent', () => {
  let component: MuralsListComponent;
  let fixture: ComponentFixture<MuralsListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MuralsListComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MuralsListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
