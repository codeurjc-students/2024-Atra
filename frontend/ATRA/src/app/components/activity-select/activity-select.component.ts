import { AlertService } from './../../services/alert.service';
import { Activity } from './../../models/activity.model';
import { ActivityService } from './../../services/activity.service';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, ElementRef, EventEmitter, Input, OnInit, Output, QueryList, SimpleChanges, ViewChild, ViewChildren } from '@angular/core';
import { NgbPopover, NgbPopoverModule } from '@ng-bootstrap/ng-bootstrap';
import L from 'leaflet';
import { MapService } from '../../services/map.service';
import { FormattingService } from '../../services/formatting.service';
import { HttpResponse } from '@angular/common/http';
import { Expansion } from '@angular/compiler';

@Component({
  selector: 'app-activity-select',
  standalone: true,
  imports: [CommonModule, NgbPopoverModule],
  templateUrl: './activity-select.component.html',
  styleUrl: './activity-select.component.scss'
})
export class ActivitySelectComponent implements OnInit, AfterViewInit{
  selected: Set<number> = new Set();
  shouldSelectAll: boolean = true;
  urlStart: string = 'me';

  @Input() isChild:boolean = false

  //When used as a main component, loadFrom should be used to have ActivitySelect fetch its own activities
  @Input() loadFrom: 'authUser' | 'user' | 'mural' = 'authUser';

  //But when it's used as a selector within another component, it should simply receive the activities to display
  @Input() activities !: Activity[];
  shownActivities !: Activity[];
  @Input() submit: () => void = () => this.defaultSubmit();
  @Output() emitter = new EventEmitter<Set<number>>();

  @Input() submitText: string = "Submit";
  @Input() showTopRow: boolean = true;

  onSubmit(){
    this.emitter.emit(this.selected)
    this.submit()
  }


  columns: string[] = ['Name', 'Date', 'Route', 'Time', 'Distance'];
  @Input() loading: boolean = false;

  constructor(private router: Router, private activityService: ActivityService, private alertService:AlertService, private urlRoute:ActivatedRoute){}


  ngOnInit(): void {

    if (this.isChild || this.activities!=null) return
    this.activities = [];
    this.loadFrom = this.urlRoute.snapshot.data['loadFrom'];
    this.loadActivities(false, 0, 2)
  }

  loadActivities(fetchAll:boolean, startPage:number, pagesToFetch:number=1){
    this.loading=true;

    if (this.loadFrom=='authUser')
      this.activityService.getAuthenticatedUserActivities(fetchAll, startPage, pagesToFetch, this.pageSize).subscribe({
        next: (response:HttpResponse<any[]>) => {this.activitiesReceived(response)},
        error: (err) => {this.loading=false;this.alertService.toastError("There was an error fetching your activities"); console.log("There was an error fetching the user's activities", err)}
      })
    else if (this.loadFrom=='mural') {
      const id = this.urlRoute.snapshot.paramMap.get('id');
      if (id==null) {
        this.alertService.toastError("Something went wrong, try reloading the page");
        console.error("(ActivitySelectComponent) Trying to load activities from mural but couldn't find its id in the paramMap");
        this.loading = false;
        return
      }
      this.urlStart = "murals/"+id
      this.activityService.getMuralActivities(id, fetchAll, startPage, pagesToFetch, this.pageSize).subscribe({
        next: (response:HttpResponse<any[]>) => {this.activitiesReceived(response)},
        error: (err) => {this.loading=false;this.alertService.toastError("There was an error fetching your activities"); console.log("There was an error fetching the user's activities", err)}
      })
    } else if (this.loadFrom=='user') {
      this.loading=false;
      throw new Error("Not implemented yet, as there's no real need for it")
    } else {
      this.loading=false;
      throw new Error("Invalid loadFrom value " + this.loadFrom);
    }
    this.loading=false;

  }

  activitiesReceived(response:HttpResponse<any[]>) {
    this.loading = false;
    this.activities.push(...this.activityService.process(response.body ?? []));

    this.total=Number(response.headers.get("ATRA-Total-Entries"))
    const lastPageTemp = Number(response.headers.get("ATRA-Total-Pages"))-1
    this.lastPage= lastPageTemp>0 ? lastPageTemp:this.total/this.pageSize

    //this.currentPage=Number(response.headers.get("ATRA-Start-Page"))
    const startOfCurrentQuery = Number(response.headers.get("ATRA-Start-Page"))

    if (startOfCurrentQuery==this.lastLoadedPage+1) //to only count new entries
      this.entriesLoaded = this.entriesLoaded+Number(response.headers.get("ATRA-Entries-Sent")) //if info already fetched is refetched this will be incorrect
    this.lastLoadedPage = Math.max(startOfCurrentQuery + Number(response.headers.get("ATRA-Pages-Sent")??Number.MAX_SAFE_INTEGER)-1,this.lastLoadedPage) //in case already loaded pages are reloaded. Though we shouldn't do that

    console.log("(ActivitySelectComponent) Activities loaded. Metadata received is: ", {totalEntries:this.total, currentPage:this.currentPage, lastPage:this.lastPage, entriesLoaded:this.entriesLoaded, lastLoadedPage:this.lastLoadedPage});

    this.reloadActivityWindow()
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['activities'] && this.activities) {
      this.activitiesInherited();
    }
  }
  activitiesInherited() {

    this.loading = false;
    this.total = this.activities.length
    this.lastPage = Math.ceil(this.total/this.pageSize)-1
    this.entriesLoaded = this.activities.length
    this.lastLoadedPage = Number.MAX_SAFE_INTEGER
    this.currentPage=0;

    this.minShown=0
    this.maxShown=this.pageSize
    this.minShownHTML=0

    this.shownActivities = this.activities.slice(0, this.pageSize);

    this.minShownHTML=this.shownActivities?.length?1:0
    this.maxShownHTML=Math.min(this.pageSize, this.shownActivities?.length??0)

  }

  toggle(id: number) {
    if (this.selected.size===this.activities.length) {this.shouldSelectAll = true}

    this.selected.has(id) ? this.selected.delete(id):this.selected.add(id)

    if (this.selected.size===this.activities.length) {this.shouldSelectAll = false}
  }

  selectAll() {
    if (this.shouldSelectAll) {
      this.activities.forEach(activity => this.selected.add(activity.id));
    } else {
      this.selected.clear();
    }
    this.shouldSelectAll = !this.shouldSelectAll
  }


  getXFromY(X: string, Y: Activity) { //Y should be an activity
    if (Y.summary==null) throw new Error("Activity summary is null, cannot get X from Y");
    switch(X.toLowerCase())  {
      case 'id': return Y.id
      case 'name': return Y.name
      case 'date': return Y.startTime.toISOString().split("T")[0]
      case 'time': return FormattingService.toHoursMinsSecs(Y.summary.totalTime)
      case 'route': return Y.route!=null ? Y.route.name : Y.route
      case 'distance': return Math.round(Y.summary.totalDistance*100)/100 + "km"
      case 'other' : return Y.other
      default : throw new Error(`Property '${X}' does not exist on object Y.`)
    }
  }

  defaultSubmit(){
    const selectedActs = Array.from(this.selected);
    if (this.selected.size === 0 ) { this.alertService.alert("You must select at least one activity") }
    else if (this.selected.size === 1) {
      this.router.navigate([this.urlStart, "activities", selectedActs[0]])
    } else {
      this.router.navigate([this.urlStart, "activities", "compare", `${selectedActs.join("-")}`])
      //this.alertService.alert("Sorry, for now you can select no more than 2 elements. We are working on expanding this feature.")
    }
  }

  //#region popovers shit
  @ViewChildren('popover') popovers!: QueryList<NgbPopover>;
  @ViewChild('mapContainer') mapContainer!: ElementRef;
  private map?: L.Map;
  path !: L.Polyline;

  ngAfterViewInit() {
    this.popovers.changes.subscribe((newList:QueryList<NgbPopover>)=>{
      newList.forEach(p => {
        var a: Activity = this.activities.filter(a=>a.id==p.popoverContext?.activityId)[0]
        p.shown.subscribe(() => {
          this.initMap(this.activityService.getCoordinates(a))
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
      this.map = MapService.mapSetup('popoverMapActivities', false, false);
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


  //#region Pagination
  minShown: number = 0;
  maxShown: number = 0;
  minShownHTML: number = 0;
  maxShownHTML: number = 0;
  total: number = 0;
  currentPage: number = 0;
  lastPage: number = 0;
  @Input() pageSize: number = 5;
  entriesLoaded: number = 0;
  lastLoadedPage:number = 0;
  goPrevious(){
    if (this.currentPage > 0) {
      this.currentPage--;
      this.reloadActivityWindow()
    }
  }
  goNext(){
    if (this.currentPage < this.lastPage) {
      this.currentPage++;
      this.reloadActivityWindow()
      this.loadNextPage()
    }
  }

  reloadActivityWindow() {
  this.minShown = this.currentPage * this.pageSize;
  this.maxShown = Math.min((this.currentPage+1) * this.pageSize, this.total);
  this.shownActivities = this.activities.slice(this.minShown, this.maxShown);

  this.minShownHTML = this.minShown==0?0:this.minShown+1
  this.maxShownHTML = this.maxShown
  }

  loadNextPage() {
    console.log({lastLoadedPage:this.lastLoadedPage, lastPage:this.lastPage,currentPage:this.currentPage});
    console.log({cond1:this.lastLoadedPage<this.lastPage,cond2:this.currentPage+2<this.lastLoadedPage});


    if (this.lastLoadedPage<this.lastPage && this.currentPage+1>this.lastLoadedPage) {
      console.log("A")
      this.loadActivities(false, this.lastLoadedPage+1, 1)
    }
  }

  //#endregion


}
