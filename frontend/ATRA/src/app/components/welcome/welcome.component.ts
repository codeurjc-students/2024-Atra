import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { UserInitComponent } from "../user-init/user-init.component";
import { AnonInitComponent } from "../anon-init/anon-init.component";

@Component({
  selector: 'app-welcome',
  standalone: true,
  imports: [CommonModule, UserInitComponent, AnonInitComponent],
  templateUrl: './welcome.component.html',
  styleUrl: './welcome.component.scss'
})
export class WelcomeComponent {

}
