import { UserService } from './../../services/user.service';
import { Component, OnInit, TemplateRef } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { User } from '../../models/user.model';
import { AlertService } from '../../services/alert.service';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, NgModel, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent implements OnInit {

  profileModal !: any;
  passwordModal !: any;
  user !: User;
  generalForm !: FormGroup;
  passwordForm !: FormGroup;

  constructor(private modalService:NgbModal, private userService:UserService, private alertService:AlertService, private router:Router, private fb: FormBuilder, private authService:AuthService){
    this.user = {
      id: 1,
      username: "username",
      password: "string",
      name: "name",
      email: "email",
      roles: [""]
    };
  }

  createForms(){
    this.generalForm = this.fb.group({
      username: [this.user.username, { validators: [Validators.required], asyncValidators: [this.userService.isUserNameTaken(this.user.username)], updateOn: 'blur' }],
      name: [this.user.name],
      email: [this.user.email, Validators.email]
    })

    this.passwordForm = this.fb.group({
      oldPassword: ['', Validators.required],
      newPassword: ['', Validators.required],
      confirmPassword: ['', Validators.required],
    }, {
      validators : [this.userService.matchPasswords("newPassword","confirmPassword")],
      updateOn: 'blur'
    });

  }

  ngOnInit(): void {
    this.userService.getCurrentUser().subscribe({
      next:(u:any)=>{
        this.user = u;
        this.createForms()
      },
      error:(e)=>{
        console.error("There's been an error. Current user could not be loaded: ", e);
      }
    })
  }

  open(template: TemplateRef<any>, password?:boolean) {
    const modal = this.modalService.open(template, {backdrop:'static'}); //consider centering. Not doing it now cause it covers more content when centered
    if (password) {
      this.passwordModal = modal
    } else {
      this.profileModal = modal
    }
  }

  saveChanges() {
    //do some shit
    if (!this.generalForm.valid) throw new Error("Invalid form")
    const username = this.generalForm.get('username')?.value
    const name = this.generalForm.get('name')?.value
    const email = this.generalForm.get('email')?.value

    if (this.user.username!==username)
      this.alertService.confirm("Updating your username will log you out. Are you sure you want to continue?", "You're about to be logged out").subscribe({
        next:(accepted)=>{
          if (accepted) {
            this.restOfSaveChanges(username, name, email)
            this.router.navigate(["/"])
          }
        }
      });
    else {
      this.restOfSaveChanges(username, name, email)
    }
  }
  private restOfSaveChanges(username:string, name:string, email:string) {
    this.user.username = username
    this.user.name = name
    this.user.email = email

    this.alertService.loading();
    this.userService.update(this.user).subscribe({
      next:(u:any)=>{
        this.user = u;
        this.generalForm.get('username')?.setAsyncValidators(this.userService.isUserNameTaken(this.user.username));
        this.profileModal.close()
        this.alertService.loaded()
      },
      error:()=>{
        this.alertService.alert("Error updating user info", "Something went wrong", ()=>this.profileModal.close()) //left as alert since it's a direct consequence of user's actions. Not sure why we're closing the modal
        this.alertService.loaded()
      }
    })
  }

  changePassword(){
    const oldPassword = this.passwordForm.get('oldPassword')?.value;
    this.userService.confirmPassword(oldPassword).subscribe({
      next: (isSame:boolean) => {
        console.log("isSame: " + isSame);

        if (isSame) {
          //apparently the user is not automatically logged out when changing password.
          //We can manually log them out with servletRequest.getSession().invalidate(); or maybe SecurityContextHolder.clearContext();
          //or we could just ignore this and not tell the user they'll be logged out or redirect them.
          this.alertService.confirm("This will log you out. Are you sure you want to continue?", "You're about to be logged out").subscribe({
            next:(accepted) => {
              if (accepted) {
                this.userService.updatePassword(this.passwordForm.get('newPassword')?.value).subscribe({
                  next:()=>{this.modalService.dismissAll(); this.authService.logout(); this.router.navigate(["/"])},
                  error:(error:any) => {this.alertService.alert("An error ocurred. Your password could not be changed.", "Something went wrong", this.modalService.dismissAll)} //left as alert since it's a direct consequence of user's actions. I seem to have insisted on closing the modals when errors happen. Not sure why, it's a pain to have to re-enter all the
                })
              } else {
                this.passwordForm.reset()
                this.modalService.dismissAll()
              }
            }
          })
        } else {
          this.alertService.toastError("The password provided does not match your current password", "Wrong Password")
        }
      }
    })
  }

  dismiss(modalName:string) {
    if (modalName==='password') {
      this.passwordModal.dismiss();
      this.passwordForm.reset()
    } else if (modalName==='profile') {
      this.profileModal.dismiss();
      this.generalForm.reset({username:this.user.username, name:this.user.name, email:this.user.email})
    }
  }

  change(v:string) {
    console.log(v);
    console.log(this.generalForm.get('name'));


  }

  downloadSession(){
    this.alertService.alert('Sorry, this functionality is not available yet', 'Functionality not implemented')
  }
  deleteAccount(){
    this.alertService.confirm(
      'You are deleting this account. This action is irreversible.\n'+
      'All your data and activities will be deleted.\nMurals you created will NOT be deleted. A random user will be selected to be the new owner. If you\'d rather select the owner manually, you can do so from the mural page.\n'+
      'Private routes will be deleted, while Mural_Specific ones will be made public. If you don\'t want this to happen, you need to change their visibility to private before deleting your account.\n'+
      'Are you sure you want to continue?', 'Warning! This action is irreversible', {accept:"Yes, I'm sure", cancel:"On second thought, maybe I shouldn't"}
    ).subscribe(
      (accepted:boolean)=>{ if (accepted)
        this.alertService.inputConfirm("Type 'delete' and click 'delete account' to delete your account.", "Deleting Account", {accept:"delete account", cancel:"cancel"}, "delete").subscribe(
          answer=>{
            if (!answer.accept) return this.alertService.alert("The operation was cancelled. Your account is still up.", "Operation cancelled")
            if (answer.text!=='delete') return this.alertService.toastWarning("The text typed does not match 'delete'. The operation was cancelled.", "Operation cancelled")

            this.userService.delete().subscribe({
              next: () => {
                this.alertService.alert("Your account has been deleted successfully. All your activities have been deleted. You have been removed from any Murals you were part of.", "Account deleted")
                this.authService.logout();
                this.router.navigate(["/"])
              },
              error: err => {
                if (err.status==401) this.alertService.alert("You must be signed in to perform this operation. It has not gone through.", "Operation cancelled")
                else if (err.status==403) this.alertService.alert("You are not authorized to perform this operation. It has been cancelled. The account is still up.", "Operation cancelled")
                else if (err.status==404) this.alertService.alert("Could not find the user to be deleted. The operation has been cancelled.", "Operation cancelled")
                else {
                  this.alertService.alert("An unexpected error ocurred, try again later.", "Error")
                  console.error("Unexpected error deleting user account: " + err);
                }
              }
            })
          }
        )
      }
    )
  }

}
