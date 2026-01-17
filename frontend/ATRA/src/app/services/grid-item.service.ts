import { Route } from './../models/route.model';
import { BehaviorSubject, combineLatest, Observable } from 'rxjs';
import { Injectable } from '@angular/core';
import { AlertService } from './alert.service';
import { Mural } from '../models/mural.model';
import { Activity } from '../models/activity.model';
import { User } from '../models/user.model';
import { HttpClient } from '@angular/common/http';
import { FormattingService } from './formatting.service';

@Injectable({
  providedIn: 'root'
})
export class GridItemService {
  mural: Mural | null = null; // This will hold the current mural if needed
  user: User | null = null; // This will hold the current mural if needed

  private activityList: BehaviorSubject<Activity[] | null> = new BehaviorSubject<Activity[] | null>(null);
  private members: BehaviorSubject<{name:string; id:number}[] | null> = new BehaviorSubject<{name:string; id:number}[] | null>(null);
  private routes: BehaviorSubject<Route[] | null>  = new BehaviorSubject<Route[] | null>(null);

  rowValues: Map<ComponentType, BehaviorSubject<string[][]|null>> = new Map<ComponentType, BehaviorSubject<string[][]|null>>([
    ['members', new BehaviorSubject<string[][] | null>(null)],
    ['records', new BehaviorSubject<string[][] | null>(null)],
    ['info', new BehaviorSubject<string[][] | null>(null)],
    ['activities', new BehaviorSubject<string[][] | null>(null)],
    ['routes', new BehaviorSubject<string[][] | null>(null)]
  ])

  rowLinks: Map<ComponentType, BehaviorSubject<string[]|null>> = new Map<ComponentType, BehaviorSubject<string[]|null>>([
    ['members', new BehaviorSubject<string[] | null>(null)],
    ['records', new BehaviorSubject<string[] | null>(null)],
    ['info', new BehaviorSubject<string[] | null>(null)],
    ['activities', new BehaviorSubject<string[] | null>(null)],
    ['routes', new BehaviorSubject<string[] | null>(null)]
  ])

  setEntity(mural: Mural | null, user: User | null) {
    this.mural = mural;
    this.user = user;
    if (mural==null && user==null) throw new Error("GridItemService.setEntity called with both mural and user as null. At least one must be provided.");
    const entity = (mural || user)!; //use entity.X instead of mural.X or user.X Make sure User has .activities and .routes (it doesn't now). This way we avoid repeating the fetching and setting code.
    if (mural) {
      this.members.next(mural.members);
    }
    this.fetchActivities(mural, user).subscribe({
        next: (activities: Activity[]) => {
          this.activityList.next(activities)
                },
        error: (err) => {
          this.alertService.toastError("Some functionality won't be available. Try reloading the page.", "Error fetching activities");
        }
      })
    this.fetchRoutes(mural, user).subscribe({
      next: (routes: Route[]) => {
        this.routes.next(routes)
      },
      error: (err) => {
        this.alertService.toastError("Some functionality won't be available. Try reloading the page.", "Error fetching routes");
      }
    })
    this.populateRowValues()
  }
  populateRowValues() {
    this.populateMembers();
    this.populateRecords();
    this.populateInfo();
    this.populateActivities();
    this.populateRoutes();
  }
  private populateMembers() {
    combineLatest([this.activityList, this.members]).subscribe({
      next: ([activities, members]) => {
        if (activities == null || members == null) {
          console.warn("Trying to populate members with null activities or members");
          return;
        }
        const memberValues: string[][] = [];
        for (var member of members) {
          const columnNames = this.getColNames("members");

          var currentRow: string[] = Array(columnNames.length).fill("");

          //calculate values for each column
          const userActivities = activities.filter(activity => activity.user.id === member.id)//.reduce((sum, activity) => sum + (activity.summary?.totalTime||0), 0);
          const name = member.name || "Unknown"; // Fallback in case name is null or undefined
          const activityCount = userActivities.length;
          var totalTime = 0;
          var totalDistance = 0;
          for (const a of userActivities) {
            if (!a.summary) console.warn("Activity with no summary in populateMembers");
            totalTime += a.summary?.totalTime || 0; // Use optional chaining to avoid errors if summary is null
            totalDistance += a.summary?.totalDistance || 0; // Use optional chaining to avoid errors if summary is null
          }

          // Fill currentRow with values
          currentRow[columnNames.indexOf("Name")] = name;
          currentRow[columnNames.indexOf("Total Time")] = FormattingService.formatTime(totalTime);
          currentRow[columnNames.indexOf("Total Distance")] = totalDistance.toFixed(2); // Assuming distance is in km
          currentRow[columnNames.indexOf("# of Activities")] = activityCount.toString();

          // Add the current row to memberValues
          memberValues.push(currentRow);
        }
        this.rowValues.get('members')?.next(memberValues);
      }
    });
  }
  private populateRecords() {
    this.activityList.subscribe({
      next: (activities) => {
        this.rowValues.get('records')?.next(this.calcRecords(activities));
      }
    })

  }
  calcRecords(activities: Activity[]|null, ignoreIds:boolean=true): string[][] {
    //['Category', 'Best', 'User']
     if (activities == null) throw new Error("Trying to populate records with null activities");

     const recordValues: string[][] = [];
     const columnNames = this.getColNames("records");

     // Assuming we want to find the best record for each distance
     const records: Record<string, [string,string,string,number]> = {}; //category: best | user
     for (const activity of activities) {
       if (!activity.summary || !activity.summary.records) continue; // Skip activities without summary
       Object.entries(activity.summary.records).forEach(([key, value]) => {

         const currentValue = Number(value);

         if (records[key] == null) {
           records[key] = [value, activity.user.name,activity.name, activity.id]; // Update the record if it didn't exist
           return; //continue;
         }
         const previousValue = Number(records[key][0]);

         if (Number.isNaN(previousValue)) throw new Error(`Previous value for key ${key} is NaN (${records[key][0]}). This should not happen.`);

         if (previousValue==-1 || ((key.includes("km") || key.includes("mile")) && currentValue!=-1 && currentValue < previousValue)) {
           records[key] = [value, activity.user.name,activity.name, activity.id]; // Update if the current record is better (lower time)
         } else if ((key.includes("min") || key.includes("hour")) && currentValue > previousValue) {
           records[key] = [value, activity.user.name,activity.name, activity.id]; // Update if the current record is better (longer distance)
         }
       })
     }
    // found the records for each category. Now we format them pretty, and order them
    for (const [category, [best, by, inAct, actId]] of Object.entries(records)) {
      if (Number(best) == -1) { // -1 is used to indicate no record
        records[category] = ["-", "-","-",-1]; //Quisiera que aparezcan al final, o que no aparezcan y punto
        continue;
      }
      if ((category.includes("km") || category.includes("mile"))) {
        records[category] = [FormattingService.formatTime(Number(best)), by, inAct, actId];
      } else if ((category.includes("min") || category.includes("hour"))) {
        records[category] = [FormattingService.formatDistance(Number(best)), by, inAct, actId];
      }
    }
    const sortedEntries = Object.entries(records).sort(([keyA], [keyB]) => {
      const priority: Record<string, number> = {km: 1,mile: 2,min: 3,hour: 4};
      const parseKey = (key: string) => {
        const match = key.match(/^(\d+)(km|mile|min|hour)$/);
        if (!match) throw new Error("Invalid key format: " + key);
        return { num: Number(match[1]), unit: match[2] };
      };

      const a = parseKey(keyA);
      const b = parseKey(keyB);

      // First compare by unit
      if (a.unit !== b.unit) {
        return priority[a.unit] - priority[b.unit];
      }

      // Then compare numerically
      return a.num - b.num;
    });
    // found the records for each category, now we need to populate the recordValues
    for (const [category, [best, by, inAct, actId]] of sortedEntries) {
      const currentRow: string[] = Array(columnNames.length).fill("");
      currentRow[columnNames.indexOf("Category")] = category;
      currentRow[columnNames.indexOf("Best")] = best;
      currentRow[columnNames.indexOf("User")] = by;
      currentRow[columnNames.indexOf("Activity")] = inAct;
      currentRow.push(actId.toString())

      recordValues.push(currentRow);
    }

    if (this.mural){
      this.rowLinks.get('records')?.next(recordValues.map(record => `/murals/${this.mural!.id}/activities/${record.at(-1)}`));
    } else if (this.user) {
      this.rowLinks.get('records')?.next(recordValues.map(record => `/me/activities/${record.at(-1)}`));
    }
    if (ignoreIds) return recordValues.map(v=>v.slice(0,-1))

    //now to set recordValues into rowValues
    return recordValues;

    //if (this.mural) {
    //  this.rowLinks.get('records')?.next(records.map(record => `/murals/${this.mural!.id}/records/${record[0]}`));
    //} else if (this.user) {
    //  this.rowLinks.get('records')?.next(records.map(record => `/me/records/${record[0]}`));
    //}
  }
  private populateInfo() {
    // which holds name, description, owner, etc, and is clickable.
    // When clicked, everything is displayed in better detail, and the owner is given options to add/delete members, change owner, and delete the mural.
    //only two columns: name, value
    //fields: name, desc, owner, # of members, # of activities, # of routes, total distance, total time, avg pace, max distance, max time
    combineLatest([this.activityList, this.members, this.routes]).subscribe({
      next: ([activities, members, routes])=> {
        const rows:string[][] = []
        if (this.mural) rows.push(["Name",this.mural.name])
        if (this.mural) rows.push(["Owner",this.mural.owner.name])
        if (members)    rows.push(["# of members", members.length.toString()])
        if (routes)     rows.push(["# of routes", routes.length.toString()])

        if (activities){
          rows.push(["# of activities", activities.length.toString()])
          rows.push(["Total Distance", FormattingService.formatDistance(activities.reduce((sum, activity) => sum + (activity.summary?.totalDistance || 0), 0)) || "0 km"])
          rows.push(["Total Time",FormattingService.formatTime(activities?.reduce((sum, activity) => sum + (activity.summary?.totalTime || 0), 0) || 0)])
          rows.push(["Avg Pace",activities.length ? FormattingService.formatPace(activities.reduce((sum, activity) => sum + (activity.summary?.averages?.['pace'] || 0), 0) / activities.length) : "No avg pace"])
          rows.push(["Max Distance",FormattingService.formatDistance(activities.reduce((sum,activity) => activity.summary!.totalDistance>sum.summary!.totalDistance ? activity:sum).summary!.totalDistance)])
          rows.push(["Max Time",FormattingService.formatTime(activities.reduce((sum,activity) => activity.summary!.totalTime>sum.summary!.totalTime ? activity:sum).summary!.totalTime)])
        }

        if (this.user) rows.push(["username",this.user.username])
        if (this.user) rows.push(["displayname",this.user.name])
        this.rowValues.get("info")?.next(rows);
      }
    })

  }
  private populateActivities() {
    //['Name', 'Date', 'Avg Pace', 'Distance', 'Time'];
    this.activityList.subscribe({
      next: (activities) => {
        if (activities == null) throw new Error("GridItemService.populateActivities called with null activities.");
        const activityValues: string[][] = [];
        const columnNames = this.getColNames("activities");
        for (const activity of activities) {
          if (!activity.summary) throw new Error("Found an activity with no summary in GridItemService.populateActivities().");
          //añadir sus valores a activityValues
          const currentRow: string[] = Array(columnNames.length).fill("");
          currentRow[columnNames.indexOf("Name")] = activity.name;
          currentRow[columnNames.indexOf("Date")] = new Date(activity.startTime).toISOString().split("T")[0]; // Format date as YYYY-MM-DD
          currentRow[columnNames.indexOf("Avg Pace")] = activity.summary.averages?.['pace'] ? FormattingService.formatPace(activity.summary.averages?.['pace']) : "No avg pace";
          currentRow[columnNames.indexOf("Distance")] = FormattingService.formatDistance(activity.summary.totalDistance);
          currentRow[columnNames.indexOf("Time")] = FormattingService.formatTime(activity.summary.totalTime);
          activityValues.push(currentRow);
        }

        this.rowValues.get('activities')?.next(activityValues);

        if (this.mural){
          this.rowLinks.get('activities')?.next(activities.map(activity => `/murals/${this.mural!.id}/activities/${activity.id}`));
        } else if (this.user) {
          this.rowLinks.get('activities')?.next(activities.map(activity => `/me/activities/${activity.id}`));
        }

      }
    })
  }
  private populateRoutes() {
    //['Name', 'Efforts', 'Distance', 'Best Time | By'] //consider in the future avg pace and time

    combineLatest([this.activityList, this.routes]).subscribe({
      next: ([activities, routes]) => {
        if (routes == null || activities == null) throw new Error("GridItemService.populateRoutes called with null activities and/or null routes.");
        const routeValues: string[][] = [];
        const columnNames = this.getColNames("routes");
        for (const route of routes) {
          //añadir sus valores a activityValues
          const currentRow: string[] = Array(columnNames.length).fill("");

          //calc best time | by
          const bestActivity = activities.reduce((best, current) => {
            if (current.summary==null || best.summary==null) throw new Error("GridItemService.activities has activities with no summary.");
            else return current.summary.totalTime > best.summary.totalTime ? current : best
          });

          //add values to currentRow
          currentRow[columnNames.indexOf("Name")] = route.name;
          currentRow[columnNames.indexOf("Distance")] = FormattingService.formatDistance(route.totalDistance);
          currentRow[columnNames.indexOf("Efforts")] = activities.filter(a=> a.route?.id === route.id).length.toString(); //copilot suggested using ids. I'd've used the route itself, but considering there are different dtos, this is better
          currentRow[columnNames.indexOf("Best Time | By")] = `${FormattingService.formatTime(bestActivity.summary!.totalTime, 1)} | ${bestActivity.user.name}`;


          routeValues.push(currentRow);
        }
        this.rowValues.get('routes')?.next(routeValues);

        if (this.mural) {
          this.rowLinks.get('routes')?.next(routes.map(route => `/murals/${this.mural!.id}/routes?selected=${route.id}`));
        } else if (this.user) {
          this.rowLinks.get('routes')?.next(routes.map(route => `/me/routes?selected=${route.id}`)); //not sure if selected will be honored. Another option is to create /routes/id/details
        }
     }
    })

  }

  data: Map<ComponentType, string[][]> = new Map<ComponentType, string[][]>();

  constructor(private alertService:AlertService, private http: HttpClient) {}

  fetchActivities(mural: Mural | null, user: User | null): Observable<Activity[]> {
    //should fetch to /murals/:id/activities or /users/:id/activities, no need to pass the list
    //thus, we can omit the argument (we were just receiving entity.activities, we'd be doing the same thing)
    if (mural!=null) return this.http.get<Activity[]>('/api/activities?from=mural&id='+mural.id+"&fetchAll=true")
    if (user!=null) return this.http.get<Activity[]>('/api/activities?from=user&id='+user.id+"&fetchAll=true")
    //if both are null
    throw new Error("GridItemService.fetchActivities called with null mural and user."); //shouldn't come to this, it should be caught above
  }

  fetchRoutes(mural: Mural | null, user: User | null) : Observable<Route[]> {
    if (mural!=null) return this.http.get<Route[]>('/api/routes?from=mural&id='+mural.id+'&type=noActivities')
    if (user!=null) return this.http.get<Route[]>('/api/routes?from=user&id='+user.id+'&type=noActivities')
    //if both are null
    throw new Error("GridItemService.fetchRoutes called with null mural and user."); //shouldn't come to this, it should be caught above

  }

  getColNames(type:ComponentType): string[] {
    const colNames = new Map<string,string[]>([
      ['members',    ['Name', 'Total Time', 'Total Distance', '# of Activities']],
      ['records',    ['Category', 'Best', 'User', 'Activity']],
      ['info',       ['Name', 'Value']], //or maybe [Name, Best|By, Avg]
      ['activities', ['Name', 'Date', 'Avg Pace', 'Distance', 'Time']],
      ['routes',     ['Name', 'Efforts', 'Distance', 'Best Time | By']]
    ])
    return colNames.get(type)!
  }

  getRowValues(type:ComponentType): BehaviorSubject<string[][] | null> {
    const value = this.rowValues.get(type);
    if (!value) throw new Error(`No data found for type: ${type}`);
    return value;
  }

  getRowLinks(type:ComponentType): BehaviorSubject<string[] | null> {
    const value = this.rowLinks.get(type);
    if (value) return value;

    throw new Error('Invalid ComponentType for getRowLinks: ' + type);
  }

  forget() {
    this.mural = null;
    this.user = null;
    this.activityList.next(null);
    this.members.next(null);
    this.routes.next(null);
    this.rowValues.forEach(value => value.next(null));
    this.rowLinks.forEach(value => value.next(null));
  }

}



export type ComponentType = 'members'
                          | 'records'
                          | 'info'
                          | 'activities'
                          | 'routes';

