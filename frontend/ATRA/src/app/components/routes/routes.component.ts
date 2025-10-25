import { AlertService } from './../../services/alert.service';
import { ActivityService } from './../../services/activity.service';
import { MapService } from './../../services/map.service';
import { CommonModule } from '@angular/common';
import { Component, TemplateRef } from '@angular/core';
import { RouteService } from '../../services/route.service';
import { Route } from '../../models/route.model';
import { Activity } from '../../models/activity.model';
import L from 'leaflet';
import { NgbModal, NgbModalOptions } from '@ng-bootstrap/ng-bootstrap';
import { ActivitySelectComponent } from "../activity-select/activity-select.component";
import { FormattingService } from '../../services/formatting.service';
import { ActivatedRoute, Router } from '@angular/router';
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
  formatDistance = FormattingService.formatDistance

  loading:boolean = false;
  columns: string[] = ['Name', 'Desc', 'Distance', 'Ele'];
  activityColumns: string[] = ['id', 'Dist', 'time'];
  map !: L.Map | null;
  polyline !: L.Polyline;

  allActivities !: Activity[];
  errorLoadingActivities : boolean = false;

  constructor(private routeService:RouteService,
    private activityService:ActivityService,
    private modalService: NgbModal,
    private alertService:AlertService,
    private activatedRoute: ActivatedRoute,
    private router:Router
  ){}

  mural: undefined | number = undefined;

  ngOnInit(): void {
    this.loading=true;
    if (this.activatedRoute.snapshot.url[0].toString()=="murals") {
      this.mural = this.activatedRoute.snapshot.params['id'];
      this.routeService.mural = this.mural;
    }
    this.routeService.getRoutes().subscribe({
      next: (value:Route[]) => {
        this.loading=false;
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
      error: (err) => {this.loading=false;this.alertService.toastError("Try reloading the page", "Error fetching routes"); console.log("There was an error fetching the Routes", err)}
    })

    this.fetchActivitiesWithNoRoute()
  }
  fetchActivitiesWithNoRoute(){
    this.errorLoadingActivities = false
    this.activityService.getWithNoRoute().subscribe({
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
      case 'desc': return Y.description.length>25 ? (Y.description.substring(0,26) + "..."):Y.description

      case 'ele': return Number(Y.elevationGain.toFixed(2)) + "m"
      case 'distance': return FormattingService.formatDistance(Y.totalDistance)
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
      case 'dist': return FormattingService.formatDistance(v.summary.totalDistance)
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
        next: ([route, activities]: [Route, Activity[]]) => {
          console.log("(RoutesComponent) Updated route fetched successfully");
          route.activities = activities
          this.allRoutes.set(route.id, route)
          this.allRoutes = new Map(this.allRoutes.entries()) //to trigger change detection. should be careful with that
          this.shownRoutes = new Map(Array.from(this.allRoutes.entries()).filter(x=> this.visibilitiesToDisplay.includes(x[1].visibility.type)))
          this.select(route.id)
          this.fetchActivitiesWithNoRoute()
        },
        error: () => this.alertService.toastError("There was an error fetching the updated route. Data might be out of date until you reload the page")
    })
  }

  deleteSelectedRoute() {
    this.alertService.confirm("This action is irreversible, are you sure you want to continue?", "Deleting route").subscribe(
      (accept)=> {
        if (this.selectedRoute==null) throw new Error("Trying to delete null route")
        if (accept) this.routeService.deleteRouteAndRefetch(this.selectedRoute.id, this.mural).subscribe({
          next: (reply: Route[]) => {
            this.allRoutes = new Map(reply.map(x=>[x.id, x])) //to trigger change detection. should be careful with that
            this.shownRoutes = new Map(Array.from(this.allRoutes.entries()).filter(x=> this.visibilitiesToDisplay.includes(x[1].visibility.type)))
            this.select(null)
            this.fetchActivitiesWithNoRoute()
          },
          error: () => this.alertService.toastError("There was an error fetching the updated route. Data might be out of date until you reload the page")
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
      this.routeService.changeVisibility(this.selectedRoute.id, this.currentVis, this.allowedMuralsList).subscribe({
        next: ([route, activities]: [Route, Activity[]]) => {
          console.log("(RoutesComponent) Updated route fetched successfully");
          route.activities = activities
          this.allRoutes.set(route.id, route)
          this.allRoutes = new Map(this.allRoutes.entries()) //to trigger change detection. should be careful with that
          this.shownRoutes = new Map(Array.from(this.allRoutes.entries()).filter(x=> this.visibilitiesToDisplay.includes(x[1].visibility.type)))
          this.select(route.id)
        },
        error: (e) => {
          if (e.message==="error changing visibility") this.currentVis = this.selectedRoute?.visibility.type ?? this.currentVis
          else this.alertService.toastError("There was an error fetching the updated route. Data might be out of date until you reload the page")

        }
      })
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
    this.routeService.addActivitiesToRouteAndRefetch(activities, this.selectedRoute.id, this.modal).subscribe({
      next: ([route, activities]: [Route, Activity[]]) => {
        route.activities = activities
        this.allRoutes.set(route.id, route)
        this.allRoutes = new Map(this.allRoutes.entries()) //to trigger change detection. should be careful with that
        this.shownRoutes = new Map(Array.from(this.allRoutes.entries()).filter(x=> this.visibilitiesToDisplay.includes(x[1].visibility.type)))
        this.select(route.id)

        this.fetchActivitiesWithNoRoute()
      },
      error: () => this.alertService.toastError("There was an error fetching the updated route. Data might be out of date until you reload the page")
    })
  }

  //#endregion

  actsToCompare:Set<number>=new Set();
  openCompareModal(template:TemplateRef<any>,enableBackdrop:boolean) {
    this.selectedRoute!.activities.forEach(a=>this.actsToCompare.add(a.id))
    this.open(template,enableBackdrop)
  }
  toggleCompare(id:number){
    if (this.actsToCompare.has(id)) this.actsToCompare.delete(id)
    else this.actsToCompare.add(id)
  }
  compareSelected(modal:any) {
    const urlParts = this.activatedRoute.snapshot.url
    const urlStart = urlParts[0].toString()
    if  (urlStart=='me') this.router.navigate([urlStart, 'activities', 'compare', Array.from(this.actsToCompare).join("-")])
    else this.router.navigate([urlStart, urlParts[1].toString(), 'activities', 'compare', Array.from(this.actsToCompare).join("-")])
    modal.dismiss()
  }

}
