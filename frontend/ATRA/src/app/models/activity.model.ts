import { ActivityStreams } from "./activity-streams.model";
import { DataPoint } from "./datapoint.model";
import { User } from './user.model';

export class Activity {

    id: number;

    name:string;
    type:string;

    startTime:Date;
    endTime:Date; //

    totalTime:number;
    totalDistance:number;
    elevationGain:number;

    user: User; //probably will need to change when User receives activities and/or routes Change to {id:number, name:string} or similar
    route:{id:number, name:string};

    dataPoints: DataPoint[];
    streams: ActivityStreams;

    other:any;

    constructor(activity: any) {
      this.id = activity.id;
      this.name = activity.name;
      this.type = activity.type;
      this.startTime = new Date(activity.startTime);
      this.endTime = new Date(activity.endTime);
      this.totalTime = activity.totalTime;
      this.totalDistance = activity.totalDistance;
      this.elevationGain = activity.elevationGain

      this.user = activity.user;
      this.route = activity.route;

      this.dataPoints = activity.dataPoints;
      this.streams = activity.streams;
      this.other = activity.other;
  }

  getOverview(): {name:string; value:string}[] {
    return [
    {name:"Name", value:this.name},
    {name:"Type", value:this.type},
    {name:"Start time", value:this.startTime.getHours()+":"+this.startTime.getMinutes()},
    {name:"Date", value:this.startTime.toISOString().split("T")[0]},
    {name:"Duration", value:""+Activity.formatTime(this.totalTime)},
    {name:"Total distance", value:this.totalDistance.toFixed(2)},
    {name:"Elevation gain", value:this.elevationGain.toFixed(2)},
    {name:"Route", value:""+this.route},
  ]

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
    const totalSeconds = seconds;
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
}
