import { Mural } from './../../models/mural.model';
import { MuralService } from './../../services/mural.service';
import { AlertService } from './../../services/alert.service';
import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable } from 'rxjs';
import { Router } from '@angular/router';

@Component({
  selector: 'app-murals-new',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './murals-new.component.html',
  styleUrl: './murals-new.component.scss'
})
export class MuralsNewComponent {

  selectedUsers: {id:number, name:string}[] = [];
  form!: FormGroup;
  thumbnailImage!:File;
  bannerImage!:File;


  constructor(protected activeModal: NgbActiveModal, private fb: FormBuilder, private alertService:AlertService, private muralService:MuralService, private router:Router) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      name: ['', Validators.required],
      description: [''],
      thumbnail: [null, Validators.required],
      banner: [null, Validators.required]
    }, {
      updateOn: 'blur'
    });
  }

  onFileSelected($event: Event, type: "thumbnail" | "banner") {
    const input = $event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      //validate it
      //disable form submission until done
      this.checkAspectRatio(file, type=="banner"? 5/1 : 3/2).subscribe((isGood)=>{
        if (isGood) {
          if (type=="banner") {
            this.bannerImage = file;
          } else if (type=="thumbnail") {
            this.thumbnailImage = file;
          }
          this.form.get(type)?.setErrors(null)
        }
        else { //maybe should return them to null
          setTimeout(()=>
            this.form.get(type)?.setErrors({invalidFile:true})
        );
          this.alertService.toastError("File must be an image with an aspect ratio of " + (type=="banner"? "5:1" : "3:2"), "File upload failed")
        }
        //re-enable form submission
      })
    }
  }

  deleteUser(id:number) {
    this.selectedUsers = this.selectedUsers.filter(user=>user.id!=id)
  }

  checkAspectRatio(file: File, desiredRatio:number, tolerance:number=0.01): Observable<boolean> {
    return new Observable((observer) => {
      //create an image to host the file, and a reader to cast the file into the image
      const img = new Image();
      const reader = new FileReader();

      //customize the reader and img. Reassigning img.source loads it
      reader.onload = (e) => {
        img.src = e.target?.result as string;
      };
      img.onload = () => {
        const aspectRatio = img.width / img.height;
        console.log("Calculated aspect ratio: " + aspectRatio);
        console.log("Desired ratio: " + desiredRatio);
        const isValid = Math.abs(aspectRatio - desiredRatio) < tolerance; // optional tolerance
        console.log("isValid: "+isValid);

        observer.next(isValid);
        observer.complete();
      };

      reader.readAsDataURL(file);
    });
  }

  submitForm() {
    if (!this.form.valid || this.thumbnailImage==null || this.bannerImage==null) {
      this.alertService.toastError("The form is invalid. This shouldn't happen","Something went wrong")
      return
    }
    this.alertService.loading();
    const name = this.form.get('name')?.value;
    const description = this.form.get('description')?.value;


    this.muralService.createMural({
      name:name,
      description:description,
      thumbnail:this.thumbnailImage,
      banner:this.bannerImage,
    }).subscribe({
      next:(mural:Mural)=>{
        this.alertService.loaded()
        this.alertService.confirm("The mural has been created, would you like to see it?", "Mural created", {accept:"Yes", cancel:"No"}).subscribe(
          (shouldRedirect)=>{
            if (shouldRedirect) {
              this.router.navigate(["/murals",mural.id,"dashboard"])
              this.activeModal.dismiss()
            } else {
              //reload owned murals and member murals
              this.activeModal.close("reloadOwnedAndMember")
            }

            }
        )},
      error:()=>{this.alertService.loaded(); this.alertService.toastError("Error creating mural")}
    });
  }

  logShit() {
    console.log("-----------------------------");
    console.log("Errors: " + this.form.errors)
    console.log("Thumbnail: " + this.form.errors?.["invalidThumbnail"])
    console.log("Banner: " + this.form.errors?.["invalidBanner"])
  }
}
