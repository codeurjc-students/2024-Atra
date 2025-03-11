
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Router } from '@angular/router';
import { Route } from '../models/route.model';


@Injectable({
  providedIn: 'root'
})
export class RouteService {

  getRoutes(): Observable<Route[]> {
    return this.http.get<Route[]>("/api/routes");
  }

  constructor(private http: HttpClient, private router: Router) {}

  createRoute(name:string, desc:string, distance:number, elevation:number, id:number) {
    return this.http.post("/api/routes",
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
