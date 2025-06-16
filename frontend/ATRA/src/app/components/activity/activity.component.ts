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
  styleUrl: './activity.component.css'
})
export class ActivityComponent implements OnInit {


  @ViewChild('chartContainer', { static: true }) chartContainer!: ElementRef;

  id !: number;
  activity !: Activity;
  stats !: {name:string, value:string}[];
  map!: L.Map;
  path !: L.Polyline;

  constructor(private route: ActivatedRoute, private router:Router, private activityService: ActivityService, private modalService: NgbModal, private routeService: RouteService, private alertService:AlertService, private authService:AuthService) {}

  ngOnInit(): void {
    if (this.map==null)
      this.map = MapService.mapSetup("map")

    const stringId = this.route.snapshot.paramMap.get("id");
    if (stringId===null) {
      this.router.navigate(["/error?code=400&reason=missingParameter"]);
      return;
    }
    this.id = parseInt(stringId);

    this.activityService.get(this.id).subscribe({
      next: (act) => this.receivedAnActivityHandler(act),
      error: () => {this.alertService.alert("There was an error fetching the activity. Try reloading the page.")}
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
    this.activityService.delete(this.id).subscribe({
      next: () => {
        this.alertService.alert("Route deleted")
        this.router.navigate(["/me/activities"])
      },
      error: (e) => {this.alertService.alert("ERROR: "+e.error)},
    });
  }

  //#region route shenanigans
  modal!: any;
  routes : Route[] = [];
  selectedRoute : number =-1;
  errorLoadingRoutes : boolean = false;
  routeMap: L.Map | null = null;
  routePath !: L.Polyline;
  displayRoute : Route | null = null;
  owned: boolean = false;

  open(content: TemplateRef<any>) {
    if (this.errorLoadingRoutes) return this.alertService.alert("There seem to be no activities with no route assigned.")
    this.modal = this.modalService.open(content)

    this.selectedRoute = this.activity.route!=null ? this.activity.route.id:-1
    this.routeMap = null;
    this.selectedRouteChange();
  }

  createRoute(name:string, desc:string, distance:string, elevation:string) {
    this.routeService.createRoute(name,desc, parseFloat(distance), parseFloat(elevation), this.activity.id).subscribe({
      next: () => {
        this.modalService.dismissAll()
        this.alertService.confirm("The route has been created properly. Do you want to be redirected to the routes page?").subscribe({
          next:(accepted)=>{
            if (accepted) {
              this.router.navigate(["/me/routes/"])
            } else {
              this.fetchRoutes()
              this.activityService.get(this.id).subscribe({
                next: (act) => this.receivedAnActivityHandler(act),
                error: () => {this.alertService.alert("There was an error fetching the updated activity. Try reloading the page.")}
              })
            }
          }
        })

      },
      error: (e) => {
        console.log(e);
      }
    });
  }

  fetchRoutes() {
    this.routeService.fetchAllRoutes().subscribe({
      next: (receivedRoutes: Route[]) => {
        this.routes = receivedRoutes;
      },
      error: (e) => {
        console.log(e);
        this.alertService.alert("There was an error fetching the routes. Try again later or after refreshing the page.")
      }
    })
  }

  submitChangeRoute(routeId: string){
    this.modal.dismiss();
    if (routeId=="-1") {
      this.activityService.removeRoute(this.id).subscribe({
        next: (activity:any) => this.receivedAnActivityHandler(activity, "success at deleting existing connection"),
        error: () => {
          console.log("fail");
          this.alertService.alert("Couldn't remove the route from the activity. Try again later, or after reloading.")
        }
      })
      return
    }
    this.activityService.addRoute(this.id, parseInt(routeId)).subscribe({
      next: (activity:any) => this.receivedAnActivityHandler(activity, "success at creating new connection"),
      error: () => {
        console.log("fail");
        this.alertService.alert("Couldn't add the route to the activity. Try again later, or after reloading.")
      }
    })
    return
  }

  selectedRouteChange(){
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

  receivedAnActivityHandler(act:Activity |null, msg?:string) {
    if (act==null) return;
    if (msg) console.log(msg);
    this.owned = this.authService.user.getValue()?.id==act.user.id;

    this.activity = act; //this.activityService.process1(act); this is now being done in the service
    this.stats = this.activity.getOverview()
    this.selectedRoute = this.activity.route!=null ? this.activity.route.id:-1;
    this.currentVisibility = this.activity.visibility

    this.addPathToMap()
  }

  currentVisibility: "PRIVATE" | "MURAL_SPECIFIC" | "MURAL_PUBLIC" | "PUBLIC" = "PRIVATE";
  visibilities: ("PRIVATE" | "MURAL_SPECIFIC" | "MURAL_PUBLIC" | "PUBLIC")[] = ["PRIVATE", "MURAL_SPECIFIC", "MURAL_PUBLIC", "PUBLIC"];
  allowedMuralsList: number[] = [];

  addAllowedMural(muralIdString: string) {
    const muralId = parseInt(muralIdString);
    if (isNaN(muralId)) {
      this.alertService.alert("Invalid mural ID. Please enter a valid number.");
      return;
    }
    if (this.allowedMuralsList.includes(muralId)) {
      return;
    }
    this.allowedMuralsList.push(muralId);
  }

  submitChangeVisibility(newVis: string) {
    if (!["PRIVATE", "MURAL_SPECIFIC", "MURAL_PUBLIC", "PUBLIC"].includes(newVis)) throw new Error("WHAT. THE. FUCK. How the fuck is newVis this value: " + newVis);
    //show loading screen
    if ("MURAL_SPECIFIC" !== newVis) this.allowedMuralsList = [];
    this.activityService.changeVisibility(this.id, newVis as "PRIVATE" | "MURAL_SPECIFIC" | "MURAL_PUBLIC" | "PUBLIC", this.allowedMuralsList).subscribe({
      next:() => {
        console.log("Visibility changed");
        //close loading screen
        this.modal.close()

      }
    })

  }
}
