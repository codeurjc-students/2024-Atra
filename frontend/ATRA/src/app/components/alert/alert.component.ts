import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'app-alert',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './alert.component.html',
  styleUrl: './alert.component.css'
})
export class AlertComponent {
  @Input() title!: string;
  @Input() message!: string;
  @Input() accept: string = "YES";
  @Input() cancel: string = "NO";
  @Input() isConfirm = false;

  constructor(public activeModal: NgbActiveModal) {}

  confirm() {
    this.activeModal.close(true); // Resolves the confirm() promise as true
  }

  dismiss() {
    this.activeModal.dismiss(); // Resolves the confirm() promise as false
  }

}
