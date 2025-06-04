import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, GuardResult, MaybeAsync, Router, RouterStateSnapshot } from '@angular/router';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  constructor(private auth: AuthService, private router:Router) { }

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): MaybeAsync<GuardResult> {
    //this is called whenever a route that has it bound (check app-routing.module.ts) is navigated to
    //It checks if the user is authenticated, and if not, redirects to the login page

    if (this.auth.isAuthenticated()) return true;

    this.auth.setRedirectUrl(state.url); // Store the attempted URL for redirecting after login
    return this.router.createUrlTree(['']); //new RedirectCommand(this.router.createUrlTree(['']));
  }
}
