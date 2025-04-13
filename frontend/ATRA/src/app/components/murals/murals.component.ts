import { Component, ElementRef, EventEmitter, Input, OnDestroy, OnInit, Output, ViewChild } from '@angular/core';
import { Mural } from '../../models/mural.model';
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
  styleUrl: './murals.component.css'
})
export class MuralsComponent implements OnInit {


  ownedMurals!: Mural[];
  memberMurals!: Mural[];
  otherMurals!: Mural[];

  title:'Owned Murals' | 'Member Murals' | 'Other Murals' = "Owned Murals";
  component: 'list' | 'category' = "list";
  selectedMurals: Mural[] | null = null;

  constructor(private muralService: MuralService, private location:Location, private router:Router) {}

  ngOnInit(): void {
    //check if the route is /murals/:category, and if so, that the category is valid
    const currentUrlParts = this.location.path().split("/")
    if (currentUrlParts.length==3) {
      if (!['owned', 'member', 'other'].includes(currentUrlParts[2])) {
        console.log("MuralsComponent called with invalid path: " + this.location.path() + "\n'" + currentUrlParts[2] + "' is not a valid argument. Valid arguments are: 'owned', 'member', 'other'" );
        this.router.navigate(["/murals"])
        return
      }
    }

    //we're either on /murals, or /murals/:category with a valid category
    this.muralService.getOwned().subscribe({
      next: (murals) =>{
        this.ownedMurals = murals;
        if (currentUrlParts.length==3 && currentUrlParts[2]=="owned") this.urlChanged(this.location.path())
      }
    })
    this.muralService.getMember().subscribe({
      next: (murals) =>{
        this.memberMurals = murals;
        if (currentUrlParts.length==3 && currentUrlParts[2]=="member") this.urlChanged(this.location.path())
      }
    })
    this.muralService.getOther().subscribe({
      next: (murals) =>{
        this.otherMurals = murals;
        if (currentUrlParts.length==3 && currentUrlParts[2]=="other") this.urlChanged(this.location.path())
      }
    })

    this.location.onUrlChange((url,state)=>{this.urlChanged(url)})
  }

  urlChanged(newUrl:string) {
    const urlParts = newUrl.split("/")
    if (urlParts.length==2) {
      this.component = "list";
      this.selectedMurals = []
    }
    else if (urlParts.length==3) {
      this.component = 'category';

      const category = urlParts[2]
      if (category === 'owned') {
        this.selectedMurals = this.ownedMurals;
        this.title = 'Owned Murals';
      }
      else if (category === 'member') {
        this.selectedMurals = this.memberMurals;
        this.title = 'Member Murals';
      }
      else {
        this.selectedMurals = this.otherMurals;
        this.title = 'Other Murals';
      }
    }
  }

  seeAllClicked($event: 'owned'|'member'|'other') {
    this.location.go("/murals/"+$event)
  }
}
