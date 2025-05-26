import { Activity } from "./activity.model";
import { User } from "./user.model";

export interface Mural {

    id: number;
    name: string;
    description: string;

    owner: User;
    members: User[];

    activities: Activity[];

    thumbnailUrl:string;
    thumbnail: Blob;
    bannerURL:string;
    banner: Blob;

}
