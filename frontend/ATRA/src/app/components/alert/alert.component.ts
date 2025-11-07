import { CommonModule } from '@angular/common';
import { Component, Input, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'app-alert',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './alert.component.html',
  styleUrl: './alert.component.scss'
})
export class AlertComponent implements OnInit {
  @Input() title!: string;
  @Input() messages!: string[];
  @Input() accept: string = "YES";
  @Input() cancel: string = "NO";
  @Input() type: 'alert' | 'confirm' | 'inputConfirm' |'loading-light' | 'loading-heavy' = 'alert';
  @Input() placeholder: string = 'delete';
  @Input() times: number = 1;
  @Input() otherOpenAlerts: number = 0;
  text:string = "";
  btnDismissAlert:string = "btn-dismiss-alert" + (this.otherOpenAlerts!=0?('-'+this.otherOpenAlerts):'');

  constructor(public activeModal: NgbActiveModal) {
  }

  ngOnInit() {
    this.btnDismissAlert = "btn-dismiss-alert" + (this.otherOpenAlerts!=0?('-'+this.otherOpenAlerts):'');

  }

  confirm() {
    this.activeModal.close(this.type=='inputConfirm'? {accept:true,text:this.text}:true); // Resolves the confirm() promise as true
  }

  dismiss() {
    this.activeModal.dismiss(); // Resolves the confirm() promise as false
  }

}
