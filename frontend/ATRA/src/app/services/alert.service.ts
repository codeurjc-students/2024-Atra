import { Injectable } from '@angular/core';
import { NgbModal, NgbModalOptions, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { AlertComponent } from '../components/alert/alert.component';
import { catchError, from, map, Observable, of } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

@Injectable({
  providedIn: 'root'
})
export class AlertService {

  confirmModalRef: NgbModalRef | null = null;
  alerts: NgbModalRef[] = [];
  //if alert("a"); alert("b") is called, b will be on top of a. This means the user could be reading messages on inverse order.
  //I deem it not a big deal, and so I won't implement it. If it happens to be important, it can be done by waiting to open until modalRef.close resolves in the open alert.

  loadingModalRef: NgbModalRef | null = null;

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
  alert(message: string, title?: string, onDismiss?:()=>void, easyDismiss:boolean=true) {
    //check comment on this.alerts
    if (this.confirm!=null) console.warn("alert called with open confirm");
    title = title ?? "Warning";
    for (let element of this.alerts) {
      if (element.componentInstance.title==title && element.componentInstance.message==message) {
        element.componentInstance.times += 1;
        //onDismiss won't happen multiple times, but that's probably a good thing
        return;
      }
    }

    var options;
    if (easyDismiss) {
      options = { keyboard: true, centered:true, windowClass:'remove-modal-background' };
    } else {
      options = { backdrop:'static', keyboard: false, centered:true, windowClass:'remove-modal-background' };
    }
    const modalRef = this.modalService.open(AlertComponent, options as NgbModalOptions);
    modalRef.componentInstance.title = title;
    modalRef.componentInstance.message = message;
    modalRef.componentInstance.type = 'alert';
    this.alerts.push(modalRef)

    from(modalRef.dismissed).subscribe(()=>this.alerts = this.alerts.filter(item=>item!==modalRef))
    from(modalRef.closed).subscribe(()=>this.alerts = this.alerts.filter(item=>item!==modalRef)) //it is likely that it can't be closed unless something goes wrong, but better safe

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
    if (this.alerts.length!=0) console.warn("confirm called with open alerts");

    if (this.confirmModalRef!=null) {
      title = title ?? "Warning";
      if (this.confirmModalRef.componentInstance.title==title && this.confirmModalRef.componentInstance.message==message) {
        this.confirmModalRef.componentInstance.times += 1;
        console.error("Opened the same confirm multiple times. This should not be done. ");
        this.toastError("Attempted to open the same confirm multiple times")
      } else {
        console.error("Attempted to open a confirm while one is already open. This is not allowed.");
        this.toastError("A confirm modal is already open", "Can't open the confirm")
      }
      throw Error("Attempted to open a confirm while one is already open. This is not allowed");
    }

    this.confirmModalRef = this.modalService.open(AlertComponent, { backdrop: 'static', keyboard: false, centered:true, windowClass:'remove-modal-background' });
    this.confirmModalRef.componentInstance.title = title ?? "Warning";
    this.confirmModalRef.componentInstance.message = message;
    this.confirmModalRef.componentInstance.type = 'confirm';
    if (options){
      this.confirmModalRef.componentInstance.accept = options.accept;
      this.confirmModalRef.componentInstance.cancel = options.cancel;
    }
    from(this.confirmModalRef.dismissed).subscribe(()=>this.confirmModalRef = null)
    from(this.confirmModalRef.closed).subscribe(()=>this.confirmModalRef = null) //it is likely that it can't be closed unless something goes wrong, but better safe

    return from(this.confirmModalRef.result).pipe(
      map(result => result ?? true), // when closed
      catchError(reason => of(reason ?? false)) // when dismissed
    );
  }

  loading(isLight:boolean=true){
    this.loadingModalRef = this.modalService.open(AlertComponent, { backdrop: 'static', keyboard: false, centered:true, windowClass:'remove-modal-background', animation:false });
    this.loadingModalRef.componentInstance.title = "Loading...";
    this.loadingModalRef.componentInstance.message = "Wait a second while we take care of some things\n This message should disappear shortly. If it doesn't, try reloading the page.";
    this.loadingModalRef.componentInstance.type = isLight ? 'loading-light':'loading-heavy';
  }

  loaded() {
    if (this.loadingModalRef==null) {
      console.error("loaded called with no open modal");
      return
    }
    this.loadingModalRef.close();
    this.loadingModalRef = null;
  }

  // Confirm method (returns a promise resolving to true/false)
  inputConfirm(message: string, title?: string, options?:{accept:string, cancel:string}, placeholder?:string): Observable<{accept:boolean, text:string}> {

    if (this.confirmModalRef!=null) {
      console.error("Attempted to open an input confirm while one is already open. This is not allowed.");
      this.toastError("An input confirm modal is already open", "Can't open the input confirm")
      throw Error("Attempted to open an input confirm while one is already open. This is not allowed");
    }

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
