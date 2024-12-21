import { ActivityService } from './../../services/activity.service';
import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
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
export class ActivityComponent implements OnInit{
  id !: string;
  stats !: {name:string, value:string}[];
  activity !: Activity;
  dataset : {name:string, value:number}[] = [];
  currentMetric : string = "pace";


  constructor(private route: ActivatedRoute, private router:Router, private activityService: ActivityService) {
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
