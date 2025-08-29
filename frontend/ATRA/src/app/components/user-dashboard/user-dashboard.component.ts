import { AuthService } from './../../services/auth.service';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { GridTableComponent } from '../stat-components/records/grid-table.component';
import { User } from '../../models/user.model';
import { UserService } from '../../services/user.service';
import { ActivatedRoute } from '@angular/router';
import { AlertService } from '../../services/alert.service';
import { GridItemService } from '../../services/grid-item.service';

@Component({
  selector: 'app-user-dashboard',
  standalone: true,
  imports: [GridTableComponent],
  templateUrl: './user-dashboard.component.html',
  styleUrl: './user-dashboard.component.scss'
})
export class UserDashboardComponent implements OnInit, OnDestroy{
  user: User | null = null;

  constructor(private authService:AuthService, private route: ActivatedRoute, private alertService:AlertService, private gridItemService:GridItemService){}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.gridItemService.setEntity(null, this.authService.user.value)

    setTimeout(() => {
      // Allows scrolling. On a timeout, to avoid ngOnDestroy of another component from overwriting it
      document.body.style.overflow = 'auto';
      document.documentElement.style.overflow = 'auto';
    }, 500);
  }

  ngOnDestroy(): void {
    this.gridItemService.forget()
    document.body.style.overflow = 'hidden';
    document.documentElement.style.overflow = 'hidden';
  }


}
