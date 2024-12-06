import { ActivityService } from './../../services/activity.service';
import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Activity } from '../../models/activity.model';

@Component({
  selector: 'app-activity',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './activity.component.html',
  styleUrl: './activity.component.css'
})
export class ActivityComponent implements OnInit{
  id !: string;
  stats !: {name:string, value:string}[];
  activity !: Activity;


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
        this.initStats();

      },
      error: (err) => {alert("There was an error fetching the activity. Try reloading the page.")}
    })
  }

  private initStats(){
    this.stats = this.activity.getOverview()
    console.log(this.stats)

  }

}
