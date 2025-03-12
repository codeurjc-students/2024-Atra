import { AlertService } from './alert.service';
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { UserService } from './user.service';
import { Activity } from '../models/activity.model';
import { catchError, map, Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ActivityService {
  getCoordinates(activity: Activity): [number, number][] {
    return activity.getStream("position").map((x:string)=>{
      const parts = x.split(";")
      const lat = parseFloat(parts[0])
      const lon = parseFloat(parts[1])

      return [lat, lon]
    })
  }

  validMetrics: string[] = ["timeElapsed", "timeOfDay", "totalDistance"]

  constructor(private http: HttpClient, private router: Router, private userService: UserService, private alertService:AlertService) {}

  uploadActivity(event: Event) {
    const input = event.target as HTMLInputElement;
    console.log("# of files: " + input.files?.length);
    if (input.files?.length) {
      const file = input.files[0]; // Access the first selected file
      console.log("file name: " + file.name); // Example: Print file name

      if (!file.name.toLowerCase().endsWith(".gpx")){
        this.alertService.alert("File upload failed. The file must be a gpx file!")
        return
      }
      this.userService.isLoggedIn().subscribe({
        next: (response) => {
          if (!response) {
            this.alertService.alert("Sorry, but you need to be logged in to upload a file. In the future we will make this feature available without login")
            return
          } else {
            this.uploadGPX(file);
          }
        },
        error: (error) => {
          console.error('Error ', error);
          this.alertService.alert('Login failed. Please check your credentials and try again.');
        }
      });
    }
  }

  private uploadGPX(file: File) {
    const formData = new FormData();
    formData.append('file', file);
    this.http.post('/api/activities', formData).subscribe({
      next: (activity:any) => {
        this.alertService.confirm("Activity has been added. Do you want to see it?").subscribe({
          next:(accepted) => {
            if (accepted)
              this.router.navigate(["/me/activity-view/", activity.id])
            //else reload current page
          }
        })
      },
      error: (error) => {
        if (error.status == 413) {
          this.alertService.alert("Upload failed! The file uploaded exceeds the 10MB limit.")
        } else {
          this.alertService.alert("Upload failed! Try again later")
        }
      }
    });
  }

  getAuthenticatedUserActivities(){
    return this.http.get<any[]>("/api/activities");
  }

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

  get(id: number){
    return this.http.get<any[]>("/api/activities/" + id);
  }

  removeRoute(id: number){
    return this.http.delete("/api/activities/" + id + "/route");
  }

  addRoute(activityId: number, routeId:number){
    return this.http.post("/api/activities/" + activityId + "/route", routeId);
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


  getAll(cond:string) {
    return this.http.get<any[]>("/api/activities").pipe(
      map((reply: any[]) => {
        console.log("processing");
        const activities = this.process(reply)
        console.log("cond: "+cond);
        if (cond=="routeIsNull") {
          console.log("Filtering");
          return activities.filter(act => act.route==null)
        }
        console.log("Did not filter");
        return activities
      }),
      catchError(error => {
        console.log("Couldn't fetch activities. Error: "+error);
        return []
      })
    );
  }
}
