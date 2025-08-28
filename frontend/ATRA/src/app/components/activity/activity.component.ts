import { AuthService } from './../../services/auth.service';
import { MapService } from './../../services/map.service';
import { ActivityService } from './../../services/activity.service';
import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Activity } from '../../models/activity.model';
import { FormsModule } from '@angular/forms';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { ActivityStudyComponent } from "../activity-study/activity-study.component";
import L from 'leaflet';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { RouteService } from '../../services/route.service';
import { Route } from '../../models/route.model';
import { AlertService } from '../../services/alert.service';


@Component({
  selector: 'app-activity',
  standalone: true,
  imports: [CommonModule, FormsModule, NgxChartsModule, ActivityStudyComponent],
  templateUrl: './activity.component.html',
  styleUrl: './activity.component.scss'
})
export class ActivityComponent implements OnInit {


  @ViewChild('chartContainer', { static: true }) chartContainer!: ElementRef;

  id !: number;
  muralId: string | null = null; // this is used to fetch the activity from a mural, if it is not null
  activity !: Activity;
  stats !: {name:string, value:string}[];
  map!: L.Map;
  path !: L.Polyline;

  constructor(private route: ActivatedRoute, private router:Router, private activityService: ActivityService, private modalService: NgbModal, private routeService: RouteService, private alertService:AlertService, private authService:AuthService) {}

  ngOnInit(): void {
    console.log("(ActivityComponent) ngOnInit");
    this.alertService.loading()

    if (this.map==null) this.map = MapService.mapSetup("map")

      console.log(this.route.snapshot.url[0].toString());

    this.muralId = this.route.snapshot.paramMap.get("muralId");
    const stringId = this.route.snapshot.paramMap.get("activityId");
    console.log("(ActivityComponent) stringId: "+stringId);

    if (stringId===null) {
      this.router.navigate(["/error?code=400&reason=missingParameter"]);
      return;
    }
    this.id = parseInt(stringId);

    this.activityService.get(this.id, this.muralId).subscribe({
      next: (act) => {
        this.receivedAnActivityHandler(act)
        if (act!=null) this.alertService.loaded();
      },
      error: (e) => {this.alertService.loaded();this.alertService.alert("There was an error fetching the activity. Try reloading the page.")}
    })
    this.fetchRoutes()
  }


  addPathToMap() {
    if (this.path!=null) this.path.remove()
    const coordinates = this.activityService.getCoordinates(this.activity)
    this.path = L.polyline(coordinates, {
      color: 'blue',         // Line color
      weight: 4,             // Line thickness
      opacity: 0.8,          // Line opacity
    }).addTo(this.map);

    this.map.fitBounds(this.path.getBounds());
  }
  deleteActivity() {
    this.alertService.confirm("This action is irreversible, are you sure you want to continue?", "Deleting activity").subscribe((accepted)=>{
      if (accepted) {
        this.activityService.delete(this.id).subscribe({
          next: () => {
            this.alertService.toastSuccess("Route deleted")
            this.router.navigate(["/me/activities"])
          },
          error: (e) => {this.alertService.toastError("There was an error deleting the activity"); console.error("Error deleting activity with id " + this.id + ": "+e.error);}
        });
      }
    })

  }

  //#region route shenanigans
  modal!: any;
  routes : Route[] = [];
  selectedRoute : number =-1;
  routeMap: L.Map | null = null;
  routePath !: L.Polyline;
  displayRoute : Route | null = null;
  owned: boolean = false;

  open(content: TemplateRef<any>) {
    this.modal = this.modalService.open(content, { centered:true })
    this.selectedRoute = this.activity.route!=null ? this.activity.route.id:-1
    this.routeMap = null;
    this.selectedRouteChange();
  }

  createRoute(name:string, desc:string, distance:string, elevation:string) {
    this.alertService.loading();
    this.routeService.createRoute(name,desc, parseFloat(distance), parseFloat(elevation), this.activity.id).subscribe({
      next: (r:Route) => {
        this.alertService.loaded()
        this.modalService.dismissAll()
        this.alertService.confirm("The route has been created properly. Do you want to be redirected to the routes page?", "Route created").subscribe({
          next:(accepted)=>{
            if (accepted) {
              this.router.navigate(["/me/routes"], { queryParams: { selected:r.id }})
            } else {
              // why not just this.location.reload() ? It pretty much does the same thing methinks (reloads the whole page)
              this.fetchRoutes()
              this.activityService.get(this.id, this.muralId).subscribe({
                next: (act) => this.receivedAnActivityHandler(act),
                error: () => {this.alertService.toastError("There was an error fetching the updated activity. Try reloading the page.", "Activity may not reflect latest changes")}
              })
            }
          }
        })

      },
      error: (e) => {
        this.alertService.loaded()
        console.log(e);
      }
    });
  }

  fetchRoutes() {
    this.routeService.getRoutesNoActivities().subscribe({
      next: (receivedRoutes: Route[]) => {
        this.routes = receivedRoutes;
      },
      error: (e) => {
        console.log(e);
        this.alertService.toastError("You may be unable to change the activity's route. Try refreshing the page.", "Error fetching routes")
      }
    })
  }

  submitChangeRoute(routeId: string){
    this.modal.dismiss();
    this.alertService.loading();
    if (routeId=="-1") {
      this.activityService.removeRoute(this.id).subscribe({
        next: (activity:Activity) => {this.alertService.loaded();this.receivedAnActivityHandler(activity, "success at deleting existing connection")},
        error: () => {
          console.error("Error removing the route from the activity");
          this.alertService.loaded()
          this.alertService.toastError("Couldn't remove the route from the activity.")
        }
      })
      return
    }
    this.activityService.addRoute(this.id, parseInt(routeId)).subscribe({
      next: (activity:Activity) => {this.alertService.loaded();this.receivedAnActivityHandler(activity, "success at creating new connection")},
      error: () => {
        console.error("Error changing the avtivity's route");
        this.alertService.loaded()
        this.alertService.toastError("Couldn't add the route to the activity.")
      }
    })
    return
  }

  selectedRouteChange(){
    //all of this is used for the change route functionality
    if (this.selectedRoute==-1) return this.routeMap = null
    if (this.routeMap==null || this.routeMap == undefined) {
      setTimeout(() => {
        if (this.routeMap==null) {
          console.log("creating map");
          this.routeMap = MapService.mapSetup("routeMap")
        }
        this.updateMap()
        return
      })
      return
    }
    this.updateMap()
    return
  }
  updateMap() {
    //all of this is used for the change route functionality
    this.displayRoute = null;
    for (var r of this.routes) {
      console.log("routeId: "+r.id);

      if (r.id==this.selectedRoute) {
        this.displayRoute = r
        break
      }
    }

    if (this.displayRoute==null) throw new Error(`selectedRoute ${this.selectedRoute} does not match any route Ids within routes`)
    if (this.routeMap==null) throw new Error("routeMap was somehow null")
    this.routePath = MapService.addPolyline(this.displayRoute.coordinates,this.routeMap, this.routePath)
    return
  }

  //#endregion

  receivedAnActivityHandler(act:Activity | null, msg?:string) {
    if (act==null) return;
    if (msg) console.log(msg);
    this.owned = this.authService.user.getValue()?.id==act.user.id;

    this.activity = new Activity(act); //equivalent to activityService.proces1(). Neccessary to use activity methods
    this.stats = this.activity.getOverview()
    this.selectedRoute = this.activity.route!=null ? this.activity.route.id:-1;
    this.currentVisibility = this.activity.visibility
    this.allowedMuralsList = this.activity.allowedMurals || [];

    this.addPathToMap()
  }

  currentVisibility: "PRIVATE" | "MURAL_SPECIFIC" | "MURAL_PUBLIC" | "PUBLIC" = "PRIVATE";
  visibilities: ("PRIVATE" | "MURAL_SPECIFIC" | "MURAL_PUBLIC" | "PUBLIC")[] = ["PRIVATE", "MURAL_SPECIFIC", "MURAL_PUBLIC", "PUBLIC"];
  allowedMuralsList: number[] = [];

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

  submitChangeVisibility(newVis: string) {
    if (!["PRIVATE", "MURAL_SPECIFIC", "MURAL_PUBLIC", "PUBLIC"].includes(newVis)) throw new Error("WHAT. THE. FUCK. How the fuck is newVis this value: " + newVis);
    this.alertService.loading();
    if ("MURAL_SPECIFIC" !== newVis) this.allowedMuralsList = [];
    this.activityService.changeVisibility(this.id, newVis as "PRIVATE" | "MURAL_SPECIFIC" | "MURAL_PUBLIC" | "PUBLIC", this.allowedMuralsList).subscribe({
      next:() => {
        console.log("Visibility changed");
        this.alertService.loaded();
        this.modal.close()

      }
    })

  }
}
