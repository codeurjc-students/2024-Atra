import { Injectable, OnInit } from '@angular/core';
import { User } from '../models/user.model';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, BehaviorSubject, retry, of } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  redirectUrl: string | null= null;
  readonly user: BehaviorSubject<User | null> = new BehaviorSubject<User | null>(null);
  fetchingUser: boolean = false;

  constructor(private http:HttpClient) {
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
  isAuthenticated() {
    if (this.fetchingUser) return true; // if we are fetching the user, we ASSUME the user is authenticated
    return this.user.getValue() !== null;
  }

  logout() {
    this.user.next(null);
    this.fetchingUser = false; //just in case
    localStorage.removeItem("user");
    this.http.post("/api/auth/logout",{});
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
    //the method should by all means be merged with isAuthenticated
    if (this.isAuthenticated()) return of(true);
    return this.http.get<boolean>("/api/users/IsLoggedIn")
  }
}
