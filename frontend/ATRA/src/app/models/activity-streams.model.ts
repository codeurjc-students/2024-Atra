export class ActivityStreams {
  static getGraphableKeys(): string[]{
    return ["altitude", "distance", "heartrate", "cadence"]
  }

  time : string[] = []
  distance : string[] = []
  position : string[] = []
  altitude : string[] = []
  heartrate : string[] = []
  cadence : string[] = []
  other: any[] = []

  constructor(){}


}
