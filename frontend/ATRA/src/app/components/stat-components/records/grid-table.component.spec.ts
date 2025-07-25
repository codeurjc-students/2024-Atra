import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GridTableComponent } from './grid-table.component';

describe('RecordsComponent', () => {
  let component: GridTableComponent;
  let fixture: ComponentFixture<GridTableComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GridTableComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GridTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
