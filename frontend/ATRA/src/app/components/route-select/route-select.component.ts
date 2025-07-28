import { AlertService } from './../../services/alert.service';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, ElementRef, EventEmitter, Input, OnInit, Output, QueryList, ViewChild, ViewChildren } from '@angular/core';
import { RouteService } from '../../services/route.service';
import { NgbPopover, NgbPopoverModule } from '@ng-bootstrap/ng-bootstrap';
import L from 'leaflet';
import { MapService } from '../../services/map.service';
import { Route } from '../../models/route.model';

@Component({
  selector: 'app-route-select',
  standalone: true,
  imports: [CommonModule, NgbPopoverModule],
  templateUrl: './route-select.component.html',
  styleUrl: './route-select.component.scss'
})
export class RouteSelectComponent implements OnInit, AfterViewInit{
  selected: Set<number> = new Set();
  shouldSelectAll: boolean = true;
  urlStart: string = 'me';

  //When used as a main component, loadFrom should be used to have RouteSelect fetch its own activities
  @Input() loadFrom: 'authUser' | 'user' | 'mural' = 'authUser';

  //But when it's used as a selector within another component, it should simply receive the activities to display
  @Input() routes !: Route[];
  @Input() submit: () => void = () => {console.warn("(RouteSelectComponent) No submit function provided");
  ;}
  @Output() emitter = new EventEmitter<Set<number>>();

  @Input() submitText: string = "Submit";
  @Input() showTopRow: boolean = true;

  onSubmit(){
    this.emitter.emit(this.selected)
    this.submit()
  }


  columns: string[] = ['Name', 'Desc', 'Distance', 'Ele'];

  constructor(private router: Router, private alertService:AlertService, private urlRoute:ActivatedRoute, private routeService:RouteService){}


  ngOnInit(): void {
    if (this.routes!=null) return
    //the component itself should show a spinner. Add that in next commit. alertService.loading() is for when the whole page is loading, to stop the user from doing things. Here, just a part is loading, so just that part should show that
    this.loadFrom = this.urlRoute.snapshot.data['loadFrom'];
    if (this.loadFrom=='authUser'){
      //load from authenticated user |remnant from RouteSelectComponent. Left in case it needs to be expanded
    } else if (this.loadFrom=='mural') {
      // load from mural |remnant from RouteSelectComponent. Left in case it needs to be expanded
    } else if (this.loadFrom=='user') {
      //load from specified user |remnant from RouteSelectComponent. Left in case it needs to be expanded
     }
  }

  toggle(id: number) {
    if (this.selected.size===this.routes.length) {this.shouldSelectAll = true}

    this.selected.has(id) ? this.selected.delete(id):this.selected.add(id)

    if (this.selected.size===this.routes.length) {this.shouldSelectAll = false}
  }

  selectAll() {
    console.log(this.routes)
    if (this.shouldSelectAll) {
      this.routes.forEach(route => this.selected.add(route.id));
    } else {
      this.selected.clear();
    }
    this.shouldSelectAll = !this.shouldSelectAll
  }


  getXFromY(X: string, Y: Route) { //Y should be an route
    switch(X.toLowerCase())  {
      case 'id': return Y.id
      case 'name': return Y.name
      case 'desc': return Y.description.substring(0,50) + (Y.description.length > 50 ? '...' : '');
      case 'ele': return Y.elevationGain.toFixed(2) + "m"
      case 'distance': return Math.round(Y.totalDistance*100)/100 + "km"
      default : throw new Error(`Property '${X}' does not exist on object Y.`)
    }
  }

  toHoursMinsSecs(n: number){ //format should be H:MM:SS but this is fine for now
    // Route.formatTime(n) does a similar thing, in a different format
    const hours = Math.floor(n/3600)
    n = n%3600
    const mins = Math.floor(n/60)
    const secs = n%60

    const hoursString = hours != 0 ? hours+"h ":""
    const minsString = mins != 0 ? mins + "m ":""
    const secsString = secs + "s "


    return `${hoursString}${minsString}${secsString}`
  }


  //#region popovers shit
  @ViewChildren('popover') popovers!: QueryList<NgbPopover>;
  @ViewChild('mapContainer') mapContainer!: ElementRef;
  private map?: L.Map;
  path !: L.Polyline;

  ngAfterViewInit() {
    this.popovers.changes.subscribe((newList:QueryList<NgbPopover>)=>{
      newList.forEach(p => {
        var a: Route = this.routes.filter(a=>a.id==p.popoverContext?.routeId)[0]
        p.shown.subscribe(() => {
          this.initMap(a.coordinates)
        });
        p.hidden.subscribe(() => {
          if (this.map) {
            this.map.remove(); // Destroy map instance
            this.map = undefined; // Allow reinitialization
          }
        });
      })

    })
  }

  //togglePopover() {
  //  this.popoverBtn.isOpen() ? this.popoverBtn.close() : this.popoverBtn.open();
  //}

  private initMap(coordinates:[number,number][]) {
    if (this.map || !this.mapContainer) return;

    setTimeout(() => { // Wait for popover to fully render
      this.map = MapService.mapSetup('popoverMap', false, false);
      this.addPathToMap(coordinates)
    });
  }

  addPathToMap(coordinates:[number,number][]) {
    if (this.map==undefined) throw Error("addPathToMap called with undefined map")
    if (this.path!=null) this.path.remove()
    this.path = L.polyline(coordinates, {
      color: 'blue',         // Line color
      weight: 4,             // Line thickness
      opacity: 0.8,          // Line opacity
    }).addTo(this.map);

    this.map.fitBounds(this.path.getBounds());
  }
}
