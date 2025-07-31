import { AlertService } from './../../services/alert.service';
import { User } from './../../models/user.model';
import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { UserService } from '../../services/user.service';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';


@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss',
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
      this.alertService.toastWarning("Debes completar todos los campos antes de continuar")
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
      this.alertService.toastError("Something went wrong, the form is not valid", "This shouldn't happen")
      return
    }

    this.userService.createUser(this.collectUser()).subscribe({
      next: () => {
        this.activeModal.close()
        this.alertService.toastInfo("You can proceed with the login","Account created successfully")
      },
      error: (e) => this.alertService.toastError("Try again later", "Error creating your account")
    });
  }

  collectUser(): User {
    const username = this.registerForm.get('username')?.value
    const password = this.registerForm.get('password')?.value
    const email = this.registerForm.get('email')?.value
    var name = this.registerForm.get('name')?.value
    return {
      id: 0,
      username: username,
      password: password,
      name: (name == "") ? username:name,
      email: email,
      roles: []
    };
  }
}
