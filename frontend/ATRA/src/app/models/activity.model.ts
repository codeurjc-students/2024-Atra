import { ActivityStreams } from "./activity-streams.model";
import { DataPoint } from "./datapoint.model";
import { User } from './user.model';

export class Activity {

    id: number;

    name:string;
    type:string;
    startTime:Date;

    endTime:string; //

    totalTime:number;
    totalDistance:number;
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
      this.endTime = activity.endTime;
      this.totalTime = activity.totalTime;
      this.totalDistance = activity.totalDistance;

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
    {name:"Duration", value:""+this.totalTime},
    {name:"Total distance", value:this.totalDistance.toFixed(2)},
    {name:"Route", value:""+this.route},
  ]

  }

  getStream(stream: string){
    if (!(Object.keys(this.streams).includes(stream))) return [`Requested metric '${stream}' is not a key of activity.streams`]
    return this.streams[stream as keyof typeof this.streams]
  }
}
