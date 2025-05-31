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

export const routes: Routes = [
  {path: '', component: WelcomeComponent},
  {path: 'login', component: LoginComponent},
  {path: 'register', component: RegisterComponent},
  {path: 'error', component: ErrorComponent},
  {path: 'me', component: ProfileComponent},
  {path: 'me/home', component: HomeComponent},
  {path: 'me/routes', component: RoutesComponent},
  {path: 'me/activity-comparison', component: ActivitySelectComponent},
  {path: 'me/activity-comparison/:id', component: ComparisonComponent}, // this with extra ones in query parameters. Alt would be me/activity-comparison?ids=1,2
                                                                        // this depends on wether I want to differentiate the elements being compared. If there is one important one
                                                                        // being compared against others, it should be /:id?ids=1,2. If all take the same importance, it should be
                                                                        // /activity-comparison?ids=1,2,
  {path: 'me/activity-view', component: ActivitySelectComponent},
  {path: 'me/activity-view/:id', component: ActivityComponent}, //
  {path: 'murals', component: MuralsComponent},
  {path: 'murals/:category', component: MuralsComponent},
  {path: 'murals/:id/activity-comparison', component: ComparisonComponent},
  {path: 'murals/:id/activity-view', component: ActivitySelectComponent},
  {path: 'murals/:id/dashboard', component: MuralsDashboardComponent},

  {path: '**', component: ErrorComponent},

];
