import { AlertService } from './../../services/alert.service';
import { User } from './../../models/user.model';
import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { AbstractControl, AsyncValidatorFn, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { map } from 'rxjs';
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
    protected activeModal: NgbActiveModal,
    private alertService:AlertService
  ) {}

  ngOnInit(): void {
    this.registerForm = this.fb.group({
      username: ['', { validators: [Validators.required], asyncValidators: [this.userService.isUserNameTaken()], updateOn: 'blur' }],
      password: ['', Validators.required],
      confirm: ['', Validators.required],

      email: ['', [Validators.email]]
    }, {
      validators : [this.userService.matchPasswords("password","confirm")],
      updateOn: 'blur'
    });
  }

  next() {
    if (this.requiredArePresentAndValid()){
      this.step = 'optional';
    }
    else {
      this.alertService.alert("Debes completar todos los campos antes de continuar")
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
      this.alertService.alert("Something went wrong, the form is not valid")
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
