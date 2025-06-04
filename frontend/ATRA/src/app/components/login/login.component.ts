import { AlertService } from './../../services/alert.service';
import { UserService } from './../../services/user.service';
import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormGroup, FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NgbActiveModal, NgbModal, NgbModalModule } from '@ng-bootstrap/ng-bootstrap';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule]
})
export class LoginComponent implements OnInit {
  loginForm!: FormGroup;


  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private router: Router,
    protected activeModal: NgbActiveModal,
    private alertService: AlertService,
    private authService: AuthService
  ){}

  ngOnInit(): void {
    this.loginForm = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    }, {
      updateOn: 'blur'
    });
  }

  onSubmit() {
    if (this.loginForm.invalid) {
      this.alertService.alert("Something went wrong, the form is not valid")
      return
    }
    this.activeModal.close();
    this.authService.login(this.loginForm.get("username")?.value, this.loginForm.get("password")?.value).subscribe({
      next: (response) => {
        console.log('Login successful', response);
        const redirect = this.authService.getRedirectUrl();
        if (redirect) {
          this.router.navigate([redirect]);
        } else {
          this.router.navigate(['/me/home']);
        }
      },
      error: (error) => {
        console.error('Login failed', error);
        this.alertService.alert('Login failed. Please check your credentials and try again.');
      }
    });
  }

}
