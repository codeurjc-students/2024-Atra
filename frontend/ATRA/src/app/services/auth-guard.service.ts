import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, GuardResult, MaybeAsync, Router, RouterStateSnapshot } from '@angular/router';
import { AuthService } from './auth.service';
import { AlertService } from './alert.service';
import { map, of, switchMap } from 'rxjs';
import { MuralService } from './mural.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  constructor(private auth: AuthService, private router:Router, private alertService:AlertService, private muralService: MuralService) { }

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): MaybeAsync<GuardResult> {
    //this is called whenever a route that has it bound is navigated to (check app-routing.module.ts to see which routes use it)
    //It checks if the user is authenticated, and if not, redirects to the login page
    this.alertService.loading();
    //if the loading screen becomes too much of an issue, we can go back to how it was previously, with isFrontAuthenticated.
    return this.auth.isLoggedIn().pipe(
      switchMap(isLoggedIn => {
        this.alertService.loaded()

        if (!isLoggedIn) {
          this.auth.setRedirectUrl(state.url); // Store the attempted URL for redirecting after login
          this.alertService.toastInfo("You need to log in to access this resource")
          return of(this.router.createUrlTree([''])); //new RedirectCommand(this.router.createUrlTree(['']));
        }
        //check if it's a mural with no access
        if (state.url.startsWith('/murals/')) {
          const muralId = route.paramMap.get('id') ?? route.paramMap.get('muralId');
          if (isNaN(Number(muralId))) return of(true) //skip if id is not a number (other,owned...)
          return this.muralService.isVisible(Number(muralId)).pipe(map(
            isVisible => {
              if (isVisible) return true
              this.alertService.toastInfo("You don't have access to this mural");
              return this.router.createUrlTree(['/murals']); // stay on current page
            }
          ))
        }

      return of(true);
      })
    )
  }
}
