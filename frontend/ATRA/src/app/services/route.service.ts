
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Router } from '@angular/router';
import { Route } from '../models/route.model';


@Injectable({
  providedIn: 'root'
})
export class RouteService {
  makeRoutesNotVisibleToMural(id: number, selectedRoutes: Set<number> | null) {
    if (selectedRoutes==null) throw new Error("(RouteService) makeRoutessNotVisibleToMural called with null selectedRoutes");
    return this.http.patch("/api/routes/visibility/mural?id="+id, Array.from(selectedRoutes)/*,   { headers: { 'Content-Type': 'application/json' } }*/)
  }
  changeVisibility(id: number, currentVis: "PUBLIC" | "MURAL_SPECIFIC" | "PRIVATE", allowedMuralsList: number[] = []): Observable<Route> {
    return this.http.patch<Route>("/api/routes/"+id+"/visibility", {"visibility": currentVis, "allowedMuralsList": JSON.stringify(allowedMuralsList)});
  }

  getRoutes(mural?:number|undefined): Observable<Route[]> {
    if (mural==undefined)
      return this.http.get<Route[]>("/api/routes");
    else
      return this.http.get<Route[]>("/api/routes?from=mural&id="+mural);

  }

  constructor(private http: HttpClient, private router: Router) {}

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

  removeActivity(routeId: number, activityId: number): Observable<any> {
    return this.http.delete("/api/routes/"+routeId+"/activities/"+activityId)
  }

  addActivitiesToRoute(activityIds: Set<number>, route: number): Observable<any> {
    return this.http.post("/api/routes/"+route+"/activities/", Array.from(activityIds))
  }

  fetchAllRoutes(): Observable<any> {
    console.log("fetchingroutes");

    return this.http.get("/api/routes?type=noActivities")
  }

  deleteRoute(id: number): Observable<any> {
    console.log("b");
    return this.http.delete("/api/routes/"+id)
  }
}
