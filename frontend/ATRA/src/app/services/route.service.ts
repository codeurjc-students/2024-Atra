
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, EMPTY, forkJoin, map, Observable, switchMap, throwError } from 'rxjs';
import { Route } from '../models/route.model';
import { ActivityService } from './activity.service';
import { Activity } from '../models/activity.model';
import { AlertService } from './alert.service';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';


@Injectable({
  providedIn: 'root'
})
export class RouteService {
  makeRoutesNotVisibleToMural(id: number, selectedRoutes: Set<number> | null) {
    if (selectedRoutes==null) throw new Error("(RouteService) makeRoutessNotVisibleToMural called with null selectedRoutes");
    return this.http.patch("/api/routes/visibility/mural?id="+id, Array.from(selectedRoutes)/*,   { headers: { 'Content-Type': 'application/json' } }*/)
  }
  changeVisibility(id: number, currentVis: "PUBLIC" | "MURAL_SPECIFIC" | "PRIVATE", allowedMuralsList: number[] = []): Observable<[Route, Activity[]]> {
    //the whole pipe could be moved to the caller, but it makes sense to me to have it here
    return this.http.patch<Route>("/api/routes/"+id+"/visibility", {"visibility": currentVis, "allowedMuralsList": JSON.stringify(allowedMuralsList)}).pipe(
        catchError((e) => {
          if (e.status==422) this.alertService.toastError(e.error.message)
          else this.alertService.toastError("There was an error changing visibility.")
          return throwError(() => new Error("error changing visibility",e));
        }),
        switchMap(() => {
          this.alertService.toastSuccess("Visibility changed successfully")
          console.log("(RoutesComponent) Visibility changed successfully. Fetching updated route");
          return forkJoin([this.getRoute(id), this.getRouteActivities(id)])
        })
      )
  }

  getRoutes(): Observable<Route[]> {
    if (this.mural==undefined)
      return this.http.get<Route[]>("/api/routes");
    else
      return this.http.get<Route[]>("/api/routes?from=mural&id="+this.mural);

  }

  mural:number|undefined;
  constructor(private http: HttpClient, private activityService: ActivityService, private alertService:AlertService) {}

  createRoute(name:string, desc:string, distance:number, elevation:number, id:number) {
    return this.http.post<Route>("/api/routes",
    {
      name:name,
      description:desc,
      totalDistance:distance,
      elevationGain:elevation,
      id:id
    })
  }

  removeActivity(routeId: number, activityId: number): Observable<[Route, Activity[]]> {
    //the whole pipe could be moved to the caller, but it makes sense to me to have it here
    return this.http.delete("/api/routes/"+routeId+"/activities/"+activityId).pipe(
      catchError((e) => {
        console.error("(RoutesComponent) There was an error removing the activity from the route", e);
        this.alertService.toastError("Couldn't remove the activity. Try again later, or after reloading.")
        return EMPTY;
      }),
      switchMap(() => {
        this.alertService.toastSuccess("Activity removed from route successfully")
        console.log("(RoutesComponent) Activity removed from route successfully. Fetching updated route");
        return forkJoin([this.getRoute(routeId), this.getRouteActivities(routeId)])
      })
    )
  }

  addActivitiesToRoute(activityIds: Set<number>, route: number): Observable<any> {
    return this.http.post("/api/routes/"+route+"/activities/", Array.from(activityIds))
  }
  addActivitiesToRouteAndRefetch(activityIds: Set<number>, route: number, modal:NgbModalRef): Observable<[Route, Activity[]]> {
    //the whole pipe could be moved to the caller, but it makes sense to me to have it here
    return this.http.post("/api/routes/"+route+"/activities/", Array.from(activityIds)).pipe(
      catchError((e) => {
        console.error("Error adding activities to route: ", e);
        this.alertService.toastError("Try again later.", "Error adding activities to route")
        return EMPTY;
      }),
      switchMap(() => {
        this.alertService.toastSuccess("Activities added to route successfully")
        console.log("Activities added to route successfully. Fetching updated route");
        modal.dismiss();
        return forkJoin([this.getRoute(route), this.getRouteActivities(route)])
      })
    )
  }

  getRoutesNoActivities(): Observable<any> {
    return this.http.get("/api/routes?type=noActivities")
  }

  deleteRoute(id: number): Observable<any> {
    return this.http.delete("/api/routes/"+id)
  }
  deleteRouteAndRefetch(id: number, mural:number|undefined): Observable<Route[]> {
    //the whole pipe could be moved to the caller, but it makes sense to me to have it here
    return this.deleteRoute(id).pipe(
        catchError((e) => {
          console.error("There was an error deleting the route", e);
          this.alertService.toastError("Couldn't remove the route. Try again later, or after reloading.")
          return EMPTY;
        }),
        switchMap(() => {
          this.alertService.toastSuccess("Route removed successfully")
          console.log("Route removed successfully. Fetching updated route list");
          return this.getRoutes()
        })
      )
  }

  getRoute(id:number): Observable<Route> {
    return this.http.get<Route>("/api/routes/"+id)
  }

  getRouteActivities(id:number): Observable<Activity[]> {
    return this.http.get("/api/routes/"+id+"/activities?mural="+(this.mural??-1)).pipe(map((act:any) => this.activityService.process(act)))
  }
}
