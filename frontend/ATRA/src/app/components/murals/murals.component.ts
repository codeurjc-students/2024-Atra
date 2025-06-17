import { Component, OnInit } from '@angular/core';
import { MuralService } from '../../services/mural.service';
import { CommonModule, Location } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MuralsListComponent } from '../murals-list/murals-list.component';
import { MuralsCategoryComponent } from "../murals-category/murals-category.component";

@Component({
  selector: 'app-murals',
  standalone: true,
  imports: [CommonModule, RouterModule, MuralsListComponent, MuralsCategoryComponent],
  templateUrl: './murals.component.html',
  styleUrl: './murals.component.scss'
})
export class MuralsComponent implements OnInit {

  title:'Owned Murals' | 'Member Murals' | 'Other Murals' = "Owned Murals";
  component: 'list' | 'category' = "list";

  constructor(private muralService: MuralService, private location:Location, private router:Router) {}

  //this component can be deleted with very little consequence.
  //Effectively, it only serves to:
  // - show muralsList or MuralsCategory depending on the route, which normal routing would do
  // - and initialize the muralService caches with loadData(), which is not necessary since the first fetch of each will do that anyway.
  //I'm leaving it here for the feels mainly.
  ngOnInit(): void {
    this.muralService.loadData();

    //check if the route is /murals/:category, and if so, that the category is valid
    const currentUrlParts = this.location.path().split("/")
    if (currentUrlParts.length==3) {
      if (!['owned', 'member', 'other'].includes(currentUrlParts[2])) {
        console.log("MuralsComponent called with invalid path: " + this.location.path() + "\n'" + currentUrlParts[2] + "' is not a valid argument. Valid arguments are: 'owned', 'member', 'other'" );
        this.router.navigate(["/murals"])
        return
      }
      this.component = "category";
    }
  }
}
