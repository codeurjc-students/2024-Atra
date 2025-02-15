import { Injectable } from '@angular/core';
import { ActivityStreams } from '../models/activity-streams.model';
import { Activity } from '../models/activity.model';
import { ActivityService } from './activity.service';
import { map } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class GraphService {

  getDisplayData(dataset: { name: string; value: number; }[], selectedMetric: string, selectedChart: string): { name: string; series: { name: string; value: number }[] }[] | { name: string; value: number }[] {
    return selectedChart=="line"  ? [{'name':selectedMetric, 'series':dataset}] : dataset
  }

  calc(name: string, data:number[], prevValue:number=0): number {
    if (name==="σ") return this.getDeviation(data)
    if (name==="25th percentile") return this.getQuantile(data, 0.25)
    if (name==="50th percentile") return this.getQuantile(data, 0.5)
      if (name==="75th percentile") return this.getQuantile(data, 0.75)
    return prevValue
  }

constructor(private activityService: ActivityService) { }

getAvg(data:number[]): number{
  let total = 0
  for (let d of data){
    total += d
  }
  return total/data.length
}

getDeviation(data:number[], includedAvg:boolean=false, avg:number=0){
  if (!includedAvg) {
    avg = this.getAvg(data)
  }

  let total = 0
  for (let d of data) {
    total += Math.pow(d-avg, 2)
  }
  return total/(data.length-1)
}


getGraphData(metric: string, activity: Activity, xAxis: string, partitions:number=-1): { name: string, value: number; }[] {
    var metricData:number[];
    const time = activity.streams.time;
    const distance = activity.streams.distance
    console.log('partitions', partitions)
    console.log('xAxis', xAxis)


    if (!(Object.keys(new ActivityStreams())).includes(metric)) {
      throw new Error(`Given metric '${metric}' is not a key of ActivityStreams`)
    }
    metricData = activity.streams[metric as keyof ActivityStreams].map(x => parseFloat(x))

    if (xAxis=="distribution"){
      return this.distributionData(metricData, partitions)
    }

    const processedData: {name: string; value: number;}[] = []
    const startTime = new Date(time[0]);
    for (var i=0;i<metricData.length;i++) {
      const currentTime = new Date(time[i])

      switch(xAxis) {
        case "timeElapsed": processedData.push({name:this.activityService.getTime(currentTime, startTime),value:metricData[i]}); break;
        case "timeOfDay": processedData.push({name:currentTime.getHours().toString()+":"+currentTime.getMinutes().toString()+":"+currentTime.getSeconds().toString(),value:metricData[i]}); break;
        case "totalDistance": processedData.push({name:parseFloat(distance[i]).toFixed(2),value:metricData[i]}); break;
        default: {
          console.log("xAxisRepresents has a bad value. Defaulting to timeElapsed")
          processedData.push({name:this.activityService.getTime(currentTime, startTime),value:metricData[i]})
        }
      }
    }


    return processedData //[{ name: "1", value: 2 }, { name: "2", value: 2 },{ name: "3", value: 3 }]
  }




  distributionData(metricData: number[], partitions: number) {
    const lowest = Math.min(...metricData)
    const highest = Math.max(...metricData)
    const diff = highest-lowest
    const partSize = diff/partitions
    const arrays: number[][]= Array.from({ length: partitions }, () => []); // n empty arrays
    const processedData: {name: string; value: number;}[] = []

    console.log("Lowest:", lowest);
    console.log("Highest:", highest);
    console.log("Difference:", diff);
    console.log("Partition Size:", partSize);

    for (const n of metricData){

      if (Math.floor((n-lowest)/partSize)>=partitions){
        console.log("exception")
        arrays[partitions-1].push(n)
      }
      else {
        arrays[Math.floor((n-lowest)/partSize)].push(n)
      }
    }
    for (let i = 0; i < partitions; i++) {
      processedData.push({name: (lowest+(i*partSize)).toFixed(1)+"-"+(lowest+(i+1)*partSize).toFixed(1), value: arrays[i].length})
    }
    console.log(processedData)

    //c++;
    //container.push(parseFloat(metricData[i]))
    //if (c==p || i==metricData.length-1){
    //  console.log("We in")
    //  processedData.push({name:i.toString(),value:this.avg(container)})
    //  c=0
    //  container=[]
    //};

    return processedData
  }

  avg(container: number[]): number {
    var sum = 0
    for (const i of container){
      sum += i
    }
    return sum/container.length
  }

  getQuantile(data: number[], quantile: number): number {
    if (data.length === 0) return 0; //throw new Error("Data array is empty");

    const sorted = [...data].sort((a, b) => a - b);
    const index = (sorted.length - 1) * quantile;
    const lower = Math.floor(index);
    const upper = Math.ceil(index);

    if (lower === upper) return sorted[lower];

    return sorted[lower] + (index - lower) * (sorted[upper] - sorted[lower]);
  }
}
