import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';

@Component({
  selector: 'app-activity',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './activity-select.component.html',
  styleUrl: './activity-select.component.css'
})
export class ActivitySelectComponent {
  selected: Set<number> = new Set();
  shouldSelectAll: boolean = true;

  columns: string[] = ['Name', 'Date', 'Route', 'Time', 'Distance'];

  constructor(private router: Router){}

  toggle(id: number) {
    if (this.selected.size===this.activities.length) {this.shouldSelectAll = true}

    this.selected.has(id) ? this.selected.delete(id):this.selected.add(id)

    if (this.selected.size===this.activities.length) {this.shouldSelectAll = false}
  }

  selectAll() {
    if (this.shouldSelectAll) {
      this.activities.forEach(activity => this.selected.add(activity.id));
    } else {
      this.selected.clear();
    }
    this.shouldSelectAll = !this.shouldSelectAll
  }


  getXFromY(X: string, Y: any) { //Y should be an activity
    switch(X.toLowerCase())  {
      case 'id': return Y.id
      case 'name': return Y.name
      case 'date': return Y.date
      case 'time': return Y.time
      case 'route': return Y.route
      case 'distance': return Y.distance
      case 'other' : return Y.other
      default : throw new Error(`Property '${X}' does not exist on object Y.`)
    }
  }

  submit(){
    if (this.selected.size === 0 ) { alert("You must select at least one activity") }
    else if (this.selected.size === 1) {
      this.router.navigate([`/me/activity-view/${Array.from(this.selected)[0]}`])
    } else if (this.selected.size === 2) {
      this.router.navigate([`/me/activity-comparison/${Array.from(this.selected)[0]}-${Array.from(this.selected)[1]}`])
    } else {
      alert("Sorry, for now you can select no more than 2 elements. We are working on expanding this feature.")
    }
  }

  activities = [
    {
      id:1,
      name: "Workout",
      route: "Usual",
      date: "now",
      time: "then",
      distance: "5km",
      other: "none"
    },
    {
      id:2,
      name: "Workout",
      route: "Usual",
      date: "now",
      time: "then",
      distance: "5km",
      other: "none"
    },
    {
      id:3,
      name: "Workout",
      route: "Usual",
      date: "now",
      time: "then",
      distance: "5km",
      other: "none"
    },
    {
      id:4,
      name: "Workout",
      route: "Usual",
      date: "now",
      time: "then",
      distance: "5km",
      other: "none"
    },
    {
      id:5,
      name: "Workout",
      route: "Usual",
      date: "now",
      time: "then",
      distance: "5km",
      other: "none"
    },
    {
      id:6,
      name: "Workout",
      route: "Usual",
      date: "now",
      time: "then",
      distance: "5km",
      other: "none"
    },
    {
      id:7,
      name: "Workout",
      route: "Usual",
      date: "now",
      time: "then",
      distance: "5km",
      other: "none"
    },
    {
      id:8,
      name: "Workout",
      route: "Usual",
      date: "now",
      time: "then",
      distance: "5km",
      other: "none"
    },
    {
      id:9,
      name: "Workout",
      route: "Usual",
      date: "now",
      time: "then",
      distance: "5km",
      other: "none"
    },
    {
      id:10,
      name: "Workout",
      route: "Usual",
      date: "now",
      time: "then",
      distance: "5km",
      other: "none"
    },
    {
      id:11,
      name: "Workout",
      route: "Usual",
      date: "now",
      time: "then",
      distance: "5km",
      other: "none"
    },
    {
      id:12,
      name: "Workout",
      route: "Usual",
      date: "now",
      time: "then",
      distance: "5km",
      other: "none"
    },
    {
      id:13,
      name: "Workout",
      route: "Usual",
      date: "now",
      time: "then",
      distance: "5km",
      other: "none"
    },
    {
      id:14,
      name: "Workout",
      route: "Usual",
      date: "now",
      time: "then",
      distance: "5km",
      other: "none"
    },
    {
      id:15,
      name: "Workout",
      route: "Usual",
      date: "now",
      time: "then",
      distance: "5km",
      other: "none"
    },
    {
      id:16,
      name: "Workout",
      route: "Usual",
      date: "now",
      time: "then",
      distance: "5km",
      other: "none"
    },
    {
      id:17,
      name: "Workout",
      route: "Usual",
      date: "now",
      time: "then",
      distance: "5km",
      other: "none"
    },
    {
      id:18,
      name: "Workout",
      route: "Usual",
      date: "now",
      time: "then",
      distance: "5km",
      other: "none"
    },
    {
      id:19,
      name: "Workout",
      route: "Usual",
      date: "now",
      time: "then",
      distance: "5km",
      other: "none"
    },
    {
      id:20,
      name: "Workout",
      route: "Usual",
      date: "now",
      time: "then",
      distance: "5km",
      other: "none"
    },
    {
      id:21,
      name: "Workout",
      route: "Usual",
      date: "now",
      time: "then",
      distance: "5km",
      other: "none"
    },
    {
      id:22,
      name: "Workout",
      route: "Usual",
      date: "now",
      time: "then",
      distance: "5km",
      other: "none"
    },
    {
      id:23,
      name: "Workout",
      route: "Usual",
      date: "now",
      time: "then",
      distance: "5km",
      other: "none"
    },
    {
      id:24,
      name: "Workout",
      route: "Usual",
      date: "now",
      time: "then",
      distance: "5km",
      other: "none"
    },
    {
      id:25,
      name: "Workout",
      route: "Usual",
      date: "now",
      time: "then",
      distance: "5km",
      other: "none"
    },
    {
      id:26,
      name: "Workout",
      route: "Usual",
      date: "now",
      time: "then",
      distance: "5km",
      other: "none"
    },
    {
      id:27,
      name: "Workout",
      route: "Usual",
      date: "now",
      time: "then",
      distance: "5km",
      other: "none"
    },
    {
      id:28,
      name: "Workout",
      route: "Usual",
      date: "now",
      time: "then",
      distance: "5km",
      other: "none"
    },
    {
      id:29,
      name: "Workout",
      route: "Usual",
      date: "now",
      time: "then",
      distance: "5km",
      other: "none"
    },
    {
      id:30,
      name: "Workout",
      route: "Usual",
      date: "now",
      time: "then",
      distance: "5km",
      other: "none"
    }
  ]
}
