
import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import {catchError, map, Observable} from 'rxjs';
import { User } from '../models/user.model';
import { Router } from '@angular/router';


@Injectable({
  providedIn: 'root'
})
export class RouteService {

  constructor(private http: HttpClient, private router: Router) {}

  createRoute(name:string, desc:string, distance:number, elevation:number, id:number) {
    console.log("posting");

    this.http.post("/api/routes",
    {
      name:name,
      description:desc,
      totalDistance:distance,
      elevationGain:elevation,
      id:id
    }).subscribe({
      next: () => {

        //load up http://localhost:4200/me/routes or similar
      },
      error: (e) => {
        console.log(e);


      }
    }
  );
  }
}
