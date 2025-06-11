
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, map } from 'rxjs';
import { Router } from '@angular/router';
import { Mural } from '../models/mural.model';


@Injectable({
  providedIn: 'root'
})
export class MuralService {
  public static defaultThumbnail: string = 'assets/thumbnailImage.png'; // Default thumbnail path

  constructor(private http: HttpClient, private router: Router) {}

  getOwned(): Observable<Mural[] | null>{ //shouldn't this be && !this.loadingOnwned
    if (this.ownedMurals.getValue() == null && !this.loadingOwned) {
      this.loadingOwned = true;
      this.http.get<Mural[]>("/api/murals?type=owned").subscribe(murals  => {this.ownedMurals.next(murals); this.loadingOwned=false; setTimeout(() => this.ownedMurals.next(null),  this.CACHE_DURATION);});
    }
    return this.ownedMurals.asObservable();
  }
  getMember(): Observable<Mural[] | null>{
    if (this.memberMurals.getValue() == null && !this.loadingMember) {
      this.loadingMember = true;
      this.http.get<Mural[]>("/api/murals?type=member").subscribe(murals  => {this.memberMurals.next(murals); this.loadingMember=false;  setTimeout(() => this.memberMurals.next(null),  this.CACHE_DURATION);});
    }
    return this.memberMurals.asObservable();
  }
  getOther(): Observable<Mural[] | null>{ // this can also be done with tap and switchMap, but this is just as good
    if (this.otherMurals.getValue() == null && !this.loadingOther) {
      this.loadingOther = true;
      this.http.get<Mural[]>("/api/murals?type=other").subscribe(murals  => {this.otherMurals.next(murals); this.loadingOther=false;  setTimeout(() => this.otherMurals.next(null),  this.CACHE_DURATION);});
    }
    return this.otherMurals.asObservable();
  }

  getMural(id:number): Observable<Mural> {
    return this.http.get<Mural>("/api/murals/"+id)
  }

  createMural(mural: { name: string; description: string; thumbnail: File; banner: File; }) {
    const formData = new FormData();
    formData.append('name', mural.name);
    formData.append('description', mural.description);
    formData.append('thumbnail', mural.thumbnail);  // File object
    formData.append('banner', mural.banner);  // File object

    return this.http.post<Mural>("/api/murals",formData);
  }

  //state handling
  readonly CACHE_DURATION: number = 1000 * 30; // 30 seconds for testing, should bump to 1000 * 60 * 5 for 5 minutes in production
  ownedMurals: BehaviorSubject<Mural[] | null> = new BehaviorSubject<Mural[] | null>(null);
  memberMurals: BehaviorSubject<Mural[] | null> = new BehaviorSubject<Mural[] | null>(null);
  otherMurals: BehaviorSubject<Mural[] | null> = new BehaviorSubject<Mural[] | null>(null);
  loadingOwned: boolean = false; // to prevent multiple requests
  loadingMember: boolean = false; // to prevent multiple requests
  loadingOther: boolean = false; // to prevent multiple requests

  private getOwnedNoCache(): Observable<Mural[]> {return this.http.get<Mural[]>("/api/murals?type=owned")}
  private getMemberNoCache(): Observable<Mural[]> {return this.http.get<Mural[]>("/api/murals?type=member")}
  private getOtherNoCache(): Observable<Mural[]> {return this.http.get<Mural[]>("/api/murals?type=other")}

  loadData(): void {
    this.getOwnedNoCache().subscribe(murals  => {this.ownedMurals.next(murals);  setTimeout(() => this.ownedMurals.next(null),  this.CACHE_DURATION);});
    this.getMemberNoCache().subscribe(murals => {this.memberMurals.next(murals); setTimeout(() => this.memberMurals.next(null), this.CACHE_DURATION);});
    this.getOtherNoCache().subscribe(murals  => {this.otherMurals.next(murals);  setTimeout(() => this.otherMurals.next(null),  this.CACHE_DURATION);});
  }
  unloadData(): void {
    //not necessarily necessary, it's not used, but good for completion's sake
    this.ownedMurals.next(null);
    this.memberMurals.next(null);
    this.otherMurals.next(null);
  }

  getData(category: string): Observable<Mural[] | null> {
    switch (category) {
      case 'owned': return this.ownedMurals.asObservable();
      case 'member': return this.memberMurals.asObservable();
      case 'other': return this.otherMurals.asObservable();
      default:
        throw new Error('Invalid category');
    }
  }

}
