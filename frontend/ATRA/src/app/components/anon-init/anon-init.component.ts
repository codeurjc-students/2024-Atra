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
  constructor(private activityService: ActivityService){}

  onFileSelected(event: Event){
    this.activityService.uploadActivity(event)
  }
}
