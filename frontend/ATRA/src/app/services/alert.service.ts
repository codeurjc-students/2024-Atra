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
    modalRef.componentInstance.type = 'alert';

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
    modalRef.componentInstance.type = 'confirm';
    if (options){
      modalRef.componentInstance.accept = options.accept;
      modalRef.componentInstance.cancel = options.cancel;
    }

    return from(modalRef.result).pipe(
      map(result => result ?? true), // when closed
      catchError(reason => of(reason ?? false)) // when dismissed
    );
  }

  loading(){
    const modalRef = this.modalService.open(AlertComponent, { backdrop: 'static', keyboard: false, centered:true, windowClass:'remove-modal-background' });
    modalRef.componentInstance.title = "Loading...";
    modalRef.componentInstance.message = "Wait a second while we take care of some things\n This message should disappear shortly.";
    modalRef.componentInstance.type = 'loading';
    return
  }

  // Confirm method (returns a promise resolving to true/false)
  inputConfirm(message: string, title?: string, options?:{accept:string, cancel:string}, placeholder?:string): Observable<{accept:boolean, text:string}> {
    const modalRef = this.modalService.open(AlertComponent, { backdrop: 'static', keyboard: false, centered:true, windowClass:'remove-modal-background' });
    modalRef.componentInstance.title = title ?? "Warning";
    modalRef.componentInstance.message = message;
    modalRef.componentInstance.type = 'inputConfirm';
    modalRef.componentInstance.placeholder = placeholder ?? 'delete';


    if (options){
      modalRef.componentInstance.accept = options.accept;
      modalRef.componentInstance.cancel = options.cancel;
    }

    return from(modalRef.result).pipe(
      map(result => {
        if (result==null) throw Error("InputConfirm alert closed without argument")
        return result;
      }), // when closed
      catchError(reason => of(reason ?? false)) // when dismissed
    );
  }
}
