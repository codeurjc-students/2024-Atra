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
  constructor(private activityService: ActivityService){}

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    console.log("# of files: " + input.files?.length);
    if (input.files?.length) {
      const file = input.files[0]; // Access the first selected file
      console.log("file name: " + file.name); // Example: Print file name
      console.log("file type: " + file.type); // Example: Print file name

      this.activityService.uploadGPX(file);

    }
  }

}
