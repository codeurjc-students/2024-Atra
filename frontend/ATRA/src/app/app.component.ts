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
          console.log("routerURL: " + this.router.url);

          this.urlStart = '/' + this.router.url.split("/")[1]
          console.log("urlStart: " + this.urlStart);

          if (this.urlStart.split('?')[0]=="/error") { //what does this do, why is this here?
            this.urlStart = "/me"
          }
          if (this.urlStart=="/murals") {
            const id = this.router.url.split("/")[2];
            if (id!=null && !isNaN(Number(id))) {
              this.urlStart = "/murals/" + id;
            }
          }
          console.log("url start: " + this.urlStart);

        }
      }
    )
  }

  isProfileRoute(){return this.router.url==("/me")}
  isPrivateRoute(){return this.router.url.startsWith("/me/")}
  isMuralRoute(){return this.location.path().startsWith("/murals")}
  isMuralRouteSelected() {return this.location.path().startsWith("/murals/") && this.location.path().split("/").length>3}
  isMuralRouteCategory() {return this.location.path().startsWith("/murals/") && this.location.path().split("/").length==3}
  isRouteActivities() {
    const p = this.location.path().split("/")
    return p.length==3 && p[2]=="activities"
  }
  isRouteStudy() {
    const p = this.location.path().split("/")
    return p.length>3 && !Number.isNaN(Number(p[3]));
  }
  isRouteCompare() {
    const p = this.location.path().split("/")
    return p.length>3 && p[3]=="compare"
  }

  uploadFile(event: Event) {
    this.activityService.uploadActivity(event)
  }

  linkWithStrava() {
    this.alertService.alert("This function is yet to be implemented")
  }
}
