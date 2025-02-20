export class ActivityStreams {
  static getGraphableKeys(): string[]{
    return ["altitude", "distance", "heartrate", "cadence", "pace"]
  }

  time : string[] = []
  distance : string[] = []
  position : string[] = []
  altitude : string[] = []
  heartrate : string[] = []
  cadence : string[] = []
  pace : string[] = []
  other: any[] = []

  constructor(){}


}
