import { ActivityService } from './../../services/activity.service';
import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Activity } from '../../models/activity.model';
import { FormsModule } from '@angular/forms';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { ActivityStudyComponent } from "../activity-study/activity-study.component";
import L from 'leaflet';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { RouteService } from '../../services/route.service';


@Component({
  selector: 'app-activity',
  standalone: true,
  imports: [CommonModule, FormsModule, NgxChartsModule, ActivityStudyComponent],
  templateUrl: './activity.component.html',
  styleUrl: './activity.component.css'
})
export class ActivityComponent implements OnInit {


  @ViewChild('chartContainer', { static: true }) chartContainer!: ElementRef;

  id !: string;
  activity !: Activity;
  stats !: {name:string, value:string}[];
  map!: L.Map;

  constructor(private route: ActivatedRoute, private router:Router, private activityService: ActivityService, private modalService: NgbModal, private routeService: RouteService) {}

  ngOnInit(): void {
    this.mapSetup()

    const id = this.route.snapshot.paramMap.get("id");
    if (id===null) {
      this.router.navigate(["/error?reason=missingParameter"]);
      return;
    }
    this.id = id;

    this.activityService.get(this.id).subscribe({
      next: (act) => {
        this.activity = this.activityService.process1(act);
        this.stats = this.activity.getOverview()

        this.addPathToMap()
      },
      error: () => {alert("There was an error fetching the activity. Try reloading the page.")}
    })
  }

  mapSetup(){
    const baseMaps = {
      "OSM Standard": L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'),
      "Satellite": L.tileLayer('https://tiles.stadiamaps.com/tiles/alidade_satellite/{z}/{x}/{y}{r}.jpg'),
      "Vector": L.tileLayer('https://tiles.stadiamaps.com/tiles/outdoors/{z}/{x}/{y}{r}.jpg')
    };

    // Initialize the map centered on coordinates
    this.map = L.map('map', {
      layers: [baseMaps["OSM Standard"]]
    })
    // Add control to switch between layers
    L.control.layers(baseMaps).addTo(this.map);
     //end of map things
  }

  addPathToMap() {
    const coordinates = this.activityService.getCoordinates(this.activity)
    const path = L.polyline(coordinates, {
      color: 'blue',         // Line color
      weight: 4,             // Line thickness
      opacity: 0.8,          // Line opacity
    }).addTo(this.map);

    this.map.fitBounds(path.getBounds());
  }

  //#region create route
  modal!: any;

  open(content: TemplateRef<any>) {
    this.modal = this.modalService.open(content)
  }

  createRoute(name:string, desc:string, distance:string, elevation:string) {
    this.routeService.createRoute(name,desc, parseFloat(distance), parseFloat(elevation), this.activity.id)
  }

  //#endregion
}
