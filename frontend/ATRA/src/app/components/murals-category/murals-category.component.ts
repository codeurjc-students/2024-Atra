import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { Mural } from '../../models/mural.model';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MuralService } from '../../services/mural.service';

@Component({
  selector: 'app-murals-category',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './murals-category.component.html',
  styleUrl: './murals-category.component.css'
})
export class MuralsCategoryComponent implements OnInit, OnDestroy {

  @Input() title!:string;
  @Input() murals!:Mural[];

  ngOnInit(): void {
    // Allows scrolling
    document.body.style.overflow = '';
    document.documentElement.style.overflow = '';
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
