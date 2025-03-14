import { Component, OnInit } from '@angular/core';
import { NavigationEnd, Router, RouterModule, RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ActivityService } from './services/activity.service';
import { AlertService } from './services/alert.service';


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

  constructor(private router: Router, private activityService: ActivityService, private alertService: AlertService){}

  ngOnInit(){
    this.router.events.subscribe(
      (event) => {
        if (event instanceof NavigationEnd) {
          this.showSideBar = this.router.url !== "/"
          this.urlStart = '/' + this.router.url.split("/")[1]
          if (this.urlStart.split('?')[0]=="/error") {
            this.urlStart = "/me"}
        }
      }
    )
  }

  isPrivateRoute(){return this.router.url.startsWith("/me/")}
  isMuralRoute(){return this.router.url.startsWith("/mural/")}
  uploadFile(event: Event) {
    this.activityService.uploadActivity(event)
  }

  linkWithStrava() {
    this.alertService.alert("This function is yet to be implemented")}
}
