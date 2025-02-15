import { GraphService } from '../../services/graph.service';
import { ActivityStreams } from './../../models/activity-streams.model';
import { ActivityService } from './../../services/activity.service';
import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, ElementRef, HostListener, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Activity } from '../../models/activity.model';
import { FormsModule, ValidationErrors } from '@angular/forms';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';


@Component({
  selector: 'app-activity',
  standalone: true,
  imports: [CommonModule, FormsModule, NgxChartsModule],
  templateUrl: './activity.component.html',
  styleUrl: './activity.component.css'
})
export class ActivityComponent implements OnInit {

  @ViewChild('chartContainer', { static: true }) chartContainer!: ElementRef;

  modal !: any;
  id !: string;
  activity !: Activity;
  view: [number, number] = [400, 400];

  //Chart selection
  charts: Record<string, boolean> = {"line":true, "histogram":false, "scatter":false, "boxplot":false};
  chartsKeys = Object.keys(this.charts);
  selectedChart: string = this.chartsKeys[0];

  //Chart Manipulation
  xAxisRepresents: string = "timeElapsed";
  extrasSet: Set<string> = new Set();

  //Data
  stats !: {name:string, value:string}[];
  dataset : {name:string, value:number}[] = [];
  displayData: {name:string, series:{name:string, value:number}[]} [] | { name: string; value: number }[] = [];

  //Metrics
  metrics: string[] = ActivityStreams.getGraphableKeys();
  selectedMetric = this.metrics[0];

  extrasValues = {
    goal:630,
    upperLimit:670,
    lowerLimit:600
  }
  ratings = [
    { name: 'Acceptable Time', value: '90%' },
    { name: 'Below Avg Time', value: '48%' },
    { name: 'Above Avg Time', value: '55%' },
    { name: 'σ', value: this.graphService.getDeviation(this.dataset.map(d => d.value)) }
  ];

  constructor(private route: ActivatedRoute, private router:Router, private activityService: ActivityService, private modalService: NgbModal, private graphService:GraphService) {}

  open(content: TemplateRef<any>) {
    this.modal = this.modalService.open(content)
  }

  @HostListener('window:resize', ['$event'])
  ngAfterViewInit(): void {
    const containerWidth = this.chartContainer.nativeElement.offsetWidth;
    const containerHeight = this.chartContainer.nativeElement.offsetHeight;
    if (containerHeight == 0 || containerWidth == 0) {
       this.view = [800, 375]
       return
    }
    this.view = [containerWidth*0.8, containerHeight*0.8];
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get("id");
    if (id===null) {
      this.router.navigate(["/error?reason=missingParameter"]);
      return;
    }
    this.id = id;

    this.activityService.get(this.id).subscribe({
      next: (act) => {
        this.activity = this.activityService.process([act])[0];
        this.stats = this.activity.getOverview()
        this.updateChart()
      },
      error: () => {alert("There was an error fetching the activity. Try reloading the page.")}
    })
  }

  //#region Chart Lifecycle
  updateChart() {
    console.log(this.selectedMetric)
    this.displayData = []
    this.dataset = this.graphService.getGraphData(this.selectedMetric, this.activity, this.xAxisRepresents, 20)
    this.displayData = this.graphService.getDisplayData(this.dataset, this.selectedMetric, this.selectedChart)
    this.pushExtras()
    this.updateRatings()
  }
  pushExtras(){
    console.log("Extras is ", this.extrasSet)
    //const xAxis = this.activityService.getMetric(this.xAxisRepresents, this.activity)
    //if (this.extrasSet.has("goal")){
    //  this.displayData.push({'name':"goal", 'series':this.LineAt(xAxis, this.extrasValues.goal)}) //This can be done iterating through the set and with this.extrasValues[currentVar]
    //}
    //if (this.extrasSet.has("upperLimit")){
    //  console.log("Inside upperLimit")
    //  this.displayData.push({'name':"Upper Limit", 'series':this.LineAt(xAxis, this.extrasValues.upperLimit)})
    //}
    //if (this.extrasSet.has("lowerLimit")){
    //  console.log("Inside lowerLimit")
    //  this.displayData.push({'name':"Lower Limit", 'series':this.LineAt(xAxis, this.extrasValues.lowerLimit)})
    //}
  }
  updateRatings(){
    for (let tier of this.ratings) {
     tier.value = this.graphService.calc(tier.name, this.dataset.map(d=>d.value), tier.value)
    }
  }
  private LineAt(xAxis: any[], value: number): { name: string; value: number; }[] {
    const result: { name: string; value: number; }[] = []
    for (let x of xAxis) {
      result.push({name:x, value:value})
    }
    return result
  }
  //#endregion

  //#region onChange
  changeChart(newChart:string) {
    for (let key of this.chartsKeys) {
      this.charts[key] = false
    }
    this.charts[newChart] = true
    this.xAxisRepresents = "distribution"
    this.updateChart()
  }

  updateExtras(extra: string) {
    if (this.extrasSet.has(extra))
      this.extrasSet.delete(extra)
    else
      this.extrasSet.add(extra)
    this.updateChart()
  }
  //#endregion

}
