import { MuralService } from './../../services/mural.service';
import { Component, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output, SimpleChanges, TemplateRef } from '@angular/core';
import { Mural } from '../../models/mural.model';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { MuralsNewComponent } from '../murals-new/murals-new.component';
import { AlertService } from '../../services/alert.service';

@Component({
  selector: 'app-murals-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './murals-list.component.html',
  styleUrl: './murals-list.component.scss'
})
export class MuralsListComponent implements OnChanges, OnInit {

  ownedMurals!: Mural[];
  memberMurals!: Mural[];
  otherMurals!: Mural[];

  ownedMuralsWindow!: Mural[];
  memberMuralsWindow!: Mural[];
  otherMuralsWindow!: Mural[];

  ownedMuralsOffset: number = 0;
  memberMuralsOffset: number = 0;
  otherMuralsOffset: number = 0;

  activeModal: NgbModalRef | null = null;

  @Output() seeAllClicked:EventEmitter<'owned'|'member'|'other'> = new EventEmitter<'owned'|'member'|'other'>();

  constructor(private ngbModal:NgbModal, private muralService:MuralService, private modalService:NgbModal, private alertService:AlertService) {}

  ngOnInit(): void {
    // Initialize owned, member, and other murals
    this.loadOwned()
    this.loadMember()
    this.loadOther()
  }

  loadOwned(){
    console.log("(MuralsListComponent) ------------------------------- updating owned murals cache");
    this.muralService.unloadData({owned:true})
    this.muralService.getOwned().subscribe((murals) => {
        if (murals==null) return
        this.ownedMurals = murals;
        this.ownedMuralsOffset = 0;
        this.ownedMuralsWindow = this.ownedMurals.slice(0,4);
      }
    );
  }
  loadMember(){
    console.log("(MuralsListComponent) ------------------------------- updating member murals cache");
    this.muralService.unloadData({member:true})
    this.muralService.getMember().subscribe((murals) => {
        if (murals==null) return
        this.memberMurals = murals;
        this.memberMuralsOffset = 0;
        this.memberMuralsWindow = this.memberMurals.slice(0,4);
      }
    );
  }
  loadOther(){
    console.log("(MuralsListComponent) ------------------------------- updating other murals cache");
    this.muralService.unloadData({other:true})
    this.muralService.getOther().subscribe((murals) => {
        if (murals==null) return
        this.otherMurals = murals;
        this.otherMuralsOffset = 0;
        this.otherMuralsWindow = this.otherMurals.slice(0,4);
    });
  }

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
    const modal = this.ngbModal.open(MuralsNewComponent, { centered:true });
    modal.result.then(
      (result) => {
        if (result=="reloadOwnedAndMember") {
          this.loadOwned();
          this.loadMember();
        }
      }
    )

  }

  seeAllClickedMethod(which:'owned'|'member'|'other') {
    this.seeAllClicked.emit(which);
  }

  errorLoadingImage($event: ErrorEvent) {
    const img = $event.target as HTMLImageElement;
    console.error("Error loading image:", img.id);
    img.src = MuralService.defaultThumbnail;
  }

  openJoinMural(content: TemplateRef<any>) {
    this.activeModal = this.modalService.open(content, { centered:true })

  }

  joinMural(muralCode: string) {
    if (muralCode.split("-").length!=3 || !muralCode.split("-").every(s=>s.length==4)) return this.alertService.toastWarning("correct format is aaaa-bbbb-cccc", "Incorrect code format, try again")
   this.alertService.loading()
   this.muralService.joinMural(muralCode).subscribe({
     next:(result)=>{
       this.alertService.loaded()
       this.activeModal?.close();

       if (result==1) this.alertService.toastWarning("Can't join a mural you're part of")
       else if (result==0) {
         this.alertService.toastSuccess("Joined Mural")
         this.loadMember();
         this.loadOther();
       }
     },
     error:(e)=>{
       //interceptor should ignore these
       this.alertService.loaded()
       this.alertService.toastError("Error joining mural");
       console.error("Error joining mural: ", e);

     }
   })
  }

  muralClicked(id: number) {
    this.alertService.alert("You are not yet part of this mural.\nIf you want to join, you should ask its owner, " + this.otherMurals.find((m)=>m.id==id)?.owner.name + ", for the code","Not yet part of this mural")
  }
}
