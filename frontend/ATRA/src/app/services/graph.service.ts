import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class GraphService {
  calc(name: string, data:number[], prevValue:string|number=""): string | number {
    if (name==="σ") return this.getDeviation(data)
    return prevValue
  }

constructor() { }

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



}
