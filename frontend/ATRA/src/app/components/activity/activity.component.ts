import { ActivityService } from './../../services/activity.service';
import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, ElementRef, HostListener, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Activity } from '../../models/activity.model';
import { FormsModule } from '@angular/forms';
import { NgxChartsModule } from '@swimlane/ngx-charts';


@Component({
  selector: 'app-activity',
  standalone: true,
  imports: [CommonModule, FormsModule, NgxChartsModule],
  templateUrl: './activity.component.html',
  styleUrl: './activity.component.css'
})
export class ActivityComponent implements OnInit, AfterViewInit{
  @ViewChild('chartContainer', { static: true }) chartContainer!: ElementRef;

  id !: string;
  stats !: {name:string, value:string}[];
  activity !: Activity;
  dataset : {name:string, value:number}[] = [];
  currentMetric : string = "pace";
  view: [number, number] = [400, 400];

  ngAfterViewInit() {
    const containerWidth = this.chartContainer.nativeElement.offsetWidth;
    const containerHeight = this.chartContainer.nativeElement.offsetHeight;
    if (containerHeight == 0 || containerWidth == 0) {
       this.view = [800, 375]
       return
    }
    console.log([containerWidth, containerHeight])
    this.view = [containerWidth*0.8, containerHeight*0.8];
  }

  constructor(private route: ActivatedRoute, private router:Router, private activityService: ActivityService) {
  }
  resizeChart(): void {
    this.ngAfterViewInit();
  }


  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get("id");
    if (id===null) {
      this.router.navigate(["/error?reason=missingParameter"]);
      return;
    }
    this.id = id;

    this.activityService.get(this.id).subscribe({
      next: (act) => {
        console.log("subscribed")
        this.activity = this.activityService.process([act])[0];
        this.dataset = this.activity.getMetricData(this.currentMetric, this.activity)
        this.initStats();

      },
      error: (err) => {alert("There was an error fetching the activity. Try reloading the page.")}
    })
  }

  private initStats(){
    this.stats = this.activity.getOverview()
    console.log(this.stats)

  }


  // Update chart size on window resize
  @HostListener('window:resize', ['$event'])
  onResize() {
    this.ngAfterViewInit();
  }






  metrics: string[] = ['Pace', 'Speed', 'Cadence'];
  selectedMetric = this.metrics[0];

  goals = {
    worst: 6.45,
    best: 5.30,
    target: 6.00
  };

  ratings = [
    { name: 'Acceptable Time', value: '90%' },
    { name: 'Below Avg Time', value: '48%' },
    { name: 'Above Avg Time', value: '55%' },
    { name: 'σ', value: '0.2' }
  ];

}
