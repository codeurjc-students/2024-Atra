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
    //it checks for errors and displays messages accordingly
    // if the error is 401, the user is logged out and redirected to the login page, since their credentials have expired
    //This may become a bother, as many requests have their own error messages. This may lead to multiple notifications being given
    // Ideally, there'd be a way to deactivate some of the interceptor's functionality for a specific request when making it,
    // or the interceptor's message could be configured when the request is made (though this runs into concurrency issues)

    return next.handle(req).pipe(
      catchError(err => {
        if (err.status === 400) {
          this.alertService.alert("Something is wrong with your request, we can't process it.", '400 Bad Request'); //Should be toast
        } else if (err.status === 401) {
          //check that the user is actually logged out and it's not an error

          this.alertService.confirm(
            "You are not logged in, or your credentials have expired. \n If you think this is a mistake, you can reload the page. If it was a mistake you'll be allowed to continue.\n Otherwise, click 'ok', and you'll be redirected to the login page.",
            "Not logged in",
            {accept:'Log out', cancel:'Refresh page'}
          ).subscribe((accepted:boolean)=>{
            if (accepted) {
              this.authService.logout();
              this.router.navigate(['/'])
            } else {
              window.location.reload();
            }

          });
        } else if (err.status === 403) {
          this.alertService.alert('You do not have permission to access this resource.', '403 Forbidden'); //Should be toast
        } else if (err.status === 404) {
          this.alertService.alert("We could not find the resource you're looking for, it may have been deleted", '404 Not found'); //Should be toast
        } else if (err.status === 500) {
          this.alertService.alert("Something went wrong on our side. Try again later.", '500 Internal server error'); //Should be toast
        }
        return throwError(() => err);
      })
    );
  }
}
