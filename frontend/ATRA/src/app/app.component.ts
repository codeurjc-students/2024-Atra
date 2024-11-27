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

  constructor(private router: Router){}

  ngOnInit(){
    this.router.events.subscribe(
      (event) => {
        if (event instanceof NavigationEnd) {
          console.log(this.router.url)
          this.showSideBar = this.router.url === "/"
        }
      }
    )
  }


}
