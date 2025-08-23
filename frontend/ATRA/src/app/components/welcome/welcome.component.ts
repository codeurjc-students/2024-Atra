import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { NgbModal, NgbPopover } from '@ng-bootstrap/ng-bootstrap';
import { RegisterComponent } from '../register/register.component';
import { LoginComponent } from '../login/login.component';
import { ActivityService } from '../../services/activity.service';

@Component({
  selector: 'app-welcome',
  standalone: true,
  imports: [CommonModule, NgbPopover],
  templateUrl: './welcome.component.html',
  styleUrl: './welcome.component.scss'
})
export class WelcomeComponent {

  constructor(private modalService: NgbModal, private activityService:ActivityService){}

  onFileSelected(event: Event){
    this.activityService.uploadActivity(event)
  }

  openRegisterModal() {
    const modalRef = this.modalService.open(RegisterComponent, { centered:true });
  }
  openLoginModal() {
    const modalRef = this.modalService.open(LoginComponent, { centered:true });
  }

}
