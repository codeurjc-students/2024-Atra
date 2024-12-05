import { UserService } from './../services/user.service';
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

    streams: {
      time : string[],
      distance : number[],
      position : number[],
      altitude : number[],
      heartrate : number[],
      cadence : number[],
      other: any[]
    }
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
}
