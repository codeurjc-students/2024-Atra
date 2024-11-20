
import { Injectable } from '@angular/core';
import {HttpClient, HttpResponse} from '@angular/common/http';
import {catchError, map, Observable} from 'rxjs';
import { User } from '../models/user.model';
import { Router } from '@angular/router';


@Injectable({
  providedIn: 'root'
})
export class UserService {

  constructor(private http: HttpClient, private router: Router) {}

  login(username: string, password: string): Observable<any> {
    const body = { username, password };
    return this.http.post("/api/auth/login", body);
  }

  getUserById(userId : number):Observable<User>{
    return this.http.get<User>('/api/users/'+userId);
  }

  isUsernameTaken(userName: string) {
    return this.http.get<boolean>("/api/users/IsUsernameTaken?username="+userName)
    //.pipe(
    //  map(
    //    (response: HttpResponse<any>) => {
    //      return response;
    //    }
    //  )
    //)
  }

  createUser(user: User) {
    this.http.post("/api/users",
      {
        username:user.username,
        password: user.password,
        displayname: user.displayname,
        email: user.email
      }).subscribe({
        next: () => {
          alert("You have successfully created your account")
          window.location.reload()
        },
        error: () => {
          this.router.navigate(["/error"])
        }
      }
    );
  }

  //update(user: {id: number, name: string; email: string; username: string}) {
  //  return this.http.post("/api/users/updateProfile", user);
  //}
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

}
