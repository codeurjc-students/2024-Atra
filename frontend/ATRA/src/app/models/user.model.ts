export interface User {
    id: number;
    username: string;
    password: string;

    name: string;
    email: string;

    roles: string[];

    activities?: { name: string; id: number }[];
    routes?: { name: string; id: number }[];
    murals?: { name: string; id: number }[];

}
