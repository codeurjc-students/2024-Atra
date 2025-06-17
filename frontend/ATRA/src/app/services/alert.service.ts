import { Injectable } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { AlertComponent } from '../components/alert/alert.component';
import { catchError, from, map, Observable, of } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

@Injectable({
  providedIn: 'root'
})
export class AlertService {

  modalRef: any;

  constructor(private modalService: NgbModal, private toastService: ToastrService) {}

  toast(msg:string, title?:string, type:MsgType='info') {
    switch (type) {
      case 'success': this.toastService.success(msg, title); break;
      case 'info': this.toastService.info(msg, title); break;
      case 'warning': this.toastService.warning(msg, title); break;
      case 'error': this.toastService.error(msg, title); break;
    }
  }

  //for convenience
  toastError(msg:string, title?:string) {
    this.toastService.error(msg, title);
  }
  toastWarning(msg:string, title?:string) {
    this.toastService.warning(msg, title);
  }
  toastSuccess(msg:string, title?:string) {
    this.toastService.success(msg, title);
  }
  toastInfo(msg:string, title?:string) {
    this.toastService.info(msg, title);
  }

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

  loading(isLight:boolean=true){
    this.modalRef = this.modalService.open(AlertComponent, { backdrop: 'static', keyboard: false, centered:true, windowClass:'remove-modal-background' });
    this.modalRef.componentInstance.title = "Loading...";
    this.modalRef.componentInstance.message = "Wait a second while we take care of some things\n This message should disappear shortly. If it doesn't, try reloading the page.";
    this.modalRef.componentInstance.type = isLight ? 'loading-light':'loading-heavy';
  }

  loaded() {
    if (this.modalRef==null) console.error("loaded called with no open modal");
    this.modalRef.close();
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

type MsgType = 'info' | 'warning' | 'success' | 'error'
