import { AlertService } from './alert.service';
import { Injectable, OnInit } from '@angular/core';
import { User } from '../models/user.model';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, BehaviorSubject, retry, of, map } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  redirectUrl: string | null= null;
  readonly user: BehaviorSubject<User | null> = new BehaviorSubject<User | null>(null);
  fetchingUser: boolean = false;

  constructor(private http:HttpClient, private alertService:AlertService) {
    this.user.subscribe(user=>{console.log(`(AuthService) Currently Authenticated user changed:`, user);})

    const storedUser = localStorage.getItem("user")
    if (storedUser!=='null' && storedUser!=null)
      this.user.next(JSON.parse(storedUser)); //as User
  }

  setRedirectUrl(url: string) {
    this.redirectUrl = url;
  }
  getRedirectUrl(): string | null {
    return this.redirectUrl;
  }

  private isFrontAuthenticated() {
    if (this.fetchingUser) return true; // if we are fetching the user, we ASSUME the user is authenticated. This is because this.fetchingUser means fetchAndSetUser was called. This only happens inmediately after a successful call to login. So even though the user may not yet be cached, and caching could even failed, they have valid credentials and should be considered logged in
    return this.user.getValue() !== null;
  }
  private isBackAuthenticated(): Observable<boolean> { //should only be called when isFrontAuthenticated fails
    // Possibly, a call to /me would be better (effectively just fetchAndSetUser). We could use it to update the cache as well (though if the cache is only used for permissions I don't think we'd really need that)
    return this.http.get<boolean>("/api/auth/IsLoggedIn").pipe(tap({
      next:(isLoggedIn) => {
        if (isLoggedIn) {
          this.fetchAndSetUser(); //update the cache
        }
      },
      error:(e)=>{
        console.error("Something went wrong checking the user's authentication, ", e);
      }
    }))
  }

  logout() {
    this.http.post("/api/auth/logout",{}).subscribe({
      next: ()=> {
        this.user.next(null);
        this.fetchingUser = false; //just in case
        localStorage.removeItem("user");
      },
      error: (err) => {
        console.error("Logout failed: ", err);
        this.alertService.toastError("Logout failed");
      }

    });
  }

  login(username: string, password: string): Observable<any> {
    localStorage.removeItem("user");
    return this.http.post("/api/auth/login", { username, password }).pipe(tap((response: any) => {if (response.status === "SUCCESS") this.fetchAndSetUser()}));
  }

  fetchAndSetUser(): void {
    this.fetchingUser = true;
    this.http.get<User>("/api/users/me").subscribe({
      next: (user) => {
        this.user.next(user);
        this.fetchingUser = false;
        localStorage.setItem("user", JSON.stringify(user)); // Store user in localStorage
      },
      error: (error) => {
        console.error('(AuthService) Error fetching user after login:', error);
        this.user.next(null);
        this.fetchingUser = false;
      }
    })
  }

  isLoggedIn() {
    if (this.isFrontAuthenticated()) return of(true);
    return this.isBackAuthenticated();
  }
}
