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
      distance : string[],
      position : string[],
      altitude : string[],
      heartrate : string[],
      cadence : string[],
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

  getOverview(): {name:string; value:string}[] {
    var result:{name:string; value:string}[]  = [];
    result.push({name:"Name", value:this.name})
    result.push({name:"Type", value:this.type})
    result.push({name:"Start time", value:this.startTime.getHours()+":"+this.startTime.getMinutes()})
    result.push({name:"Date", value:this.startTime.toISOString().split("T")[0]})
    result.push({name:"Duration", value:""+this.totalTime})
    result.push({name:"Total distance", value:""+this.totalDistance})
    result.push({name:"Route", value:""+this.route})

    return result
  }

  getMetricData(metric: string, activity: Activity): { name: string, value: number; }[] {
    var metricData: string[] = [];
    const time = activity.streams.time;
    switch (metric) {
      //case ("time") : metricData = activity.streams.time; break;
      case ("distance") : metricData = activity.streams.distance; break;
      //case ("position") : metricData = activity.streams.position; break;
      case ("altitude") : metricData = activity.streams.altitude; break;
      case ("heartrate") : metricData = activity.streams.heartrate; break;
      case ("cadence") : metricData = activity.streams.cadence; break;
      case ("other") : metricData = activity.streams.other; break;
    }
    for (const datapoint of  metricData) {

    }

    return [{ name: "1", value: 1 }, { name: "2", value: 2 }]
  }

}
