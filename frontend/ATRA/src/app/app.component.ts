import { Component, OnInit } from '@angular/core';
import { NavigationEnd, Router, RouterModule, RouterOutlet } from '@angular/router';
import { RegisterComponent } from "./components/register/register.component";
import { CommonModule } from '@angular/common';


@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, CommonModule, RouterModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit{

  title = 'ATRA';
  showSideBar: boolean = true;
  urlStart: string = '/me';

  constructor(private router: Router){}

  ngOnInit(){
    this.router.events.subscribe(
      (event) => {
        if (event instanceof NavigationEnd) {
          this.showSideBar = this.router.url !== "/"
          this.urlStart = '/' + this.router.url.split("/")[1]

        }
      }
    )
  }

  isPrivateRoute(){return this.router.url.startsWith("/me/")}
  isMuralRoute(){return this.router.url.startsWith("/mural/")}
}
