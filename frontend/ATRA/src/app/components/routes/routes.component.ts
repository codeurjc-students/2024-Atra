import { AlertService } from './../../services/alert.service';
import { ActivityService } from './../../services/activity.service';
import { MapService } from './../../services/map.service';
import { CommonModule } from '@angular/common';
import { Component, TemplateRef } from '@angular/core';
import { RouteService } from '../../services/route.service';
import { Route } from '../../models/route.model';
import { Activity } from '../../models/activity.model';
import L from 'leaflet';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ActivitySelectComponent } from "../activity-select/activity-select.component";

@Component({
  selector: 'app-routes',
  standalone: true,
  imports: [CommonModule, ActivitySelectComponent],
  templateUrl: './routes.component.html',
  styleUrl: './routes.component.css'
})
export class RoutesComponent {

  routes !: Map<number, Route>;
  selectedRoute !: Route|null;
  columns: string[] = ['Name', 'Desc', 'Distance', 'Ele'];
  activityColumns: string[] = ['id', 'Dist', 'time'];
  map !: L.Map | null;
  polyline !: L.Polyline;

  allActivities !: Activity[];
  errorLoadingActivities : boolean = false;

  constructor(private routeService:RouteService, private activityService:ActivityService, private modalService: NgbModal, private alertService:AlertService){}


  ngOnInit(): void {
    this.routeService.getRoutes().subscribe({
      next: (value:Route[]) => {
        if (value.length!=0) {
          this.routes = new Map(value.map(x => [x.id, x]))
        }
        else {
          this.alertService.alert("No routes were found. You can create one by clicking on create route.")
          this.routes = new Map()
        }
      },
      error: (err) => {this.alertService.alert("There was an error fetching the Routes"); console.log("There was an error fetching the Routes", err)}
    })

    this.fetchActivitiesWithNoRoute()
  }
  fetchActivitiesWithNoRoute(){
    this.errorLoadingActivities = false
    this.activityService.getAll("routeIsNull").subscribe({
      next:(activities:Activity[]) => {
        if (activities.length==0) this.errorLoadingActivities=true
        this.allActivities = activities
      }
    })
  }

  getXFromY(X: string, Y: Route) { //Y should be an activity
    switch(X.toLowerCase())  {
      case 'id': return Y.id
      case 'name': return Y.name
      case 'desc': return Y.description
      case 'ele': return Y.elevationGain
      case 'distance': return Y.totalDistance.toFixed(2) + "km"
      default : throw new Error(`Property '${X}' does not exist on object ${typeof Y}}.`)
    }
  }
  getXFromYAct(attr:string, v:Activity) {
    //this is called with selectedActivity.activities[n]. To avoid trouble in serialization, those are ids.
    // have to decide if front should recive all of a route's activities as a DTO or as ids
    // I'm leaning toward DTO
    if (v.summary==null) {
      throw new Error(`Activity ${v.id} has no summary, so it cannot be displayed.`)
    }
    switch(attr.toLowerCase())  {
      case 'id': return v.id
      case 'name': return v.name
      case 'time': return Activity.formatTime(v.summary.totalTime)
      case 'ele': return v.summary.elevationGain
      case 'dist': return v.summary.totalDistance.toFixed(2) + "km"
      default : throw new Error(`Property '${attr}' does not exist on object ${typeof v}.`)
    }
  }

  select(selectedId: number|null) {
    if (selectedId==null) {
      this.selectedRoute = null
      this.map = null;
      return
    }
    const r = this.routes.get(selectedId)
    if (r==null) {
      throw new Error(`The id requested matches no route; ${selectedId} is not a valid key for map ${this.routes}`)
    }
    this.selectedRoute = r

    setTimeout(() => {
      if (this.map==null) {
        console.log("creating map");

        this.map = MapService.mapSetup("map")
      }
      this.updateMap()
    })
  }

  updateMap() {
    if (this.map==null) {throw new Error("updateMap called with map==null")}
    if (this.selectedRoute==null) {throw new Error("updateMap called with selectedRoute==null")}
    this.polyline = MapService.addPolyline(this.selectedRoute.coordinates, this.map, this.polyline)
  }

  removeActivity(id: number) {
    if (!this.selectedRoute) throw new Error("Cannot remove activity as there's no selected route")
    this.routeService.removeActivity(this.selectedRoute.id, id).subscribe({
      next: (reply: Route) => {
        console.log("success at deleting connection");
        this.routes.set(reply.id, reply)
        this.routes = new Map(this.routes.entries()) //to trigger change detection. should be careful with that
        this.select(reply.id)
        this.fetchActivitiesWithNoRoute()

      },
      error: () => {
        console.log("fail");
        this.alertService.alert("Couldn't remove the activity. Try again later, or after reloading.")
      }
    })
  }

  deleteSelectedRoute() {
    if (this.selectedRoute==null) throw new Error("Trying to delete null route")
    this.routeService.deleteRoute(this.selectedRoute.id).subscribe({
      next: (reply: Route[]) => {
        console.log("success at deleting connection");
        if (this.selectedRoute==null) throw new Error("Trying to delete null route")
        this.routes.delete(this.selectedRoute.id)
        this.routes = new Map(reply.map(x=>[x.id, x])) //to trigger change detection. should be careful with that
        this.select(null)
        this.fetchActivitiesWithNoRoute()

      },
      error: () => {
        console.log("fail");
        this.alertService.alert("Couldn't remove the activity. Try again later, or after reloading.")
      }
    })
  }


  //#region modal crap

  modal!: any;
  modalColumns: string[] = ['Name', 'Date', 'Route', 'Time', 'Distance'];
  modalSelected: Set<number> = new Set();

  open(content: TemplateRef<any>) {
    if (this.errorLoadingActivities) return this.alertService.alert("There seem to be no activities with no route assigned.")
    this.modal = this.modalService.open(content, {size:'lg'})
  }
  submit(){}
  addActivitiesToRoute(activities: Set<number>){
    if (this.selectedRoute==null)  throw new Error("addActivitiesToRoute called with undefined route")
    this.routeService.addActivitiesToRoute(activities, this.selectedRoute.id).subscribe({
      next: (reply: Route) => {
        console.log("success at creating new connections");
        this.modal.dismiss();
        this.routes.set(reply.id, reply)
        this.routes = new Map(this.routes.entries()) //to trigger change detection. should be careful with that
        this.select(reply.id)

        this.fetchActivitiesWithNoRoute()

      },
      error: () => {
        console.log("fail");
        this.alertService.alert("Couldn't add the activities to the route. Try again later, or after reloading.")
      }
    })
  }

  //#endregion

}
