import { AlertService } from './../../services/alert.service';
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
  constructor(private activityService: ActivityService, private alertService:AlertService){}

  onFileSelected(event: Event){
    this.activityService.uploadActivity(event)
  }
}
