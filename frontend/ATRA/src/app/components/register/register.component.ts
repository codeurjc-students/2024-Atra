import { User } from './../../models/user.model';
import { CommonModule } from '@angular/common';
import { HttpResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { AbstractControl, AsyncValidatorFn, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { map, of } from 'rxjs';
import { UserService } from '../../services/user.service';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';


@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrl: './register.component.css',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule]
})
export class RegisterComponent implements OnInit{

  step: 'optional' | 'required' = 'required';

  nextDisabled: boolean = true;
  registerForm!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    //private http:HttpClient,
    //private router: Router,
    protected activeModal: NgbActiveModal
  ) {}

  ngOnInit(): void {
    this.registerForm = this.fb.group({
      username: ['', { validators: [Validators.required], asyncValidators: [this.isUserNameTaken()], updateOn: 'blur' }],
      password: ['', Validators.required],
      confirm: ['', Validators.required],

      displayname: [''],
      email: ['', [Validators.email]]
    }, {
      validators : [this.matchPasswords()],
      updateOn: 'blur'
    });
  }

  isUserNameTaken(): AsyncValidatorFn {
    return (control: AbstractControl) => {
      const username = control.value;
      //if (username===this.currentUser.username) return of(null);
      return this.userService.isUsernameTaken(username).pipe(
        map((isTaken: boolean) => {return isTaken ? { usernameTaken: true } : null})
      )
    }
  }

  matchPasswords(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const password = control.get("password")?.value;
      const confirmPassword = control.get("confirm")?.value;

      if (!control.get("password")?.touched && !control.get("confirm")?.touched) return null;

      if (password !== confirmPassword) {
        //alert("Passwords do not match");
        return {differentPasswords:true};
      }
      return null;
    }
  }

  next() {
    if (this.requiredArePresentAndValid()){
      this.step = 'optional';
    }
    else {
      alert("Debes completar todos los campos antes de continuar")
    }
  }

  previous() {
    this.step = 'required';
  }

  requiredArePresentAndValid(): boolean {
     const arePresent = this.registerForm?.get("username")?.value && this.registerForm?.get("password")?.value && this.registerForm?.get("confirm")?.value;
     console.log("Are Present: " + arePresent)
     console.log("Are Valid: " + this.registerForm.valid)
    return arePresent && this.registerForm.valid;
  }

  onSubmit() {
    if (this.registerForm.invalid) {
      alert("Something went wrong, the form is not valid")
      return
    }

    this.userService.createUser(this.collectUser());
  }

  collectUser(): User {
    const username = this.registerForm.get('username')?.value
    const password = this.registerForm.get('password')?.value
    const email = this.registerForm.get('email')?.value
    var displayname = this.registerForm.get('displayname')?.value
    return {
      id: 0,
      username: username,
      password: password,
      displayname: (displayname == "") ? username:displayname,
      email: email,
      roles: []
    };
  }
}

/*
  registerForm: FormGroup;
  isLoading = false;
  submitButtonEnabled: boolean = false;
  imgUrl = "./assets/logo.png";

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private http:HttpClient,
    private router: Router,
  ) {
    this.registerForm = this.fb.group({
      fullName: ['', Validators.required],
      username: ['', { validators: [Validators.required], asyncValidators: [this.userNameTaken()], updateOn: 'blur' }],
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
      confirmPassword: ['', Validators.required]
    }, {
      validators : [this.matchPasswords()],
      updateOn: 'blur'
    });
  }

  onSubmit() {
    //send a login request to /api/auth/login
    const fullName: string = this.registerForm.get("fullName")?.value;
    const username: string = this.registerForm.get("username")?.value;
    const email: string = this.registerForm.get("email")?.value;
    const password: string = this.registerForm.get("password")?.value;
    this.http.post("/api/users",
      {
        name:fullName,
        username: username,
        email: email,
        password: password
      }).subscribe({
        next: (response) => {
          this.router.navigate(["/login"])
        },
        error: (error) => {
          this.router.navigate(["/error"])
        }
      }
    );
  }

  userNameTaken(): AsyncValidatorFn {
    return (control: AbstractControl) => {
      const userName = control.value;
      return this.userService.usernameTaken(userName).pipe(
        map(
          (isTaken: HttpResponse<any>) => {
            if (isTaken) {
              //alert("UserName is taken. Try a different one");
              this.submitButtonOnClick = this.warnNameTaken;
              return {userNameTaken:true};
            } else {
              this.submitButtonOnClick = this.validateForm;
              return null
            }
          }
        )
      )
    }
  }

  warnNameTaken() {
    alert("Can't create an account with that nickname. Try a different one.");
    return false;
  }
  validateForm() {
    return true;
  }

  matchPasswords(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const password = control.get("password")?.value;
      const confirmPassword = control.get("confirmPassword")?.value;
      if (password !== confirmPassword) {
        //alert("Passwords do not match");
        return {passwordsMatch:true};
      }
      return null;
    }
  }

  submitButtonOnClick: () => boolean = this.validateForm;
*/

