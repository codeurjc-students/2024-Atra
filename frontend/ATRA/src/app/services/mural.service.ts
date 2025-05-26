
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
  public static defaultThumbnail: string = 'assets/thumbnailImage.png'; // Default thumbnail path

  constructor(private http: HttpClient, private router: Router) {}

  getOwned(): Observable<Mural[]>{
    return this.http.get<Mural[]>("/api/murals?type=owned")
  }
  getMember(): Observable<Mural[]>{
    return this.http.get<Mural[]>("/api/murals?type=member")
  }
  getOther(): Observable<Mural[]>{
    return this.http.get<Mural[]>("/api/murals?type=other")
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
