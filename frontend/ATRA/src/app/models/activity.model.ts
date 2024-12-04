export interface Activity {
    id: number;

    name:string;
    type:string;
    startTime:string;
    endTime:string;
    totalTime:string;
    user: {id:number, name:string};
    route:{id:number, name:string}

    streams: {
      time : number[],
      distance : number[],
      position : number[],
      altitude : number[],
      heartrate : number[],
      cadence : number[],
      other: any[]
    }
}
