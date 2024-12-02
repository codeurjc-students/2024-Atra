import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { WelcomeComponent } from './components/welcome/welcome.component';
import { HomeComponent } from './components/home/home.component';
import { MuralsComponent } from './components/murals/murals.component';
import { ActivityComponent } from './components/activity/activity.component';
import { ComparisonComponent } from './components/comparison/comparison.component';

export const routes: Routes = [
  {path: '', component: WelcomeComponent},
  {path: 'login', component: LoginComponent},
  {path: 'register', component: RegisterComponent},
  {path: 'home', component: HomeComponent},
  {path: 'activity-comparison', component: ComparisonComponent},
  {path: 'activity-view', component: ActivityComponent},
  {path: 'murals', component: MuralsComponent}
];
