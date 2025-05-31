import { GridItemService } from '../../services/grid-item.service';
import { GridTableComponent } from '../stat-components/records/grid-table.component';
import { ActivatedRoute } from '@angular/router';
import { Mural } from '../../models/mural.model';
import { MuralService } from '../../services/mural.service';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { AlertService } from '../../services/alert.service';

@Component({
  selector: 'app-murals-dashboard',
  standalone: true,
  imports: [GridTableComponent],
  templateUrl: './murals-dashboard.component.html',
  styleUrl: './murals-dashboard.component.css'
})
export class MuralsDashboardComponent implements OnInit, OnDestroy{
  mural!: Mural;

  constructor(private muralService:MuralService, private route: ActivatedRoute, private alertService:AlertService, private gridItemService:GridItemService){}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    console.log(id);

    this.muralService.getMural(id).subscribe({
      next:(mural:Mural)=> {console.log("MURAL: " + mural);
       this.mural=mural
       this.gridItemService.setEntity(mural, null)
      },
      error:(err)=>this.alertService.alert("Something went wrong fetching the Mural. Try reloading the page. If the error persists, try again later", "Something went wrong!") //could reload the page on dismiss
    })
    // Allows scrolling
    document.body.style.overflow = '';
    document.documentElement.style.overflow = '';
  }

  ngOnDestroy(): void {
    document.body.style.overflow = 'hidden';
    document.documentElement.style.overflow = 'hidden';
  }

}
