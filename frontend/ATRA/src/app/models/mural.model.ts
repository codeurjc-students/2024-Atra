import { Activity } from "./activity.model";
import { User } from "./user.model";

export interface Mural {

    id: number;
    name: string;
    description: string;

    code:string;

    owner: {name:string;id:number};
    members: {name:string;id:number}[];

    activities: {name:string;id:number}[];
    routes: {name:string;id:number}[];

    thumbnailUrl:string;
    thumbnail: Blob;
    bannerURL:string;
    banner: Blob;

}
