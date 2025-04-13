import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MuralsCategoryComponent } from './murals-category.component';

describe('MuralsCategoryComponent', () => {
  let component: MuralsCategoryComponent;
  let fixture: ComponentFixture<MuralsCategoryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MuralsCategoryComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MuralsCategoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
