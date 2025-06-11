import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { Mural } from '../../models/mural.model';
import { CommonModule, Location } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { MuralService } from '../../services/mural.service';

@Component({
  selector: 'app-murals-category',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './murals-category.component.html',
  styleUrl: './murals-category.component.css'
})
export class MuralsCategoryComponent implements OnInit, OnDestroy {

  title!:string; //title:'Owned Murals' | 'Member Murals' | 'Other Murals' = "Owned Murals";
  murals:Mural[] | null = null;

  constructor(private muralService: MuralService, private router:Router, private location:Location) {}

  ngOnInit(): void {
    const category = this.location.path().split("/")[2];
    if (!['owned', 'member', 'other'].includes(category) || category == null) {
      console.error("MuralsComponent called with invalid path: " + this.location.path() + "\n'" + category + "' is not a valid argument. Valid arguments are: 'owned', 'member', 'other'" );
      this.router.navigate(['/murals']);
      return;
    }
    this.muralService.getData(category).subscribe(murals => this.murals = murals);

    setTimeout(() => {
      // Allows scrolling. On a timeout, to avoid ngOnDestroy of another component from overwriting it
      document.body.style.overflow = 'auto';
      document.documentElement.style.overflow = 'auto';
    }, 500);
  }

  ngOnDestroy(): void {
    document.body.style.overflow = 'hidden';
    document.documentElement.style.overflow = 'hidden';
  }

  errorLoadingImage($event: ErrorEvent) {
    const img = $event.target as HTMLImageElement;
    console.error("Error loading image:", img.id);
    img.src = MuralService.defaultThumbnail;
  }


}
