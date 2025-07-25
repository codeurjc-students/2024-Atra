import { AlertService } from './../../services/alert.service';
import { ActivityService } from './../../services/activity.service';
import { MapService } from './../../services/map.service';
import { CommonModule } from '@angular/common';
import { Component, ComponentFactory, TemplateRef } from '@angular/core';
import { RouteService } from '../../services/route.service';
import { Route } from '../../models/route.model';
import { Activity } from '../../models/activity.model';
import L from 'leaflet';
import { NgbModal, NgbModalOptions } from '@ng-bootstrap/ng-bootstrap';
import { ActivitySelectComponent } from "../activity-select/activity-select.component";
import { FormattingService } from '../../services/formatting.service';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-routes',
  standalone: true,
  imports: [CommonModule, ActivitySelectComponent, FormsModule],
  templateUrl: './routes.component.html',
  styleUrl: './routes.component.scss'
})
export class RoutesComponent {


  visibilitiesToDisplay: string[] = ["PRIVATE", "MURAL_SPECIFIC", "PUBLIC"];

  onCheckboxChange(event: Event) {
    const checkbox = event.target as HTMLInputElement;
    if (checkbox.checked) {
      this.visibilitiesToDisplay.push(checkbox.value);
    } else {
      this.visibilitiesToDisplay = this.visibilitiesToDisplay.filter(v => v !== checkbox.value);
    }
    //cambiar las que se estén mostrando
    this.shownRoutes = new Map(Array.from(this.allRoutes.entries()).filter(x => this.visibilitiesToDisplay.includes(x[1].visibility.type)))
  }

  allRoutes !: Map<number, Route>;
  shownRoutes !: Map<number, Route>;
  selectedRoute !: Route|null;
  currentVis : "PUBLIC" | "MURAL_SPECIFIC" | "PRIVATE" | null = null;
  allowedMuralsList: number[] = [];

  columns: string[] = ['Name', 'Desc', 'Distance', 'Ele'];
  activityColumns: string[] = ['id', 'Dist', 'time'];
  map !: L.Map | null;
  polyline !: L.Polyline;

  allActivities !: Activity[];
  errorLoadingActivities : boolean = false;

  constructor(private routeService:RouteService, private activityService:ActivityService, private modalService: NgbModal, private alertService:AlertService, private activatedRoute: ActivatedRoute){}


  ngOnInit(): void {
    //the component itself should show a spinner. Add that in next commit. alertService.loading() is for when the whole page is loading, to stop the user from doing things. Here, just a part is loading, so just that part should show that
    var mural: undefined | number = undefined;
    if (this.activatedRoute.snapshot.url[0].toString()=="murals") {
      mural = this.activatedRoute.snapshot.params['id'];
    }
    this.routeService.getRoutes(mural).subscribe({
      next: (value:Route[]) => {
        if (value.length!=0) {
          this.allRoutes = new Map(value.map(x => [x.id, x]))
          this.shownRoutes = new Map(Array.from(this.allRoutes.entries()).filter(x=> this.visibilitiesToDisplay.includes(x[1].visibility.type)))
          console.log("routes", this.allRoutes);
          console.log("shownRoutes", this.shownRoutes);


          this.activatedRoute.queryParamMap.subscribe(params => {
            const selectedId = params.get('selected');
            this.select(selectedId==null ? null:Number(selectedId)); //bc apparently Number(null) is 0
          })
        }
        else {
          this.alertService.toastInfo("No routes were found. You can create one by clicking on create route.") //fix: instead of this, the component itself should show a message when there are no routes methinks
          this.allRoutes = new Map()
        }
      },
      error: (err) => {this.alertService.toastError("Try reloading the page", "Error fetching routes"); console.log("There was an error fetching the Routes", err)}
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

  getXFromY(X: string, Y: Route) { //Y should be a Route
    switch(X.toLowerCase())  {
      case 'id': return Y.id
      case 'name': return Y.name
      case 'desc': return Y.description.substring(0,26) + "..."
      case 'ele': return Number(Y.elevationGain.toFixed(2)) + "m"
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
      case 'time': return FormattingService.formatTime(v.summary.totalTime)
      case 'ele': return v.summary.elevationGain
      case 'dist': return v.summary.totalDistance.toFixed(2) + "km"
      default : throw new Error(`Property '${attr}' does not exist on object ${typeof v}.`)
    }
  }

  select(selectedId: number|null) {
    if (selectedId==null) {
      this.selectedRoute = null
      this.currentVis=null
      this.map = null;
      this.allowedMuralsList = []
      return
    }
    const r = this.allRoutes.get(selectedId)
    if (r==null) {
      throw new Error(`The id requested matches no route; ${selectedId} is not a valid key for map ${this.allRoutes}`)
    }
    this.selectedRoute = r
    this.currentVis = r.visibility.type
    this.allowedMuralsList = r.visibility.allowedMurals

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
        this.allRoutes.set(reply.id, reply)
        this.allRoutes = new Map(this.allRoutes.entries()) //to trigger change detection. should be careful with that
        this.select(reply.id)
        this.fetchActivitiesWithNoRoute()

      },
      error: () => {
        console.log("fail");
        this.alertService.toastError("Couldn't remove the activity. Try again later, or after reloading.")
      }
    })
  }

  deleteSelectedRoute() {
    this.alertService.confirm("This action is irreversible, are you sure you want to continue?", "Deleting route").subscribe(
      (accept)=> {
        if (this.selectedRoute==null) throw new Error("Trying to delete null route")
        if (accept) this.routeService.deleteRoute(this.selectedRoute.id).subscribe({
          next: (reply: Route[]) => {
            console.log("success at deleting connection");
            if (this.selectedRoute==null) throw new Error("Trying to delete null route")
            //this.routes.delete(this.selectedRoute.id)
            this.allRoutes = new Map(reply.map(x=>[x.id, x])) //to trigger change detection. should be careful with that
            this.select(null)
            this.fetchActivitiesWithNoRoute()

          },
          error: () => {
            console.log("fail");
            this.alertService.toastError("Couldn't remove the route. Try again later, or after reloading.")
          }
    })})
  }


  //#region modal crap

  modal!: any;
  modalColumns: string[] = ['Name', 'Date', 'Route', 'Time', 'Distance'];
  modalSelected: Set<number> = new Set();

  open(content: TemplateRef<any>, openSmall: boolean = false, enableBackdrop:boolean = true) {
    console.log(this.errorLoadingActivities);

    if (this.errorLoadingActivities) return this.alertService.toastInfo("There seem to be no activities with no route assigned.")
    var options: NgbModalOptions = {centered:true}
    options.size = openSmall ? undefined:'lg'
    options.backdrop = enableBackdrop ? true : 'static'
    this.modal = this.modalService.open(content, options)
  }

  confirmChangeVis() {
    this.alertService.confirm("You are making this route public. This will allow all users and murals to see it and add activities to it.\nThis action is irreversible, and it revokes your ownership of this route. You will not be able to edit or delete it.\nAre you sure you want to continue?",
      "This action is irreversible").subscribe((shouldContinue:boolean)=>{if (shouldContinue) this.changeVis()})
  }

  changeVis() {
    if (this.selectedRoute==null || this.currentVis==null) throw new Error("changeVis called with null selectedRoute or currentVis")
    //if (this.currentVis!=this.selectedRoute?.visibility.type) {
      //gotta change visibility, so ask backend
      this.routeService.changeVisibility(this.selectedRoute.id, this.currentVis, this.allowedMuralsList).subscribe({
        next: (reply: Route) => {
          this.alertService.toastSuccess("Visibility changed successfully")
          console.log("(RoutesComponent) Visibility changed successfully");
          this.allRoutes.set(reply.id, reply)
          this.allRoutes = new Map(this.allRoutes.entries()) //to trigger change detection. should be careful with that
          this.select(reply.id)
        },
        error: (e) => {
          if (e.status==422) {
            this.alertService.toastError(e.error.message)
          } else {
            this.alertService.toastError("There was an error changing visibility.")
          }
        }
    })
    //} else {
    //  this.alertService.toastInfo("Visibility unchanged")
    //}
    this.modal.dismiss();
  }

    addAllowedMural(muralIdString: string) {
    const muralId = parseInt(muralIdString);
    if (isNaN(muralId)) {
      this.alertService.toastError("Invalid mural ID. Please enter a valid number.");
      return;
    }
    if (this.allowedMuralsList.includes(muralId)) {
      return;
    }
    this.allowedMuralsList.push(muralId);
  }

  submit(){}
  addActivitiesToRoute(activities: Set<number>){
    if (this.selectedRoute==null)  throw new Error("addActivitiesToRoute called with undefined route")
    this.routeService.addActivitiesToRoute(activities, this.selectedRoute.id).subscribe({
      next: (reply: Route) => {
        console.log("success at creating new connections");
        this.modal.dismiss();
        this.allRoutes.set(reply.id, reply)
        this.allRoutes = new Map(this.allRoutes.entries()) //to trigger change detection. should be careful with that
        this.select(reply.id)

        this.fetchActivitiesWithNoRoute()

      },
      error: (e) => {
        console.error("Error adding activities to route: " + e);
        this.alertService.toastError("Try again later.", "Error adding activities to route")
      }
    })
  }

  //#endregion

}
