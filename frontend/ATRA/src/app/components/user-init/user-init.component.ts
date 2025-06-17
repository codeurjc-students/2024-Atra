import { AlertService } from './../../services/alert.service';
import { Component } from '@angular/core';
import { LoginComponent } from "../login/login.component";
import { RegisterComponent } from '../register/register.component';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'app-user-init',
  standalone: true,
  imports: [],
  templateUrl: './user-init.component.html',
  styleUrl: './user-init.component.scss'
})
export class UserInitComponent {
  constructor(private modalService: NgbModal, private alertService:AlertService){}

  openRegisterModal() {
    const modalRef = this.modalService.open(RegisterComponent);
  }
  openLoginModal() {
    const modalRef = this.modalService.open(LoginComponent);
  }
}
