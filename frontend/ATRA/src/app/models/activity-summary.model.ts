export class ActivitySummary {

    id: number;
    startTime: Date; // ISO string, e.g. Instant.toString()
    totalDistance: number;
    totalTime: number; // seconds
    elevationGain: number;

    averages?: Record<string, number>;
    records?: Record<string, string>;


    constructor(summary: any) {
      this.id = summary.id;
      this.startTime = new Date(summary.startTime);
      this.totalDistance = summary.totalDistance;
      this.totalTime = summary.totalTime; // seconds
      this.elevationGain = summary.elevationGain;
      this.averages = summary.averages;
      this.records = summary.records;
  }
}
