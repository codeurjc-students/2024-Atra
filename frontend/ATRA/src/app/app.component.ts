import { Component, OnInit } from '@angular/core';
import { NavigationEnd, Router, RouterModule, RouterOutlet } from '@angular/router';
import { CommonModule, Location } from '@angular/common';
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

  constructor(private location:Location, private router: Router, private activityService: ActivityService, private alertService: AlertService){}

  ngOnInit(){
    //disable scroll
    document.body.style.overflow = 'hidden';
    document.documentElement.style.overflow = 'hidden';


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

  isProfileRoute(){return this.router.url==("/me")}
  isPrivateRoute(){return this.router.url.startsWith("/me/")}
  isMuralRoute(){return this.location.path().startsWith("/murals")}
  isMuralRouteSelected() {return this.location.path().startsWith("/murals/") && this.location.path().split("/").length>3}
  isMuralRouteCategory() {return this.location.path().startsWith("/murals/") && this.location.path().split("/").length==3}

  uploadFile(event: Event) {
    this.activityService.uploadActivity(event)
  }

  linkWithStrava() {
    this.alertService.alert("This function is yet to be implemented")
  }
}
