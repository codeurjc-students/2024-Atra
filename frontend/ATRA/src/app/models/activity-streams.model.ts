export class ActivityStreams {
  static getGraphableKeys(): string[]{
    return ["altitude", "distance", "heartrate", "cadence", "pace", "elevation_gain"]
  }

  time : string[] = []
  distance : string[] = []
  position : string[] = []
  altitude : string[] = []
  elevation_gain : string[] = []
  heartrate : string[] = []
  cadence : string[] = []
  pace : string[] = []
  other: any[] = []

  constructor(){}


}
