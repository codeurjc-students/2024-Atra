import { UserService } from './../../services/user.service';
import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormGroup, FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';

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
    protected activeModal: NgbActiveModal
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
      alert("Something went wrong, the form is not valid")
      return
    }
    this.activeModal.close();
    this.userService.login(this.loginForm.get("username")?.value, this.loginForm.get("password")?.value).subscribe({
      next: (response) => {
        console.log('Login successful', response);
        this.router.navigate(['/me/home']);
      },
      error: (error) => {
        console.error('Login failed', error);
        alert('Login failed. Please check your credentials and try again.');
      }
    });
  }

}
