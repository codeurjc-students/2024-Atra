import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { WelcomeComponent } from './components/welcome/welcome.component';
import { HomeComponent } from './components/home/home.component';
import { MuralsComponent } from './components/murals/murals.component';
import { ActivitySelectComponent } from './components/activity-select/activity-select.component';
import { ComparisonComponent } from './components/comparison/comparison.component';
import { ActivityComponent } from './components/activity/activity.component';
import { RoutesComponent } from './components/routes/routes.component';
import { ErrorComponent } from './components/error/error.component';
import { ProfileComponent } from './components/profile/profile.component';
import { MuralsDashboardComponent } from './components/murals-dashboard/murals-dashboard.component';
import { AuthGuard } from './services/auth-guard.service';

export const routes: Routes = [
  {path: '', component: WelcomeComponent}, //pretty sure login and register should be removed, they are actually handled by the root
  {path: 'login', component: LoginComponent}, //pretty sure login and register should be removed, they are actually handled by the root
  {path: 'register', component: RegisterComponent}, //pretty sure login and register should be removed, they are actually handled by the root
  {path: 'error', component: ErrorComponent, canActivate: [AuthGuard],},
  {path: 'me', component: ProfileComponent, canActivate: [AuthGuard],},
  {path: 'me/home', component: HomeComponent, canActivate: [AuthGuard],},
  {path: 'me/routes', component: RoutesComponent, canActivate: [AuthGuard],},
  //{path: 'me/activity-comparison', component: ActivitySelectComponent, canActivate: [AuthGuard],},
  {path: 'me/activities/compare/:id', component: ComparisonComponent, canActivate: [AuthGuard],}, // this with extra ones in query parameters. Alt would be me/activity-comparison?ids=1,2
                                                                        // this depends on wether I want to differentiate the elements being compared. If there is one important one
                                                                        // being compared against others, it should be /:id?ids=1,2. If all take the same importance, it should be
                                                                        // /activity-comparison?ids=1,2,
  {path: 'me/activities', component: ActivitySelectComponent, canActivate: [AuthGuard],},
  {path: 'me/activities/:id', component: ActivityComponent, canActivate: [AuthGuard],},
  {path: 'murals', component: MuralsComponent, canActivate: [AuthGuard],},
  {path: 'murals/:category', component: MuralsComponent, canActivate: [AuthGuard],},
  //{path: 'murals/:id/activity-comparison', component: ComparisonComponent, canActivate: [AuthGuard],},
  {path: 'murals/:id/activities', component: ActivitySelectComponent, canActivate: [AuthGuard],},
  {path: 'murals/:id/activities/:id', component: ActivityComponent, canActivate: [AuthGuard],},
  {path: 'murals/:id/dashboard', component: MuralsDashboardComponent, canActivate: [AuthGuard],},
  {path: 'murals/:id/routes', component: RoutesComponent, canActivate: [AuthGuard],},

  {path: '**', component: ErrorComponent},

];
