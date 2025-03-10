import { Activity } from "./activity.model";

export interface Route {

    id: number;
    name: string;
    description: string;

    totalDistance: number;
    elevationGain: number;

    coordinates: [lat:number, lon:number][];

    activities: Activity[];

}
