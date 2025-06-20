import { FormattingService } from "../services/formatting.service";
import { ActivityStreams } from "./activity-streams.model";
import { ActivitySummary } from "./activity-summary.model";
import { DataPoint } from "./datapoint.model";
import { User } from './user.model';

export class Activity {

    id: number;

    name:string;
    type:string;

    startTime:Date;
    endTime:Date; //

    user: User; //probably will need to change when User receives activities and/or routes Change to {id:number, name:string} or similar
    route:{id:number, name:string};

    dataPoints: DataPoint[];
    streams: ActivityStreams;

    summary:ActivitySummary | null;
    other:any;

    visibility: "PRIVATE" | "MURAL_SPECIFIC" | "MURAL_PUBLIC" | "PUBLIC";

    constructor(activity: any) {
      this.id = activity.id;
      this.name = activity.name;
      this.type = activity.type;
      this.startTime = new Date(activity.startTime);
      this.endTime = new Date(activity.endTime);

      this.user = activity.user;
      this.route = activity.route;

      this.dataPoints = activity.dataPoints;
      this.streams = activity.streams;
      this.summary = activity.summary;
      this.other = activity.other;

      this.visibility = activity.visibility || "PRIVATE"; // Default to PRIVATE if not set
  }

  getOverview(): {name:string; value:string}[] {
    var overview = [
    {name:"Name", value:this.name},
    {name:"Type", value:this.type},
    {name:"Start time", value:FormattingService.formatDateTime(this.startTime)},
    {name:"Date", value:this.startTime.toISOString().split("T")[0]},
    ]
    if (this.summary) {
      if (this.summary.totalTime) overview.push({name:"Duration", value:""+FormattingService.formatTime(this.summary.totalTime)});
      if (this.summary.totalDistance) overview.push({name:"Total distance", value:this.summary.totalDistance.toFixed(2)});
      if (this.summary.elevationGain) overview.push({name:"Elevation gain", value:this.summary.elevationGain.toFixed(2)});
      if (this.summary.averages) { // Alternatively, just add overview.push({name:`Average Pace`, value:this.summary.averages["pace"].toFixed(2)});
        for (const [key, value] of Object.entries(this.summary.averages)) {
          if (key!="pace") overview.push({name:`Average ${key}`, value:value.toFixed(2)});
          else overview.push({name:`Average Pace`, value:FormattingService.formatPace(value)});
        }
      }
    }
    if (this.route) overview.push({name:"Route", value:this.route.name});
    return overview;
  }

  getStream(stream: string){
    if (!(Object.keys(this.streams).includes(stream))) return [`Requested metric '${stream}' is not a key of activity.streams`]
    return this.streams[stream as keyof typeof this.streams]
  }

  hasRoute(): boolean {
    return this.route != null
  }
}
