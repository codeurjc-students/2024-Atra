import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MuralsNewComponent } from './murals-new.component';

describe('MuralsNewComponent', () => {
  let component: MuralsNewComponent;
  let fixture: ComponentFixture<MuralsNewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MuralsNewComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MuralsNewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
