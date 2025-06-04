import { AlertService } from './alert.service';
import { HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthInterceptorService {

  constructor(private router: Router, private authService: AuthService, private alertService:AlertService) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    //this is called whenever an answer is received from the server
    //it checks if the answer is 401 or 403
    // if 401, the user is logged out and redirected to the login page, since their credentials have expired
    // if 403, the user is shown a toast indicating they tried to access a forbidden resource

    return next.handle(req).pipe(
      catchError(err => {
        if (err.status === 401) {
          this.authService.logout(); // maybe clear tokens, etc.
          this.alertService.alert("You are not logged in, or your credentials have expired. You'll be redirected to the login page.", "Not logged in",  ()=>this.router.navigate(['/']));
        } else if (err.status === 403) {
          this.alertService.alert('You do not have permission to access this resource.', '403 Forbidden'); //Should be toast
        }
        return throwError(() => err);
      })
    );
  }
}
