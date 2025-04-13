import { MuralService } from './../../services/mural.service';
import { Component, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges } from '@angular/core';
import { Mural } from '../../models/mural.model';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { MuralsNewComponent } from '../murals-new/murals-new.component';

@Component({
  selector: 'app-murals-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './murals-list.component.html',
  styleUrl: './murals-list.component.css'
})
export class MuralsListComponent implements OnChanges {

  @Input() ownedMurals!: Mural[];
  @Input() memberMurals!: Mural[];
  @Input() otherMurals!: Mural[];

  ownedMuralsWindow!: Mural[];
  memberMuralsWindow!: Mural[];
  otherMuralsWindow!: Mural[];

  ownedMuralsOffset: number = 0;
  memberMuralsOffset: number = 0;
  otherMuralsOffset: number = 0;

  @Output() seeAllClicked:EventEmitter<'owned'|'member'|'other'> = new EventEmitter<'owned'|'member'|'other'>();

  constructor(private ngbModal:NgbModal, private muralService:MuralService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['ownedMurals'] && changes['ownedMurals'].currentValue!=null) {
      this.ownedMuralsWindow = changes['ownedMurals'].currentValue.slice(0,4);
    }
    if (changes['memberMurals'] && changes['memberMurals'].currentValue!=null) {
      this.memberMuralsWindow = changes['memberMurals'].currentValue.slice(0,4);
    }
    if (changes['otherMurals'] && changes['otherMurals'].currentValue!=null) {
      this.otherMuralsWindow = changes['otherMurals'].currentValue.slice(0,4);
    }
  }

  clicked(type: string, left:boolean) {
    if (type=="owned") {
      this.ownedMuralsOffset += left ? -1:1
      this.ownedMuralsWindow = this.ownedMurals.slice(0+this.ownedMuralsOffset,4+this.ownedMuralsOffset);
    } else if (type=="member") {
      this.memberMuralsOffset += left ? -1:1
      this.memberMuralsWindow = this.memberMurals.slice(0+this.memberMuralsOffset,4+this.memberMuralsOffset);
    } else if (type=="other") {
      this.otherMuralsOffset += left ? -1:1
      this.otherMuralsWindow = this.otherMurals.slice(0+this.otherMuralsOffset,4+this.otherMuralsOffset);
    }
  }

  createMural(){
    const modal = this.ngbModal.open(MuralsNewComponent);
    modal.result.then(
      (result) => {
        if (result=="reloadOwnedAndMember") {
          this.reloadOwnedAndMember()
        }
      }
    )

  }

  seeAllClickedMethod(which:'owned'|'member'|'other') {
    this.seeAllClicked.emit(which);
  }

  reloadOwnedAndMember() {
    console.log("Reloading things");

    this.muralService.getOwned().subscribe({
      next: (murals) =>{
        console.log("owned reloaded");
        console.log(this.ownedMurals.length==murals.length);


        this.ownedMurals = murals;
        this.ownedMuralsWindow = this.ownedMurals.slice(0+this.ownedMuralsOffset,4+this.ownedMuralsOffset);
      }
    })
    this.muralService.getMember().subscribe({
      next: (murals) =>{
        console.log("member reloaded");
        this.memberMurals = murals;
        this.memberMuralsWindow = this.memberMurals.slice(0+this.memberMuralsOffset,4+this.memberMuralsOffset);
      }
    })
  }
}
