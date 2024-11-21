import { Component } from '@angular/core';
import { LoginComponent } from "../login/login.component";
import { RegisterComponent } from '../register/register.component';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'app-user-init',
  standalone: true,
  imports: [LoginComponent, RegisterComponent],
  templateUrl: './user-init.component.html',
  styleUrl: './user-init.component.css'
})
export class UserInitComponent {
  constructor(private modalService: NgbModal){}

  openRegisterModal() {
    const modalRef = this.modalService.open(RegisterComponent);
  }
  openLoginModal() {
    const modalRef = this.modalService.open(LoginComponent);
  }
}
