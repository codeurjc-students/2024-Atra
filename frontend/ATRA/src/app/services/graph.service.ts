import { Injectable } from '@angular/core';
import { ActivityStreams } from '../models/activity-streams.model';
import { Activity } from '../models/activity.model';
import { ActivityService } from './activity.service';
import { DataPoint } from '../models/datapoint.model';
import { last } from 'rxjs';
import { FormattingService } from './formatting.service';

@Injectable({
  providedIn: 'root'
})
export class GraphService {
  //  processedData.push({name:i.toString(),value:this.avg(container)})
  //  c=0
  //  container=[]
  //};

  constructor(private activityService: ActivityService) { }

  getBetween(data: number[], n1: number, n2: number, roundTo:number=2): number {
    const high = Math.max(...[n1,n2])
    const low = Math.min(...[n1,n2])
    var count = 0
    for (const d of data){
      if (d<=high && d>=low) {
        count++
      }
    }

    return parseFloat(((count/data.length)*100).toFixed(roundTo))
  }

  getDisplayData(dataset: {name: string; value: number;}[], selectedMetric: string, selectedChart: string, seriesName?:string): NamedSeries[] | NameValue[] {
    return selectedChart=="line"  ? [{'name':seriesName??selectedMetric, 'series':dataset}] : dataset
  }
  getDisplayDataMultiple(datasets: {name: string; value: number; }[][], selectedMetric: string, selectedChart: string, seriesNames?:string[]): NamedSeries[] | NameValue[][] {
    const result = []
    for (let c=0;c<datasets.length;c++) {
      const data = datasets[c]
      const dData = this.getDisplayData(data, selectedMetric, selectedChart,seriesNames?.[c]??c.toString())
      result.push(selectedChart=="line"  ? dData[0]:dData)
    }
    return result as NamedSeries[] | NameValue[][];
  }

  calc(name: string, data:number[], prevValue:number=0): number {
    if (name==="σ" || name=='dev') return parseFloat(this.getDeviation(data).toFixed(2))
    if (name==="avg") return parseFloat(this.getAvg(data).toFixed(2))
    if (name==="25th percentile" || name=='p25') return parseFloat(this.getQuantile(data, 0.25).toFixed(2))
    if (name==="50th percentile" || name=='p50') return parseFloat(this.getQuantile(data, 0.5 ).toFixed(2))
    if (name==="75th percentile" || name=='p75') return parseFloat(this.getQuantile(data, 0.75).toFixed(2))
    if (name==="Normalized IQR" || name=='NormIQR') return parseFloat(((this.getQuantile(data, 0.75) - this.getQuantile(data, 0.25))/(Math.max(...data)-Math.min(...data))).toFixed(2))
    if (name==="IQR") return parseFloat((this.getQuantile(data, 0.75) - this.getQuantile(data, 0.25)).toFixed(2))
    if (name==="% of outliers" || name=='pOut') return parseFloat(this.calcOutliers(data)["percentage"].toFixed(2))
    return prevValue
  }

  calcOutliers(data: number[]): {lower:number, higher:number, percentage:number} {
    const q1 = this.getQuantile(data,0.25)
    const q3 = this.getQuantile(data,0.75)
    const IQR = q3-q1
    const lower = q1-1.5*IQR
    const higher = q3+1.5*IQR
    var outlierCount = 0

    for (const n of data) {
      if (n<lower || n>higher) {
        outlierCount++
      }
    }
    return {lower:lower, higher:higher, percentage:(outlierCount/data.length)*100}
  }

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
    return Math.sqrt(total/(data.length-1))
  }

  getGraphData(metric: string, activity: Activity, xAxis: string, partitions:number=-1): NameValue[] {
    var metricData:number[];
    const time = activity.streams.time;
    const distance = activity.streams.distance

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
          console.log("(GraphService) Variable xAxisRepresents holds an invalid value. Defaulting to timeElapsed")
          processedData.push({name:this.activityService.getTime(currentTime, startTime),value:metricData[i]})
        }
      }
    }
    return processedData //[{ name: "1", value: 2 }, { name: "2", value: 2 },{ name: "3", value: 3 }]
  }

  getGraphDataMultiple(metric: string, activities: Activity[], xAxis: string, partitions:number=-1, useAvgs:boolean=false): NameValue[][] {

    const result:NameValue[][] = [];
    for (const activity of activities) {
      const a = this.getGraphData(metric, activity, xAxis, partitions)
      result.push(a.sort((a,b)=>this.xValueToNumber(a.name)-this.xValueToNumber(b.name)));
    }
    return this.interpolateAndTruncate(result, useAvgs);
  }

  interpolateAndTruncate(data:NameValue[][], useAvgs:boolean):NameValue[][] {
    data.sort((a,b)=>this.xValueToNumber(a.at(-1)!.name)-this.xValueToNumber(b.at(-1)!.name))
    let firstValue = "-1";
    let jInit=0;
    let weight=1; //number of repeated values encountered so far. used to remove duplicates by calculating a running average
    const allXValuesSet = this.getAllXValues(data)
    for (let i=0;i<data.length-1;i++) {
      const shortestData = data[i]
      const lastValue = shortestData.at(-1)!.name

      const xValuesSet = this.getAllValuesBetween(firstValue,lastValue, allXValuesSet)
      const xValues = Array.from(xValuesSet).sort((a,b)=>this.xValueToNumber(a)-this.xValueToNumber(b));
      for (let k=i;k<data.length;k++) {
        const currentDataset = data[k]
        const currentXValuesSet = new Set() //could check that currentXValuesSet is subset of xValuesSet. It should be. If it isn't that's an error
        currentDataset.forEach(a=>currentXValuesSet.add(a.name))

        //interpolate values for any xValues it does not have
        const newDataset:NameValue[] = currentDataset.slice(0,jInit)
        let c = 0; //tracks how many elements are in xValues but not in currentXValues, needed to correct the index to access currentDataset
        let j;
        for (j=jInit;j-jInit<xValues.length;j++) {
          const currentXValue = xValues[j-jInit]
          if (newDataset.length!=0 && newDataset.at(-1)!.name==currentDataset[j-c].name) { //compare only name, wouldn't wanna add two measurements for the same instant with different values. Plus, it would wreck this whole algorithm. It depends strongly on a specific order and number of items
            if (!useAvgs) {
              c--;j--;
              continue //skip if it's repeated
            } else {
              const lastValue = newDataset.at(-1)
              lastValue!.value = (lastValue!.value*weight+currentDataset[j-c].value)/(weight+1)
              weight++;c--;j--;
              continue;
            }
          } else weight=1;
          if (currentXValuesSet.has(currentXValue)) {
            newDataset.push(currentDataset[j-c])
            if (newDataset.length>=2 && !this.checkIfSequential(newDataset.at(-2),newDataset.at(-1))) {
              console.error("Existing value being added non-sequentially1: "+JSON.stringify({prev:newDataset.at(-2)?.name,current:newDataset.at(-1)?.name}));
            }
          } else { //extrapolate and add
            c+=1
            const prev = currentDataset[j-c] //prev value to the one needing extrapolation (though not neccessarily inmediate predecessor)
            const next = currentDataset[j-c+1] //next value to the one needing extrapolation (though not neccessarily inmediate successor)
            if (prev==undefined || next==undefined) console.error("Error calculating interpolation. IndexOutOfBounds")

            //find difference in time/distance and in metric. Divide metric/time, add until time matches currentXValue
            const deltaX = this.xValueToNumber(next.name)-this.xValueToNumber(prev.name)
            const deltaV = next.value-prev.value
            const xDiff = this.xValueToNumber(currentXValue)-this.xValueToNumber(prev.name); //x diff from the last we have to the one we need to add
            const vDiff = (deltaV/deltaX)*xDiff
            newDataset.push({name:currentXValue,value:prev.value+vDiff})
            if (newDataset.length>=2 && !this.checkIfSequential(newDataset.at(-2),newDataset.at(-1))) {
              console.error("Missing value being added non-sequentially: "+JSON.stringify({prev:newDataset.at(-2)?.name,current:newDataset.at(-1)?.name}));
            }
          }
        }
        //newDataSet has entries for all xValues, but if we substitute now we lose its points AFTER the shortest activity ends. So we need to add the rest of the points to it
        for (let p=j-c;p<currentDataset.length;p++) newDataset.push(currentDataset[p])
        //check data[k] is subset of newDataSet. Should always be true, so commented //for (let item of data[k]) if (!newDataset.map(a=>JSON.stringify(a)).includes(JSON.stringify(item))) console.error("Data[k] is not a subset of newDataSet. This should never happen");

        data[k] = newDataset;
      }
      firstValue=lastValue;
      jInit+=xValuesSet.size-1;
    }
    return data;
  }
  private checkIfSequential(a1:NameValue|undefined,a2:NameValue|undefined):boolean {
    if (a1==undefined || a2==undefined) throw new Error("Trying to check if two NameValue are sequential but one of them is undefined")
    return this.xValueToNumber(a1.name)+1==this.xValueToNumber(a2.name) || Math.round((this.xValueToNumber(a1.name)+0.01)*100)==Math.round(this.xValueToNumber(a2.name)*100)
  }
  private xValueToNumber(xValue:string):number {
    let result = 0;
    if (xValue.includes(":")) {
      const prevParts = xValue.split(":")
      for (let i=0; i<prevParts.length; i++)
        result += parseInt(prevParts[i])*(60**(prevParts.length-1-i))
    }
    else if (!isNaN(parseFloat(xValue))) {
      result = parseFloat(xValue)
    }
    else {
      throw new Error("Error trying to transform x value to number. The x value format is unknown. Known formats are hh:mm:ss and direct numbers (1, 2, 11.32, etc)")
    }
    return result
  }
  private getAllXValues(data:NameValue[][]) {
    const values:Set<string> = new Set()
    data.forEach(d=>d.forEach(v=>values.add(v.name)))
    return values
  }
  private getAllValuesBetween(start:string, end:string, data:Set<string>) {
    const values:Set<string> = new Set()
    const startN = this.xValueToNumber(start)
    const endN = this.xValueToNumber(end)
    for (let v of data) {
      const name = this.xValueToNumber(v)
      if (name>=startN && name<=endN)
        values.add(v)
    }
    return values
  }

  distributionData(metricData: number[], partitions: number) {
    const lowest = Math.min(...metricData)
    const highest = Math.max(...metricData)
    const diff = highest-lowest
    const partSize = diff/partitions
    const arrays: number[][]= Array.from({ length: partitions }, () => []); // n empty arrays
    const processedData: {name: string; value: number;}[] = []
    var exceptionsCount = 0;

    console.log(`(GraphService) Calculating distribution with lowest=${lowest}, highest=${highest}, diff=${diff}, partSize=${partSize}`);

    for (const n of metricData){
      if (Math.floor((n-lowest)/partSize)>=partitions){
        exceptionsCount++;
        arrays[partitions-1].push(n)
      }
      else {
        arrays[Math.floor((n-lowest)/partSize)].push(n)
      }
    }
    for (let i = 0; i < partitions; i++) {
      processedData.push({name: (lowest+(i*partSize)).toFixed(1)+"-"+(lowest+(i+1)*partSize).toFixed(1), value: Math.floor(arrays[i].length/metricData.length*100)})
    }
    console.log("(GraphService) Distribution data calculated. Number of exceptions found: ", exceptionsCount);

    return processedData
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

  calcSplits(act: Activity, currentSplitDistance: number, currentSplitUnit: 'km'|'mi'):number[] {
    const goal = currentSplitUnit=='km'?currentSplitDistance:milesToKm(currentSplitDistance)
    let prevDp:DataPoint|undefined;
    let acc = 0;
    let accStart=new Date(act.dataPoints[0].time)
    const splits = []
    for (let dp of act.dataPoints) {
      if (prevDp==undefined){prevDp=dp;continue}
      acc+=totalDistance(prevDp, dp)
      if (acc>=goal) {
        const time = (new Date(dp.time).getTime()-accStart.getTime())/1000 //seconds
        const pace = Math.round(time/goal)
        splits.push(pace)
        acc=0;
        accStart=new Date(dp.time);

      }
      prevDp = dp
    }
    if (acc!=0) {
      const time = (new Date(act.dataPoints.at(-1)!.time).getTime()-accStart.getTime())/1000 //seconds
      const pace = Math.round(time/goal)
      splits.push(pace)
    }
    return splits
  }
}

export type NameValue = {name: string; value: number}
export type NamedSeries = {name: string; series: NameValue[]}

function totalDistance(dp1:DataPoint, dp2:DataPoint) {
  return totalDistanceLong(dp1.lat,dp1.lon,dp2.lat,dp2.lon);
}
function totalDistanceLong(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371; // Radius of the Earth in km
  const dLat = deg2rad(lat2 - lat1);
  const dLon = deg2rad(lon2 - lon1);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(deg2rad(lat1)) *
      Math.cos(deg2rad(lat2)) *
      Math.sin(dLon / 2) ** 2;
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c; // distance in km
}

function deg2rad(deg: number): number {
  return deg * (Math.PI / 180);
}

function milesToKm(m:number):number {
  return m*1.60934
}
