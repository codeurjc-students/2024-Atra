import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class ActivityService {
  constructor(private http: HttpClient, private router: Router) { }

  uploadGPX(file: File) {
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
}
