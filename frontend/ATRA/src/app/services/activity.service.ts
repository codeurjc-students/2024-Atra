import { AlertService } from './alert.service';
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Activity } from '../models/activity.model';
import { BehaviorSubject, catchError, map, Observable, of, tap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class ActivityService {

  makeActivitiesNotVisibleToMural(id: number, selectedActivities: Set<number> | null) {
    if (selectedActivities==null) throw new Error("(ActivityService) makeActivitiesNotVisibleToMural called with null selectedActivities");
    return this.http.patch("/api/activities/visibility/mural?id="+id, Array.from(selectedActivities)/*,   { headers: { 'Content-Type': 'application/json' } }*/)
  }

  getCoordinates(activity: Activity): [number, number][] {
    return activity.getStream("position").map((x:string)=>{
      const parts = x.split(";")
      const lat = parseFloat(parts[0])
      const lon = parseFloat(parts[1])

      return [lat, lon]
    })
  }

  validMetrics: string[] = ["timeElapsed", "timeOfDay", "totalDistance"]

  constructor(private http: HttpClient, private router: Router, private authService: AuthService, private alertService:AlertService) {
    this.currentActivity.subscribe((a)=>console.log("(ActivityService) Cached activity updated: ", a));
  }

  uploadActivity(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files?.length) {
      const file = input.files[0]; // Access the first selected file

      if (!file.name.toLowerCase().endsWith(".gpx")){
        this.alertService.toastError("File upload failed. The file must be a gpx file!")
        return
      }
      this.authService.isLoggedIn().subscribe(
        (isLoggedIn:boolean) => {
          if (isLoggedIn) this.uploadGPX(file)
          else {
            console.log('(ActivityService) Attempted to upload a file without logging in');
            this.alertService.alert("Couldn't verify if you are logged in. You need to be logged in to access this functionality. We are working to make it available without login. Try again later.", "An unexpected error ocurred")
          }
      });
    }
  }

  private uploadGPX(file: File) {
    const formData = new FormData();
    formData.append('file', file);
    this.http.post('/api/activities', formData).subscribe({
      next: (activity:any) => {
        this.alertService.confirm("Activity has been added. Do you want to see it?", "Activity created successfully").subscribe(
          (accepted) => {
            if (accepted)
              this.router.navigate(["/me/activities/", activity.id])
            //else reload current page
            else this.alertService.alert("You might need to refresh the current page for it to show data from the new activity", "Be advised")
          }
        )
      },
      error: (error) => {
        if (error.status == 413) {
          this.alertService.toastError("Upload failed! The file uploaded exceeds the 10MB limit.", "File too big")
        } else {
          console.error("(ActivityService.uploadGPX) Error uploading activity: ", error);
          this.alertService.toastError("Upload failed! Try again later")
        }
      }
    });
  }

  getAuthenticatedUserActivities(fetchAll:boolean, startPage:number=0, pagesToFetch:number=1,pageSize:number=10){
    return this.http.get<any[]>(`/api/activities?from=authUser&fetchAll=${fetchAll}&startPage=${startPage}&pagesToFetch=${pagesToFetch}&pageSize=${pageSize}`, {observe:'response'});
  }

  getMuralActivities(id: string, fetchAll:boolean, startPage:number=0, pagesToFetch:number=1,pageSize:number=20) {
    return this.http.get<any[]>(`/api/activities?from=mural&id=${id}&fetchAll=${fetchAll}&startPage=${startPage}&pagesToFetch=${pagesToFetch}&pageSize=${pageSize}`, {observe:'response'});
  }


  currentActivity: BehaviorSubject<Activity | null> = new BehaviorSubject<Activity | null>(null);
  readonly CACHE_DURATION: number = 1000 * 30; // 30 seconds for testing, should bump to 1000 * 60 * 5 for 5 minutes in production
  loadingActivity: boolean = false;
  timeout: number | undefined = undefined;


  process(value: any[]): Activity[] {
    var result: Activity[] = [];
    value.forEach(activity => {
      result.push(new Activity(activity));
    });

    return result;
  }

  process1(value: any): Activity {
    return new Activity(value);
  }

  get(id: number, muralId?:string|null): Observable<Activity>{
    const currentValue = this.currentActivity.getValue();
    if (currentValue==null || currentValue?.id!=id) {
      this.loadingActivity=true;
      window.clearTimeout(this.timeout);
      const muralAddOn = muralId==null ? "":"?mural="+muralId
      return this.http.get<Activity>("/api/activities/" + id + muralAddOn).pipe(
        map((a)=>{this.currentActivity.next(this.process1(a));this.loadingActivity=false;this.timeout=window.setTimeout(()=>{this.currentActivity.next(null);this.timeout=undefined;},this.CACHE_DURATION);return this.process1(a)}),
        catchError((e) => {
          this.loadingActivity=false;
          return throwError(()=>e)
        })
      );
    }
    return of(currentValue!);
  }
  getActivities(id:number[], muralId?:number){
    const m = muralId ? "&muralId="+muralId:""
    return this.http.get<Activity[]>("/api/activities/byIds?ids="+id.toString()+m, {observe:'response'})
  }

  removeRoute(id: number){
    return this.http.delete<Activity>("/api/activities/" + id + "/route");
  }

  addRoute(activityId: number, routeId:number){
    return this.http.post<Activity>("/api/activities/" + activityId + "/route", routeId);
  }

  delete(id:number): Observable<string> {
    return this.http.delete<string>("/api/activities/"+id);
  }

  // internal logic and services



  getMetric(metric: string, activity:Activity){
    const processedData: string[] = []
    if (metric=="timeElapsed") {
      const startTime = new Date(activity.streams.time[0]);

      for (var i=0;i<activity.streams.time.length;i++) {
        const currentTime = new Date(activity.streams.time[i])
        processedData.push(this.getTime(currentTime, startTime))
      }
      return processedData

    } else if (metric=="timeOfDay") {
      for (var i=0;i<activity.streams.time.length;i++) {
        const currentTime = new Date(activity.streams.time[i])
        processedData.push(currentTime.getHours().toString()+":"+currentTime.getMinutes().toString()+":"+currentTime.getSeconds().toString())
      }
      return processedData
    } else if (metric=="totalDistance"){
      for (var i=0;i<activity.streams.time.length;i++)
        parseFloat(activity.streams.distance[i]).toFixed(2)
    }
    return activity.getStream(metric)
  }


  getTime(date1:Date, date2:Date): string{ //utils
    // Time Difference in Milliseconds
    const milliDiff: number = date1.getTime() - date2.getTime();
    // Total number of seconds in the difference
    const totalSeconds = Math.floor(milliDiff / 1000);
    // Total number of minutes in the difference
    const totalMinutes = Math.floor(totalSeconds / 60);
    // Total number of hours in the difference
    const totalHours = Math.floor(totalMinutes / 60);
    // Getting the number of seconds left in one minute
    const remSeconds = totalSeconds % 60;
    // Getting the number of minutes left in one hour
    const remMinutes = totalMinutes % 60;

    const hoursString = totalHours != 0 ? totalHours.toString()+":":""
    const minsString = (remMinutes < 10 && totalHours!=0) ? "0"+remMinutes.toString():remMinutes.toString()
    const secsString = remSeconds < 10 ? "0"+remSeconds.toString():remSeconds.toString()


    return `${hoursString}${minsString}:${secsString}`

  }


  getWithNoRoute() {
    return this.http.get<any[]>("/api/activities?fetchAll=true&cond=nullRoute").pipe(
      map((reply: any[]) => {
        return this.process(reply)
      }),
      catchError(error => {
        console.error("(ActivityService) Couldn't fetch activities. Error: "+error);
        return []
      })
    );
  }

  changeVisibility(id: number, newVis: ActivityVisibility, allowedMuralsList:number[]) {
    if (isActivityVisibility(newVis) == false) throw new Error("WHAT. THE. FUCK. How the fuck is newVis this value: " + newVis);
    return this.http.patch<Activity>("/api/activities/" + id + "/visibility", {visibility: newVis, allowedMuralsList:JSON.stringify(allowedMuralsList)}).pipe(tap({
      next: (a:Activity) => {
        this.currentActivity.next(this.process1(a));
        this.timeout = window.setTimeout(() => {
            this.currentActivity.next(null);
            this.timeout=undefined;
          },
          this.CACHE_DURATION
        );

        //To update locally
        //const activity = this.currentActivity.getValue()
        //if (activity==null) return;
        //activity.visibility = newVis;
        //this.currentActivity.next(activity)
      },
      error: (err) => { //though this should be handled by the backend and the interceptor
        this.alertService.toastError("Error changing visibility");
      }
    }))
  };

  editName(id: number, newName: string) {
    return this.http.patch<Activity>("/api/activities/"+id,{name:newName})
  }
}

type ActivityVisibility = "PRIVATE" | "MURAL_SPECIFIC" | "MURAL_PUBLIC" | "PUBLIC";

function isActivityVisibility(value: string): value is ActivityVisibility {
  return ["PRIVATE", "MURAL_SPECIFIC", "MURAL_PUBLIC", "PUBLIC"].includes(value);
}
