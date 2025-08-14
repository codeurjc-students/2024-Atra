import { Mural } from './../../models/mural.model';
import { MuralService } from './../../services/mural.service';
import { AlertService } from './../../services/alert.service';
import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { map, Observable, of } from 'rxjs';
import { Router } from '@angular/router';

@Component({
  selector: 'app-murals-new',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './murals-new.component.html',
  styleUrl: './murals-new.component.scss'
})
export class MuralsNewComponent {

  form!: FormGroup;
  thumbnailImage!:File;
  bannerImage!:File;


  constructor(protected activeModal: NgbActiveModal, private fb: FormBuilder, private alertService:AlertService, private muralService:MuralService, private router:Router) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      name: ['', Validators.required],
      description: [''],
      thumbnail: [null, Validators.required, [this.aspectRatioValidator(this.muralService, "thumbnail")]],
      banner: [null, Validators.required, [this.aspectRatioValidator(this.muralService, "banner")]],
      visibility: ['PUBLIC', Validators.required]
    }, {
      updateOn: 'blur'
    });
    this.form.get('banner')?.statusChanges.subscribe(s => {
      console.log('banner', 'status:', s, 'errors:', this.form.get('banner')?.errors);
    });
    this.form.get('thumbnail')?.statusChanges.subscribe(s => {
     console.log('thumbnail', 'status:', s, 'errors:', this.form.get('thumbnail')?.errors);
    });

  }

  aspectRatioValidator(muralService: MuralService, type: "thumbnail" | "banner") {
    return (): Observable<ValidationErrors| null> => {
      const file = type=="banner"?this.bannerImage:this.thumbnailImage; // Create a temporary file object from the control value
      if (!file) {
        return of(null); // No file selected
      }

      return muralService.checkAspectRatio(file, type === "banner" ? 5/1 : 3/2).pipe(
        map((isGood:boolean) => isGood ? null : { invalidFile: true }))
    };
  }

  onFileSelected($event: Event, type: "thumbnail" | "banner") {
    const input = $event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      if (type=="banner") {
        this.bannerImage = file;
      } else if (type=="thumbnail") {
        this.thumbnailImage = file;
      }

      //validate it
      this.muralService.checkAspectRatio(file, type=="banner"? 5/1 : 3/2).subscribe((isGood)=>{
        if (isGood) {
          //remove the invalidFile error, and only that one, by copying the errors, removing it, and setting it back if it has anything, or null if it's now empty
          //would just be this.form.get(type)?.setErrors(null), but this overwrites any other errors there may be
          const errors = { ...this.form.get(type)?.errors };
          delete errors['invalidFile'];
          this.form.get(type)?.setErrors(Object.keys(errors).length? errors : null);
        }
        else { //maybe should return them to null
          this.form.get(type)?.setErrors({invalidFile:true})
          this.alertService.toastError("File must be an image with an aspect ratio of " + (type=="banner"? "5:1" : "3:2"), "File upload failed")
        }
      })
    }
  }

  submitForm() {
    if (!this.form.valid || this.thumbnailImage==null || this.bannerImage==null) {
      this.alertService.toastError("The form is invalid. This shouldn't happen","Something went wrong")
      return
    }
    this.alertService.loading();
    const name = this.form.get('name')?.value;
    const description = this.form.get('description')?.value;
    const visibility = this.form.get('visibility')?.value;


    this.muralService.createMural({
      name:name,
      description:description,
      visibility:visibility,
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
}




/*
  onFileSelected($event: Event, type: "thumbnail" | "banner") {
    const input = $event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      //validate it
      //disable form submission until done
      this.muralService.checkAspectRatio(file, type=="banner"? 5/1 : 3/2).subscribe((isGood)=>{
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
*/
