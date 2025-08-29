import { AlertService } from './alert.service';

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable, of } from 'rxjs';
import { User } from '../models/user.model';
import { Router } from '@angular/router';
import { AbstractControl, AsyncValidatorFn, ValidationErrors, ValidatorFn } from '@angular/forms';
import { Route } from '../models/route.model';


@Injectable({
  providedIn: 'root'
})
export class UserService {

  constructor(private http: HttpClient, private router: Router, private alertService:AlertService) {}

  getUserById(userId : number):Observable<User>{
    return this.http.get<User>('/api/users/'+userId);
  }

  isUsernameTaken(userName: string) {
    return this.http.get<boolean>("/api/users/IsUsernameTaken?username="+userName)
  }

  createUser(user: User) {
    return this.http.post("/api/users",
      {
        username:user.username,
        password: user.password,
        name: user.name,
        email: user.email
      })
  }

  getCurrentUser(){
    return this.http.get<User>("/api/users/me")
  }

  update(user: User) { //prev user: {id: number, name: string; email: string; username: string}
    return this.http.patch("/api/users/"+user.id, user);
  }
//
  //delete(id: number) {
  //  return this.http.delete("/api/users/"+id);
  //}
//
  //logout(){
  //  this.http.post("/api/auth/logout",{}).subscribe({
  //    next: value => {console.log(value)},
  //    error: err => {console.log(err)}
  //  });
  //}

  confirmPassword(password:string): Observable<boolean> {
    return this.http.post<boolean>("/api/users/verify-password", password)
  }

  updatePassword(newPassword: string) {
    return this.http.post("/api/users/password", newPassword)
  }

  delete() {
    return this.http.delete("/api/users")
  }

  //#region form validators
  isUserNameTaken(prevUsername?:string): AsyncValidatorFn {
    return (control: AbstractControl) => {
      const username = control.value;
      if (prevUsername && username===prevUsername) return of(null);
      return this.isUsernameTaken(username).pipe(
        map((isUsernameTaken: boolean) => {return isUsernameTaken ? { isTaken: true } : null})
      )
    }
  }

  matchPasswords(id1:string, id2:string): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const password = control.get(id1)?.value;
      const confirmPassword = control.get(id2)?.value;

      if (!control.get(id1)?.touched && !control.get(id2)?.touched) return null;

      if (password !== confirmPassword) {
        //this.alertService.alert("Passwords do not match");
        return {differentPasswords:true};
      }
      return null;
    }
  }
  //#endregion

  getActivitiesInMural(muralId: number) {
    return this.http.get<any[]>("/api/activities/OwnedInMural?muralId="+muralId)
  }

  getRoutesInMural(muralId: number): Observable<Route[]>{
    return this.http.get<Route[]>("/api/routes/OwnedInMural?muralId="+muralId)
  }

}
