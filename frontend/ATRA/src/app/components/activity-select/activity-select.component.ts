import { Activity } from './../../models/activity.model';
import { ActivityService } from './../../services/activity.service';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { RouteService } from '../../services/route.service';

@Component({
  selector: 'app-activity-select',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './activity-select.component.html',
  styleUrl: './activity-select.component.css'
})
export class ActivitySelectComponent implements OnInit{
  selected: Set<number> = new Set();
  shouldSelectAll: boolean = true;
  @Input() activities !: Activity[];
  @Input() submit: () => void = () => this.defaultSubmit();
  @Output() emitter = new EventEmitter<Set<number>>();


  onSubmit(){
    this.emitter.emit(this.selected)
    this.submit()
  }


  columns: string[] = ['Name', 'Date', 'Route', 'Time', 'Distance'];

  constructor(private router: Router, private activityService: ActivityService, private routeService: RouteService){}

  ngOnInit(): void {
    if (this.activities!=null) return
    this.activityService.getAuthenticatedUserActivities().subscribe({
      next: (value) => this.activities = this.activityService.process(value),
      error: (err) => {alert("There was an error fetching your activities"); console.log("There was an error fetching the user's activities", err)}
    })
  }

  toggle(id: number) {
    if (this.selected.size===this.activities.length) {this.shouldSelectAll = true}

    this.selected.has(id) ? this.selected.delete(id):this.selected.add(id)

    if (this.selected.size===this.activities.length) {this.shouldSelectAll = false}
  }

  selectAll() {
    console.log(this.activities)
    if (this.shouldSelectAll) {
      this.activities.forEach(activity => this.selected.add(activity.id));
    } else {
      this.selected.clear();
    }
    this.shouldSelectAll = !this.shouldSelectAll
  }


  getXFromY(X: string, Y: Activity) { //Y should be an activity
    switch(X.toLowerCase())  {
      case 'id': return Y.id
      case 'name': return Y.name
      case 'date': return Y.startTime.toISOString().split("T")[0]
      case 'time': return this.toHoursMinsSecs(Y.totalTime)
      case 'route': return Y.route!=null ? Y.route.name : Y.route
      case 'distance': return Math.round(Y.totalDistance*100)/100 + "km"
      case 'other' : return Y.other
      default : throw new Error(`Property '${X}' does not exist on object Y.`)
    }
  }

  defaultSubmit(){
    if (this.selected.size === 0 ) { alert("You must select at least one activity") }
    else if (this.selected.size === 1) {
      this.router.navigate([`/me/activity-view/${Array.from(this.selected)[0]}`])
    } else if (this.selected.size === 2) {
      this.router.navigate([`/me/activity-comparison/${Array.from(this.selected)[0]}-${Array.from(this.selected)[1]}`])
    } else {
      alert("Sorry, for now you can select no more than 2 elements. We are working on expanding this feature.")
    }
  }

  toHoursMinsSecs(n: number){ //format should be H:MM:SS but this is fine for now
    // Activity.formatTime(n) does a similar thing, in a different format
    const hours = Math.floor(n/3600)
    n = n%3600
    const mins = Math.floor(n/60)
    const secs = n%60

    const hoursString = hours != 0 ? hours+"h ":""
    const minsString = mins != 0 ? mins + "m ":""
    const secsString = secs + "s "


    return `${hoursString}${minsString}${secsString}`
  }

}
