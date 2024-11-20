import { UserService } from './../../services/user.service';
import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormGroup, FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { UserInitComponent } from '../user-init/user-init.component';
//import { ModalDismissReasons, NgbDatepickerModule, NgbModal } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule]
})
export class LoginComponent implements OnInit {
  loginForm!: FormGroup;

  constructor(private fb: FormBuilder, private userService: UserService, private router: Router){}

  ngOnInit(): void {
    this.loginForm = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    }, {
      updateOn: 'blur'
    });
  }

  onSubmit() {
    console.log("a")
    if (this.loginForm.invalid) {
      alert("Something went wrong, the form is not valid")
      return
    }
    console.log("b")
    this.userService.login(this.loginForm.get("username")?.value, this.loginForm.get("password")?.value).subscribe({
      next: (response) => {
        // If the login is successful, navigate to /home
        console.log('Login successful', response);
        //dismissModal();
        this.router.navigate(['/home']);
      },
      error: (error) => {
        // If the login fails, show an error alert
        console.error('Login failed', error);
        //dismissModal();
        alert('Login failed. Please check your credentials and try again.');
      }
    });
  }

}