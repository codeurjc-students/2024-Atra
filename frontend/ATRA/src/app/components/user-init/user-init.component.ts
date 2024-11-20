import { Component } from '@angular/core';
import { LoginComponent } from "../login/login.component";
import { RegisterComponent } from '../register/register.component';

@Component({
  selector: 'app-user-init',
  standalone: true,
  imports: [LoginComponent, RegisterComponent],
  templateUrl: './user-init.component.html',
  styleUrl: './user-init.component.css'
})
export class UserInitComponent {}
