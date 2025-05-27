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
  }

  getOverview(): {name:string; value:string}[] {
    var overview = [
    {name:"Name", value:this.name},
    {name:"Type", value:this.type},
    {name:"Start time", value:Activity.formatDateTime(this.startTime)},
    {name:"Date", value:this.startTime.toISOString().split("T")[0]},
    ]
    if (this.summary) {
      if (this.summary.totalTime) overview.push({name:"Duration", value:""+Activity.formatTime(this.summary.totalTime)});
      if (this.summary.totalDistance) overview.push({name:"Total distance", value:this.summary.totalDistance.toFixed(2)});
      if (this.summary.elevationGain) overview.push({name:"Elevation gain", value:this.summary.elevationGain.toFixed(2)});
      if (this.summary.averages) { // Alternatively, just add overview.push({name:`Average Pace`, value:this.summary.averages["pace"].toFixed(2)});
        for (const [key, value] of Object.entries(this.summary.averages)) {
          if (key!="pace") overview.push({name:`Average ${key}`, value:value.toFixed(2)});
          else overview.push({name:`Average Pace`, value:value.toString()});
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
    return this.route == null
  }

  static formatTime(seconds: number): string { //secsToHHMMSS

    // Total number of seconds in the difference
    const totalSeconds = Math.floor(seconds);
    // Total number of minutes in the difference
    const totalMinutes = Math.floor(totalSeconds / 60);
    // Total number of hours in the difference
    const totalHours = Math.floor(totalMinutes / 60);
    // Getting the number of seconds left in one minute
    const remSeconds = totalSeconds % 60;

    // Getting the number of minutes left in one hour
    const remMinutes = totalMinutes % 60;

    const hoursString = totalHours != 0 ? totalHours.toString()+":":""
    const minsString = (remMinutes < 10 && totalHours!=0) ? "0"+remMinutes.toString():remMinutes.toString()
    const secsString = remSeconds < 10 ? "0"+remSeconds.toString():remSeconds.toString()



    return `${hoursString}${minsString}:${secsString}`
  }

  static formatDateTime(dateTime: Date): string {
    const hours = dateTime.getHours();
    const minutes = dateTime.getMinutes()<10 ? '0'+dateTime.getMinutes():dateTime.getMinutes()
    return `${hours}:${minutes}`
  }
}
