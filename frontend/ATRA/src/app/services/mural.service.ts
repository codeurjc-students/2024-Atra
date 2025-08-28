
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

  constructor(private http: HttpClient, private router: Router) {
    this.ownedMurals.subscribe((m)=>console.log("(MuralService) ------------------------------- Owned Murals updated: ", m?.length ?? null ));
    this.memberMurals.subscribe((m)=>console.log("(MuralService) ------------------------------- Member Murals updated: ", m?.length ?? null ));
    this.otherMurals.subscribe((m)=>console.log("(MuralService) ------------------------------- Other Murals updated: ", m?.length ?? null ));
  }

  getOwned(): Observable<Mural[] | null>{ //shouldn't this be && !this.loadingOnwned
    if (this.ownedMurals.getValue() == null && !this.loadingOwned) {
      this.loadingOwned = true;
      this.http.get<Mural[]>("/api/murals?type=owned").subscribe(murals => {
        this.ownedMurals.next(murals);
        this.loadingOwned=false;
        window.clearTimeout(this.timeoutOwned);
        this.timeoutOwned = window.setTimeout(() => this.ownedMurals.next(null),  this.CACHE_DURATION);
      });
    }
    return this.ownedMurals.asObservable();
  }
  getMember(): Observable<Mural[] | null>{
    if (this.memberMurals.getValue() == null && !this.loadingMember) {
      this.loadingMember = true;
      this.http.get<Mural[]>("/api/murals?type=member").subscribe(murals => {
        this.memberMurals.next(murals);
        this.loadingMember=false;
        window.clearTimeout(this.timeoutMember);
        this.timeoutMember = window.setTimeout(() => this.memberMurals.next(null),  this.CACHE_DURATION);
      });
    }
    return this.memberMurals.asObservable();
  }
  getOther(): Observable<Mural[] | null>{ // this can also be done with tap and switchMap, but this is just as good
    if (this.otherMurals.getValue() == null && !this.loadingOther) {
      this.loadingOther = true;
      this.http.get<Mural[]>("/api/murals?type=other").subscribe(murals => {
        this.otherMurals.next(murals);
        this.loadingOther=false;
        window.clearTimeout(this.timeoutOther);
        this.timeoutOther = window.setTimeout(() => this.otherMurals.next(null),  this.CACHE_DURATION);});
    }
    return this.otherMurals.asObservable();
  }

  getMural(id:number): Observable<Mural> {
    return this.http.get<Mural>("/api/murals/"+id)
  }

  createMural(mural: { name: string; description: string; visibility:"PUBLIC"|"PRIVATE"; thumbnail: File; banner: File; }) {
    const formData = new FormData();
    formData.append('name', mural.name);
    formData.append('description', mural.description);
    formData.append('visibility', mural.visibility);
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
  timeoutOwned: number  | undefined = undefined;
  timeoutMember: number | undefined = undefined;
  timeoutOther: number  | undefined = undefined;

  private getOwnedNoCache(): Observable<Mural[]> {return this.http.get<Mural[]>("/api/murals?type=owned")}
  private getMemberNoCache(): Observable<Mural[]> {return this.http.get<Mural[]>("/api/murals?type=member")}
  private getOtherNoCache(): Observable<Mural[]> {return this.http.get<Mural[]>("/api/murals?type=other")}

  loadData(): void {
    this.getOwnedNoCache().subscribe(murals  => {this.ownedMurals.next(murals);  window.clearTimeout(this.timeoutOwned); this.timeoutOwned=window.setTimeout(() => this.ownedMurals.next(null),  this.CACHE_DURATION);});
    this.getMemberNoCache().subscribe(murals => {this.memberMurals.next(murals); window.clearTimeout(this.timeoutMember); this.timeoutMember=window.setTimeout(() => this.memberMurals.next(null), this.CACHE_DURATION);});
    this.getOtherNoCache().subscribe(murals  => {this.otherMurals.next(murals);  window.clearTimeout(this.timeoutOther); this.timeoutOther=window.setTimeout(() => this.otherMurals.next(null),  this.CACHE_DURATION);});
  }
  unloadData(unloadOnly?:Partial<Record<'owned' | 'member' | 'other', true>>): void {
    //not necessarily necessary, it's not used, but good for completion's sake
    if (unloadOnly==null) {
      window.clearTimeout(this.timeoutOwned);
      window.clearTimeout(this.timeoutMember);
      window.clearTimeout(this.timeoutOther);

      this.ownedMurals.next(null);
      this.memberMurals.next(null);
      this.otherMurals.next(null);

    } else {
      if (unloadOnly.owned) {window.clearTimeout(this.timeoutOwned);this.ownedMurals.next(null);}
      if (unloadOnly.member) {window.clearTimeout(this.timeoutMember);this.memberMurals.next(null);}
      if (unloadOnly.other) {window.clearTimeout(this.timeoutOther);this.otherMurals.next(null);}

    }
  }

  getData(category: string): Observable<Mural[] | null> {
    switch (category) {
      case 'owned': return this.getOwned();
      case 'member': return this.getMember();
      case 'other': return this.getOther();
      default:
        throw new Error('Invalid category');
    }
  }

    changeName(id: number, newName: string) {
    return this.http.patch<Mural>("/api/murals/"+id, {name: newName})
  }
  changeDesc(id: number, newDesc: string) {
    return this.http.patch<Mural>("/api/murals/"+id, {description: newDesc})
  }
  changeOwner(id: number, newOwner: number) {
    return this.http.patch<Mural>("/api/murals/"+id, {owner: newOwner})
  }
  changeThumbnail(id: number, thumbnail: File) {
    const formData = new FormData();
    formData.append("file", thumbnail);
    return this.http.put("/api/murals/"+id+"/thumbnail", formData)
  }
  changeBanner(id: number, banner: File) {
    const formData = new FormData();
    formData.append("file", banner);
    return this.http.put("/api/murals/"+id+"/banner", formData)
  }
  leave(muralId: number, inheritor?:number) {
    return this.http.delete("/api/murals/"+muralId+"/users/me"+(inheritor?("?inheritor="+inheritor):""))
  }
  kick(muralId: number, userId:number) {
    return this.http.delete<{name:string,id:number}[]>("/api/murals/"+muralId+"/users/"+userId)
  }
  ban(muralId: number, userId: number) {
    return this.http.post<{name:string,id:number}[]>("/api/murals/"+muralId+"/users/"+userId+"/ban", {})
  }
  unban(muralId: number, userId: number) {
    return this.http.post<{name:string,id:number}[]>("/api/murals/"+muralId+"/users/"+userId+"/unban", {})
  }
  joinMuralCode(muralCode: string): Observable<number> {
    return this.http.post<number>(`/api/murals/join`,muralCode)
  }
  joinMuralId(muralId: number): Observable<number> {
    return this.http.post<number>(`/api/murals/join`,muralId)
  }

  deleteMural(id:number) {
    return this.http.delete("/api/murals/"+id)
  }

  //Helper methods
  checkAspectRatio(file: File, desiredRatio:number, tolerance:number=0.01): Observable<boolean> {
    return new Observable((observer) => {
      //create an image to host the file, and a reader to cast the file into the image
      const img = new Image();
      const reader = new FileReader();

      //customize the reader and img. Reassigning img.source loads it
      reader.onload = (e) => {
        img.src = e.target?.result as string;
      };
      img.onload = () => {
        const aspectRatio = img.width / img.height;
        console.log("Calculated aspect ratio: " + aspectRatio);
        console.log("Desired ratio: " + desiredRatio);
        const isValid = Math.abs(aspectRatio - desiredRatio) < tolerance; // optional tolerance
        console.log("isValid: "+isValid);

        observer.next(isValid);
        observer.complete();
      };

      reader.readAsDataURL(file);
    });
  }

  isVisible(muralId: number) {
    return this.http.get<boolean>("/api/murals/"+muralId+"/isVisible")
  }

}
