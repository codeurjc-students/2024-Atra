import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MuralsSettingsComponent } from './murals-settings.component';

describe('MuralSettingsComponent', () => {
  let component: MuralsSettingsComponent;
  let fixture: ComponentFixture<MuralsSettingsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MuralsSettingsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MuralsSettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
