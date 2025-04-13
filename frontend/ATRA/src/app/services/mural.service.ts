
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable, of } from 'rxjs';
import { Router } from '@angular/router';
import { Route } from '../models/route.model';
import { Mural } from '../models/mural.model';


@Injectable({
  providedIn: 'root'
})
export class MuralService {


  constructor(private http: HttpClient, private router: Router) {}

  getOwned(): Observable<Mural[]>{
    return this.http.get<Mural[]>("/api/murals?type=owned").pipe(map(murals=>this.setDefaultThumbnail(murals)))
  }
  getMember(): Observable<Mural[]>{
    return this.http.get<Mural[]>("/api/murals?type=member").pipe(map(murals=>this.setDefaultThumbnail(murals)))
  }
  getOther(): Observable<Mural[]>{
    return this.http.get<Mural[]>("/api/murals?type=other").pipe(map(murals=>this.setDefaultThumbnail(murals)))
  }


  private setDefaultThumbnail(murals:Mural[]): Mural[] {
    murals.forEach(mural => {
      fetch('assets/thumbnailImage.png')
        .then(res => res.blob())
        .then(blob => {
          mural.thumbnail = blob
          mural.thumbnailURL = URL.createObjectURL(blob);
          // Use `url` as the image src
        });
    })
    return murals
  }

  createMural(mural: { name: string; description: string; thumbnail: File; banner: File; }) {
    const formData = new FormData();
    formData.append('name', mural.name);
    formData.append('description', mural.description);
    formData.append('thumbnail', mural.thumbnail);  // File object
    formData.append('banner', mural.banner);  // File object

    return this.http.post<Mural>("/api/murals",formData);
  }
}
