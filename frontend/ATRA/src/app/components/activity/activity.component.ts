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
  charts: Record<string, boolean> = {"line":true, "histogram":false}; //"scatter":false, "boxplot":false
  chartsKeys = Object.keys(this.charts);
  selectedChart: string = this.chartsKeys[0];

  //Chart Manipulation
  xAxisRepresents: string = "timeElapsed";
  extrasSet: Set<string> = new Set();
  referenceLines: {name:string, value:number}[] = [{name:'median',value:640}]
  yAxisTickFormat: (value: number) => string = (value) => value.toString()

  //Data
  stats !: {name:string, value:string}[];
  dataset : {name:string, value:number}[] = [];
  displayData: {name:string, series:{name:string, value:number}[]} [] | { name: string; value: number }[] = [];
  partitionNum: number= 5

  //Metrics
  metrics: string[] = ActivityStreams.getGraphableKeys();
  selectedMetric = this.metrics[0];

  extrasValues = {
    goal:0,
    upperLimit:1,
    lowerLimit:-1
  }
  ratings = [
    { name: '25th percentile', value: -1},
    {name: 'IQR', value: -1},
    { name: '50th percentile', value: -1},
    {name: 'Normalized IQR', value: -1},
    { name: '75th percentile', value: -1},
    {name: '% of outliers', value: -1},
    { name: 'avg', value: this.graphService.getAvg(this.dataset.map(d => d.value)) },
    { name: 'σ', value: this.graphService.getDeviation(this.dataset.map(d => d.value)) },
  ];

  constructor(private route: ActivatedRoute, private router:Router, private activityService: ActivityService, private modalService: NgbModal, private graphService:GraphService) {}

  open(content: TemplateRef<any>) {
    this.modal = this.modalService.open(content)
  }

  secsToMinSec(value: number): string {
    const minutes = Math.floor(value / 60);
    const seconds = value % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`; // Format as mm:ss 1500 100 500
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
        this.updateChart("init")
      },
      error: () => {alert("There was an error fetching the activity. Try reloading the page.")}
    })
  }

  //#region Chart Lifecycle
  updateChart(event:string) {
    this.yAxisTickFormat = this.selectedMetric==="pace" ? this.secsToMinSec:(value) => value.toString()
    this.displayData = []
    this.dataset = this.graphService.getGraphData(this.selectedMetric, this.activity, this.xAxisRepresents, this.partitionNum)
    this.displayData = this.graphService.getDisplayData(this.dataset, this.selectedMetric, this.selectedChart)
    if (event!=="changeChart")
      this.updateRatings()
    if (event==="metricChange" || event==="init")
      this.updateGoals()
    this.pushExtras()
  }
  pushExtras(){
    this.referenceLines = []
    const xAxis = this.activityService.getMetric(this.xAxisRepresents, this.activity)
    if (this.extrasSet.has("goal")){
      this.referenceLines.push({name:"goal", value:this.extrasValues.goal}) //This can be done iterating through the set and with this.extrasValues[currentVar]
    }
    if (this.extrasSet.has("upperLimit")){
      this.referenceLines.push({name:"Upper Limit", value:this.extrasValues.upperLimit})
    }
    if (this.extrasSet.has("lowerLimit")){
      this.referenceLines.push({name:"Lower Limit", value:this.extrasValues.lowerLimit})
    }

    if (this.extrasSet.has("percentiles")){
      const percentilesList = this.ratings.filter(x => x.name.includes("percentile"));
      this.referenceLines.push(...percentilesList)
    }

    if (this.extrasSet.has("outlierLimits")){
      const outliers = this.graphService.calcOutliers(this.dataset.map(x=>x.value))
      this.referenceLines.push({name:"Outlier Limit", value:outliers.higher})
      this.referenceLines.push({name:"Outlier Limit", value:outliers.lower})
    }

  }
  updateRatings(){
    for (let tier of this.ratings) {
     tier.value = this.graphService.calc(tier.name, this.dataset.map(d=>d.value), tier.value)
    }
  }
  updateGoals(){
    const variance = this.graphService.getDeviation(this.dataset.map(d => d.value))
    const goal = this.graphService.getAvg(this.dataset.map(d => d.value))
    const lowerLimit = goal - variance
    const upperLimit = goal + variance

    this.extrasValues.goal = parseFloat(goal.toFixed(2))
    this.extrasValues.lowerLimit = parseFloat(lowerLimit.toFixed(2))
    this.extrasValues.upperLimit = parseFloat(upperLimit.toFixed(2))
    this.pushExtras()
  }
  //#endregion

  //#region onChange
  changeChart(newChart:string) {
    for (let key of this.chartsKeys) {
      this.charts[key] = false
    }
    this.charts[newChart] = true

    this.xAxisRepresents = newChart==="histogram" ? "distribution":"timeElapsed"

    this.updateChart("changeChart")
  }

  updateExtras(extra: string) {
    if (this.extrasSet.has(extra))
      this.extrasSet.delete(extra)
    else
      this.extrasSet.add(extra)
    this.updateChart("extras")
  }
  //#endregion

}
