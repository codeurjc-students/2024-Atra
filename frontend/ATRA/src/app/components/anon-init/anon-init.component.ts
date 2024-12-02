import { UserService } from './../../services/user.service';
import { ActivityService } from './../../services/activity.service';
import { Component } from '@angular/core';

@Component({
  selector: 'app-anon-init',
  standalone: true,
  imports: [],
  templateUrl: './anon-init.component.html',
  styleUrl: './anon-init.component.css'
})
export class AnonInitComponent {
  constructor(private activityService: ActivityService, private userService: UserService){}

  onFileSelected(event: Event) {
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
            this.activityService.uploadGPX(file);
          }
        },
        error: (error) => {
          console.error('Error ', error);
          alert('Login failed. Please check your credentials and try again.');
        }
      });




    }
  }
}
