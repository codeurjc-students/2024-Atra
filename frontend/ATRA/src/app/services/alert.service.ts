import { Injectable } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { AlertComponent } from '../components/alert/alert.component';
import { catchError, from, map, Observable, of } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AlertService {

  constructor(private modalService: NgbModal) {}

  // Alert method (just shows a message)
  alert(message: string, title?: string, onDismiss?:()=>void) {
    const modalRef = this.modalService.open(AlertComponent, { backdrop: 'static', keyboard: false, centered:true, windowClass:'remove-modal-background' });
    modalRef.componentInstance.title = title ?? "Warning";
    modalRef.componentInstance.message = message;
    if (onDismiss!=null)
      from(modalRef.dismissed).subscribe({
        next:onDismiss,
        error:(e)=>{
          console.log("Something somehow went wrong.")
          console.log(e.error);
        }
      })
  }

  // Confirm method (returns a promise resolving to true/false)
  confirm(message: string, title?: string, options?:{accept:string, cancel:string}): Observable<boolean> {
    const modalRef = this.modalService.open(AlertComponent, { backdrop: 'static', keyboard: false, centered:true, windowClass:'remove-modal-background' });
    modalRef.componentInstance.title = title ?? "Warning";
    modalRef.componentInstance.message = message;
    modalRef.componentInstance.isConfirm = true;
    if (options){
      modalRef.componentInstance.accept = options.accept;
      modalRef.componentInstance.cancel = options.cancel;
    }

    return from(modalRef.result).pipe(
      map(result => result ?? true), // when closed
      catchError(reason => of(reason ?? false)) // when dismissed
    );
  }
}
