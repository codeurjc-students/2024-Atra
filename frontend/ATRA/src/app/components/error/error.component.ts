import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Location } from '@angular/common';


@Component({
  selector: 'app-error',
  standalone: true,
  imports: [],
  templateUrl: './error.component.html',
  styleUrl: './error.component.scss'
})
export class ErrorComponent {

  errorMessages: { [key: string]: { code:string, title: string; message: string } } = {
    '400': { code:"400", title: "Bad Request", message: "The server couldn't understand your request due to invalid syntax or missing information." },
    '403': { code:"403", title: "Forbidden", message: "You don't have permission to access this page." },
    '404': { code:"404", title: "Page Not Found", message: "The page you're looking for doesn't exist." },
    '500': { code:"500", title: "Server Error", message: "Something went wrong on our end." },
    '000': { code:"000", title: "Oops!", message: "An unexpected error occurred." },
    'default': { code:"000", title: "Oops!", message: "An unexpected error occurred." }
  };

  errorCode: string = 'default';

  constructor(private route: ActivatedRoute, private location: Location, private router:Router) {}

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.errorCode = params['code'] || 'default';
    });
  }

  goBack(): void {
    this.location.back(); // Goes back home
  }

  goHome(): void {
    this.router.navigate(["/me/home"]); // Goes to the previous page
  }


}
