import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { UserService } from './user.service';
import { Activity } from '../models/activity.model';

@Injectable({
  providedIn: 'root'
})
export class ActivityService {
  constructor(private http: HttpClient, private router: Router, private userService: UserService) {}

  uploadActivity(event: Event) {
    const input = event.target as HTMLInputElement;
    console.log("# of files: " + input.files?.length);
    if (input.files?.length) {
      const file = input.files[0]; // Access the first selected file
      console.log("file name: " + file.name); // Example: Print file name

      if (!file.name.toLowerCase().endsWith(".gpx")){
        alert("File upload failed. The file must be a gpx file!")
        return
      }
      this.userService.isLoggedIn().subscribe({
        next: (response) => {
          if (!response) {
            alert("Sorry, but you need to be logged in to upload a file. In the future we will make this feature available without a login")
            return
          } else {
            this.uploadGPX(file);
          }
        },
        error: (error) => {
          console.error('Error ', error);
          alert('Login failed. Please check your credentials and try again.');
        }
      });
    }
  }

  private uploadGPX(file: File) {
    const formData = new FormData();
    formData.append('file', file);
    this.http.post('/api/activities', formData).subscribe({
      next: () => {
        this.router.navigate(["/activity-view"])
      },
      error: (error) => {
        if (error.status == 413) {
          alert("Upload failed! The file uploaded exceeds the 10MB limit.")
        } else {
          alert("Upload failed! Try again later")
        }
      }
    });
  }

  getAuthenticatedUserActivities(){
    return this.http.get<Activity[]>("/api/activities")
  }

  process(value: any[]): Activity[] {
    var result: Activity[] = [];
    value.forEach(activity => {
      result.push(new Activity(activity));
    });

    return result;
  }
}
