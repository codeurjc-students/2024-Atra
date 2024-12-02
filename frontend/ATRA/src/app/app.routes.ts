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
  {path: 'me/home', component: HomeComponent},
  {path: 'me/activity-comparison', component: ComparisonComponent},
  {path: 'me/activity-view', component: ActivityComponent},
  {path: 'mural/home', component: MuralsComponent},
  {path: 'mural/activity-comparison', component: ComparisonComponent},
  {path: 'mural/activity-view', component: ActivityComponent},
];
