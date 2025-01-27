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
    var metricData: string[];
    const time = activity.streams.time;
    switch (metric) {
      case ("pace") : metricData = activity.streams.altitude; break; //not actually implemented
      //case ("time") : metricData = activity.streams.time; break;
      case ("distance") : metricData = activity.streams.distance; break;
      //case ("position") : metricData = activity.streams.position; break;
      case ("altitude") : metricData = activity.streams.altitude; break;
      case ("heartrate") : metricData = activity.streams.heartrate; break;
      case ("cadence") : metricData = activity.streams.cadence; break;
      case ("other") : metricData = activity.streams.other; break;
      default: metricData = [];
    }
    const processedData: {name: string; value: number;}[] = []
    const startTime = new Date(time[0]);
    for (var i=0;i<metricData.length;i++) {
      const currentTime = new Date(time[i])


      processedData.push({name:this.getTime(currentTime, startTime),value:parseFloat(metricData[i])})
    }

    return processedData //[{ name: "1", value: 2 }, { name: "2", value: 2 },{ name: "3", value: 3 }, { name: "4", value: 4 },{ name: "5", value: 5 }, { name: "6", value: 6 },{ name: "7", value: 7 }, { name: "8", value: 8 }]
  }

  getTime(date1:Date, date2:Date): string{
    // Time Difference in Milliseconds
    const milliDiff: number = date1.getTime() - date2.getTime();
    // Total number of seconds in the difference
    const totalSeconds = Math.floor(milliDiff / 1000);
    // Total number of minutes in the difference
    const totalMinutes = Math.floor(totalSeconds / 60);
    // Total number of hours in the difference
    const totalHours = Math.floor(totalMinutes / 60);
    // Getting the number of seconds left in one minute
    const remSeconds = totalSeconds % 60;
    // Getting the number of minutes left in one hour
    const remMinutes = totalMinutes % 60;

    console.log(`${totalHours}:${remMinutes}:${remSeconds}`);

    const hoursString = totalHours != 0 ? totalHours.toString()+":":""
    const minsString = (remMinutes < 10 && totalHours!=0) ? "0"+remMinutes.toString():remMinutes.toString()
    const secsString = remSeconds < 10 ? "0"+remSeconds.toString():remSeconds.toString()


    return `${hoursString}${minsString}:${secsString}`

  }

}
