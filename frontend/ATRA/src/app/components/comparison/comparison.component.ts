import { NamedSeries, NameValue } from './../../services/graph.service';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Component, OnInit, TemplateRef } from '@angular/core';
import { Activity } from '../../models/activity.model';
import { ActivityService } from '../../services/activity.service';
import { forkJoin, Observable } from 'rxjs';
import { AlertService } from '../../services/alert.service';
import { GridItemService } from '../../services/grid-item.service';
import { GraphService } from '../../services/graph.service';
import { FormattingService } from '../../services/formatting.service';
import { MapService } from '../../services/map.service';
import L from 'leaflet';
import { FormsModule } from '@angular/forms';
import { Color, NgxChartsModule, ScaleType } from '@swimlane/ngx-charts';
import { ActivityStreams } from '../../models/activity-streams.model';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'app-comparison',
  standalone: true,
  imports: [CommonModule, FormsModule, NgxChartsModule],
  templateUrl: './comparison.component.html',
  styleUrl: './comparison.component.scss'
})
export class ComparisonComponent implements OnInit {

  activities!:Activity[];
  hiddenActs:Set<number> = new Set();
  loading:boolean = false;

  constructor(
    private route:ActivatedRoute,
    private activityService:ActivityService,
    private alertService:AlertService,
    private gridItemService:GridItemService,
    private graphService:GraphService,
    private modalService:NgbModal,
    private router:Router
  ) {}

  ngOnInit(): void {
    const ids:number[] = []
    this.loading=true

    this.route.snapshot.paramMap.get('activityId')?.split('-').forEach(id => {
      const parsedId = parseInt(id);
      if (!isNaN(parsedId)) {
        ids.push(parsedId)
      }
    });
    const muralIdString = this.route.snapshot.paramMap.get('muralId')
    const muralId = muralIdString ? parseInt(muralIdString):undefined
    this.activityService.getActivities(ids,  muralId).subscribe({
      next: (reply) => {
        this.loading=false

        if (!reply.body) {console.warn("(ComparisonComponent) getActivities returned an empty response");return}
        if (reply.headers.get("ATRA-requested-forbidden")=="true") this.alertService.toastWarning("Some of the activities requested cannot be accessed. They've been omitted")

        const activities = this.activityService.process(reply.body)
        this.recordsFits=activities.length<10
        this.activities = activities;

        this.initMapCrap();
        this.compareActivities();
        this.updateSplits();
        this.calculateMetrics();
        //activities.forEach(act=>this.hiddenActs.add(act.id));
        for (let c=0;c<activities.length;c++)this.histOptions.push({name:activities[c].name,id:activities[c].id,pos:c})
        this.histOpt1=this.histOptions[0]
        this.histOpt2=this.histOptions[1]
        this.updateChart();
      },
      error: (error) => {
        this.loading=false
        this.alertService.toastError("Something went wrong fetching the activities. Try reloading the page.")
        console.error("(ComparisonComponent) Error loading activities for comparison:", error)
      }
    })
  }

  //#region Summary
  shortestLastingAct:{id:number, name:string, time:number} = {id:0, name:"",time:Infinity};
  longestLastingAct:{id:number, name:string, time:number} = {id:0, name:"",time:0};
  longestAct:{id:number, name:string, distance:number} = {id:0, name:"",distance:0};
  shortestAct:{id:number, name:string, distance:number} = {id:0, name:"",distance:Infinity};
  highestAct:{id:number, name:string, ele:number} = {id:0, name:"",ele:0};
  bestPaceAct:{id:number, name:string, pace:number} = {id:0, name:"",pace:Infinity};
  mostConsistentPaceAct:{id:number, name:string, iqr:number} = {id:0, name:"",iqr:Infinity};//use secs not deviation
  records:string[][] = [];//copy it from wherever also has records

  overviewMessage: string = "";
  compareActivities() {
    this.records = this.gridItemService.calcRecords(this.activities, false);

    this.records = this.records.filter(r=>r[1]!="-"&&r[1]!="0.00km")

    var actsWithoutSummary = 0
    var actsWithoutAverages = 0
    for (let act of this.activities) {
      const summary = act.summary;
      if (summary==null) {actsWithoutSummary++;continue;}
      if (summary.totalTime<this.shortestLastingAct.time) {this.shortestLastingAct={id:act.id,name:act.name,time:summary.totalTime}}
      if (summary.totalTime>this.longestLastingAct.time) {this.longestLastingAct={id:act.id, name:act.name,time:summary.totalTime}}
      if (summary.totalDistance>this.longestAct.distance) {this.longestAct={id:act.id, name:act.name,distance:summary.totalDistance}}
      if (summary.totalDistance<this.shortestAct.distance) {this.shortestAct={id:act.id, name:act.name,distance:summary.totalDistance}}
      if (summary.elevationGain>this.highestAct.ele) {this.highestAct={id:act.id, name:act.name,ele:summary.elevationGain}}

      const data = act.streams['pace'].map(x => parseFloat(x));
      const currentIQR = this.graphService.getQuantile(data, 0.75) - this.graphService.getQuantile(data, 0.25);
      if (currentIQR<this.mostConsistentPaceAct.iqr) {this.mostConsistentPaceAct={id:act.id, name:act.name,iqr:currentIQR}}
      const avgs = summary.averages;
      if (avgs==null) {actsWithoutAverages++;continue;}
      if (avgs['pace']<this.bestPaceAct.pace) {this.bestPaceAct={id:act.id, name:act.name,pace:avgs['pace']}}
    }

    if (actsWithoutSummary>0) {
      console.warn(`(ComparisonComponent) ${actsWithoutSummary} out of ${this.activities.length} activities did not have a summary, so they were ignored.`);
      this.alertService.toastWarning("Some of the activities could not be compared since they lack neccessary data")
    }
    if (actsWithoutAverages>0) {
      console.warn(`(ComparisonComponent) ${actsWithoutAverages} out of ${this.activities.length} activities did not have averages, so they were ignored for average-based comparisons.`);
      this.alertService.toastWarning("We couldn't get the average pace for some activities, so they weren't considered in the comparison")
    }
    this.prepareOverviewMessage();
  }
  goTo(idString:string|undefined) {
    if (idString==undefined) return this.alertService.toastWarning("This record has no activity associated")
    const id = parseInt(idString)
    if (isNaN(id)) return this.alertService.toastWarning("This record's associated activity has an invalid id")
    const urlParts = this.route.snapshot.url
    const urlStart = urlParts[0].toString()
    if  (urlStart=='me') this.router.navigate([urlStart, 'activities', id])
    else this.router.navigate([urlStart, urlParts[1].toString(), 'activities', id])
  }

  private prepareOverviewMessage() {
    this.overviewMessage = `<div>
    Out of the ${this.bold(this.activities.length.toString())} activities compared, we can highlight the following:</div><ul>
    <li>${this.bold(this.longestLastingAct.name)} was the activity that took longest to complete, taking a whopping ${this.bold(FormattingService.formatTime(this.longestLastingAct.time,1))}, which is`
    if (this.longestLastingAct.id==this.longestAct.id) {
      this.overviewMessage += ` understandable, as it also happens to be the longest activity, covering ${this.bold(FormattingService.formatDistance(this.longestAct.distance))}.\n`
    } else {
      this.overviewMessage += ` a bit perplexing, as it wasn't the longest activity. That honor instead goes to ${this.bold(this.longestAct.name)}, which covered ${this.bold(FormattingService.formatDistance(this.longestAct.distance))}.\n`
    }
    this.overviewMessage += `<li>Opposite to that, ${this.bold(this.shortestLastingAct.name)} was the quickest activity, lasting a brief ${this.bold(FormattingService.formatTime(this.shortestLastingAct.time,1))}. `

    if (this.shortestLastingAct.id==this.shortestAct.id) {
      if (this.longestLastingAct.id==this.longestAct.id) this.overviewMessage += `This, again, is`
      else this.overviewMessage += ` This is`
      this.overviewMessage += ` understandable, as it also happens to be the shortest activity, covering only ${this.bold(FormattingService.formatDistance(this.shortestAct.distance))}.\n`
    } else {
      if (this.longestLastingAct.id!=this.longestAct.id) this.overviewMessage += `This, again, is`
      else this.overviewMessage += ` This is`
      this.overviewMessage += ` surprising, as it wasn't the shortest activity. That role is covered by ${this.bold(this.shortestAct.name)}, which spanned only ${this.bold(FormattingService.formatDistance(this.longestAct.distance))}.\n`
    }

    this.overviewMessage += `</li><li>${this.bold(this.highestAct.name)} must have taken some extra effort to complete, as it had the highest elevation gain, climbing a total of ${this.bold(this.highestAct.ele.toFixed(0)+"m")}.\n`
    this.overviewMessage += `</li><li>When it comes to actual speed, ${this.bold(this.bestPaceAct.name)} takes the crown, boasting an average pace of ${this.bold(FormattingService.formatPace(this.bestPaceAct.pace))}, the best of all compared.\n`
    if (this.bestPaceAct.id==this.mostConsistentPaceAct.id) {
      this.overviewMessage += `Not only that, but it was also the most consistent in terms of pace, with half of all measurements falling in a range of only ${this.bold(this.mostConsistentPaceAct.iqr.toString())} seconds.\n`
    } else {
      this.overviewMessage += `Don't be too happy however, as it seems that ${this.bold(this.mostConsistentPaceAct.name)} had a more consistent pace (with half of all measurements falling in a range of ${this.bold(this.mostConsistentPaceAct.iqr.toString())} seconds). Having a good average is good, but consistency is also important!\n`
    }
    this.overviewMessage += `</li></ul>`

   //this.overviewMessage += `Finally, let's talk records. ${}` //if we wanted to add a "Act1 has the record for fastest 1km, in 4:32, and you must have been on fire that day, as it also holds the record for 5km and 10km!" or whatever

  }

  private bold(s:string): string {
    return `<span class="fw-bold">${s}</span>`;
  }
  //#endregion

  //#region Map
  overviewMap: L.Map|null = null;
  overviewPaths: L.Polyline[] = [];
  maps: L.Map[] = [];
  paths : L.Polyline[] = [];
  colors = ['blue', 'orange', 'green', 'red', 'purple']
  mapsToShow: number = 1;
  showOnePerMap: boolean = false;
  options:number[] = [];
  activityPerMap:number[] = [0,1,2,3]
  initMapCrap() {
    this.mapsToShow = Math.min(this.activities.length,4)
    this.summaryClicked(); //since it's the default tab


    for (let i=1;i<=this.mapsToShow;i++) this.options.push(i);
    //this.showDifferentMaps(); //this one won't actually be there
  }

  createPath(act:Activity, index:number=0) {
    const coordinates = this.activityService.getCoordinates(act)
    return L.polyline(coordinates, {
      color: this.colors[index%5],         // Line color
      weight: 4,             // Line thickness
      opacity: 0.8,          // Line opacity
    });
  }
  clearPaths() {
    this.paths = [];
  }

  summaryClicked() {
    if (this.overviewMap!=null) {this.overviewMap.remove();this.overviewMap=null}
    setTimeout(()=>{
        this.overviewMap = MapService.mapSetup("map-overview")
        const paths: L.Polyline[] = []
        for (let i=0;i<this.activities.length;i++) paths.push(this.createPath(this.activities[i],i));
        const group = L.featureGroup(paths);
        group.addTo(this.overviewMap);
        this.overviewMap.fitBounds(group.getBounds());
    },175)
  }

  mapsClicked() {
    setTimeout(()=>this.updateMaps(), 200) //wait a bit for the tab to be fully active
  }
  showOnePerMapMethod() {
    setTimeout(()=>{ //wait for the DOM to update
      for (let i=0;i<this.mapsToShow;i++) {
        this.maps.push(MapService.mapSetup("map-"+(i+1).toString()))
        const path = this.createPath(this.activities[this.activityPerMap[i]],i).addTo(this.maps.at(-1)!);
        this.maps.at(-1)!.fitBounds(path.getBounds());
      }
    })
  }
  showAllPerMapMethod() {
    setTimeout(()=>{ //wait for the DOM to update
      for (let i=0;i<this.mapsToShow;i++) {
        this.maps.push(MapService.mapSetup("map-"+(i+1).toString()))
        const paths: L.Polyline[] = []
        for (let i=0;i<this.activities.length;i++) paths.push(this.createPath(this.activities[i],i));
        const group = L.featureGroup(paths);
        group.addTo(this.maps.at(-1)!);
        this.maps.at(-1)!.fitBounds(paths[this.activityPerMap[i]].getBounds());
      }
    })

  }
  updateMaps() {
    for (let m of this.maps) {
      m.remove();
    }
    this.maps = []
    if (this.showOnePerMap) this.showOnePerMapMethod();
    else this.showAllPerMapMethod();
  }
  //#endregion

  //#region Graph

  readonly chartTypes = chartTypes;
  selectedChart: chartType = 'line';
  view: [number, number] = [1400, 500];
  colorScheme: Color = {
    name:'',
    selectable:true,
    group:ScaleType.Linear,
    domain: this.colors
  };

  metrics: string[] = ActivityStreams.getGraphableKeys();
  selectedMetric = this.metrics[0];
  xAxisRepresents: 'timeElapsed' | 'timeOfDay' | 'totalDistance' |'distribution' = "timeElapsed";

  dataset : NameValue[][] = [];
  displayData!: NamedSeries[] | NameValue[][];
  partitionNum1: number= 5
  partitionNum2: number= 5
  histOptions:{id:number, name:string,pos:number}[] = [];
  histOpt1!:{id:number, name:string, pos:number};
  histOpt2!:{id:number, name:string, pos:number};

  loadingChart:boolean = false;
  useAvgs:boolean = false;

  extrasSet: Set<string> = new Set();
  yAxisTickFormat: (value: number) => string = (value) => value.toString()

  updateChart() {
    console.log("(ComparisonComponent) Updating chart");

    this.loadingChart = true
    this.yAxisTickFormat = this.selectedMetric==="pace" ? FormattingService.formatPace:(value) => value.toString()
    const shownActivities = this.activities.filter(act=>!this.hiddenActs.has(act.id))
    this.dataset = this.graphService.getGraphDataMultiple(this.selectedMetric, shownActivities, this.xAxisRepresents, this.partitionNum1, this.useAvgs)
    this.displayData = this.graphService.getDisplayDataMultiple(this.dataset, this.selectedMetric, this.selectedChart, shownActivities.map(a=>a.name))
    this.loadingChart = false
    console.log("(ComparisonComponent) Chart updated");

  }
  changeChart() {
    //should manage variables that change depending on if we display histogram or line graph
    console.log("(ComparisonComponent) selectedChart is ",this.selectedChart);
    if (this.selectedChart=='histogram') {
      this.xAxisRepresents='distribution'
      this.hiddenActs.clear()
    } else this.xAxisRepresents='timeElapsed'
    this.updateChart()

  }

  open(content: TemplateRef<any>) {
    this.modalService.open(content/*, { centered:true }*/) //commented cause if it's above we can better see the changes to the graph as they happen
  }

  toggleAct(id:number){
    if (this.hiddenActs.has(id)) this.hiddenActs.delete(id)
    else this.hiddenActs.add(id)

    this.updateChart();
  }
  //#endregion

  //#region Study
  calcMetrics:Map<number,MetricArray> = new Map()
  lowerCalcBound:number|undefined;
  higherCalcBound:number|undefined;
  calculatorPercentages:{name:string,percent:number}[]=[]

  currentSplitDistance: number = 1;
  customSplits:boolean = false;
  currentSplitUnit:'km'|'mi'='km';
  splits:Map<number,string[]> = new Map();

  recordsFits:boolean = true;
  showCalc:boolean = true;

  calculateMetrics(){
    this.activities.forEach(act=>{
      const li = defaultMetricArray()
      if (act.summary) {
        li.dist = FormattingService.formatDistance(act.summary.totalDistance)
        li.time = FormattingService.formatTime(act.summary.totalTime)
      }
      const stream = act.streams[this.selectedMetric as keyof ActivityStreams].map(x => parseFloat(x));
      for (const key of Object.keys(li) as (keyof MetricArray)[]) {
        if (key=="dist" || key=="time") continue
        li[key] = this.graphService.calc(key, stream);
      }

      this.calcMetrics.set(act.id, li)
    })
  }

  calcPercentForCalculator(){
    if (!this.lowerCalcBound || !this.higherCalcBound) return
    const low = this.lowerCalcBound<this.higherCalcBound?this.lowerCalcBound:this.higherCalcBound
    const high = this.higherCalcBound>this.lowerCalcBound?this.higherCalcBound:this.lowerCalcBound

    this.activities.forEach(act=>{
      const stream = act.streams[this.selectedMetric as keyof ActivityStreams]
      const filteredStream = stream.filter(v=>parseFloat(v)>=low! && parseFloat(v)<=high!)
      this.calculatorPercentages.push({name:act.name, percent:filteredStream.length/stream.length*100})
    })
  }
  clearPercents(){
    this.lowerCalcBound=undefined
    this.higherCalcBound=undefined
    this.calculatorPercentages = []
  }

  updateSplits(v?:string){
    this.splits.clear()
    if (v=="-1") {this.customSplits=true;this.currentSplitDistance=1}
    else if (v) {this.customSplits=false; this.currentSplitDistance=parseInt(v)}
    const localSplits:number[][] = []
    var longestSplitCount = -1;
    this.activities.forEach(act=>{
      const actSplits = this.graphService.calcSplits(act, this.currentSplitDistance, this.currentSplitUnit)
      if (actSplits.length>longestSplitCount) longestSplitCount=actSplits.length
      localSplits.push(actSplits)
    })
    for (let i=0;i<longestSplitCount;i++) {
      const currentSplit = [];
      for (let actSplit of localSplits) {
        currentSplit.push(actSplit[i]?FormattingService.formatPace(actSplit[i]):"-")
      }
      this.splits.set((i+1)*this.currentSplitDistance,currentSplit)
    }
  }

  switchCalcAndRecords() {
    this.showCalc = !this.showCalc;
  }
  //#endregion

}

  export const chartTypes = ['line', 'histogram'] as const;
  type chartType = typeof chartTypes[number];

  export type MetricArray = {
    dist:string;
    time:string;
    p25:number;
    p50:number;
    p75:number;
    avg:number;
    dev:number
    IQR:number;
    NormIQR:number;
    pOut:number;
  }

  export function defaultMetricArray():MetricArray {
    return {
      dist: "N/A",
      time: "N/A",
      p25: -1,
      p50: -1,
      p75: -1,
      avg: -1,
      dev: -1,
      IQR: -1,
      NormIQR: -1,
      pOut: -1,
  };
  }

