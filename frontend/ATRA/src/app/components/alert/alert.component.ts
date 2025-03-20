import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'app-alert',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './alert.component.html',
  styleUrl: './alert.component.css'
})
export class AlertComponent {
  @Input() title!: string;
  @Input() message!: string;
  @Input() accept: string = "YES";
  @Input() cancel: string = "NO";
  @Input() type: 'alert' | 'confirm' | 'inputConfirm' |'loading' = 'alert';
  @Input() placeholder: string = 'delete';
  text:string = "";

  constructor(public activeModal: NgbActiveModal) {}

  confirm() {
    this.activeModal.close(this.type=='inputConfirm'? {accept:true,text:this.text}:true); // Resolves the confirm() promise as true
  }

  dismiss() {
    this.activeModal.dismiss(); // Resolves the confirm() promise as false
  }

}
