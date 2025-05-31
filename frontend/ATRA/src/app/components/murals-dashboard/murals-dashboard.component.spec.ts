import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MuralsDashboardComponent } from './murals-dashboard.component';


describe('MuralsDashboardComponent', () => {
  let component: MuralsDashboardComponent;
  let fixture: ComponentFixture<MuralsDashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MuralsDashboardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MuralsDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
