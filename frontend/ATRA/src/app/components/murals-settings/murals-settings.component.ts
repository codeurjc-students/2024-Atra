import { Component, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { GridTableComponent } from "../stat-components/records/grid-table.component";
import { ActivitySelectComponent } from "../activity-select/activity-select.component";
import { UserService } from '../../services/user.service';
import { ActivatedRoute, Router } from '@angular/router';
import { Activity } from '../../models/activity.model';
import { ActivityService } from '../../services/activity.service';
import { NgbModal, NgbModalModule, NgbModalOptions, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { CommonModule } from '@angular/common';
import { AlertService } from '../../services/alert.service';
import { MuralService } from '../../services/mural.service';
import { RouteSelectComponent } from "../route-select/route-select.component";
import { Route } from '../../models/route.model';
import { RouteService } from '../../services/route.service';
import { NgbSlide } from "@ng-bootstrap/ng-bootstrap/carousel/carousel";
import { FormsModule, NgModel } from '@angular/forms';
import { Mural } from '../../models/mural.model';

@Component({
  selector: 'app-mural-settings',
  standalone: true,
  imports: [ActivitySelectComponent, NgbModalModule, CommonModule, RouteSelectComponent, FormsModule],
  templateUrl: './murals-settings.component.html',
  styleUrl: './murals-settings.component.scss'
})
export class MuralsSettingsComponent implements OnInit {
  manageMembers(membersModal: TemplateRef<any>) {
    if (this.userList.length == 0) {
      this.alertService.toastInfo("Use the code to invite people!", "The mural has no members");
    } else this.open(membersModal)
  }
  kickUser(id: number) {
    this.alertService.confirm("Are you sure you want to kick this user? The mural will lose access to any routes and activities owned by this user.\nThey can still join the mural if they have the code.").subscribe(accepted=> {if (accepted) this.muralService.kick(this.id, id).subscribe({
      next: (newUserList) => {
        this.alertService.toastSuccess("User has been kicked, but can still join the mural with the code");
        this.userList = newUserList;
      },
      error: (err) => {
        console.error("Error kicking user:", err);
        this.alertService.toastError("There was an error kicking the user");
      }
    })
    else this.alertService.toastInfo("Operation cancelled")
    })
  }
  banUser(id: number) {
    this.alertService.confirm("Are you sure you want to ban this user? The mural will lose access to any routes and activities owned by this user.\nThey will not be able to join the mural anymore, even if they have the code.").subscribe(accepted=> {if (accepted) this.muralService.ban(this.id, id).subscribe({
      next: (newUserList) => {
        this.alertService.toastSuccess("User banned successfully");
        this.userList = newUserList.filter(user => user.id !== this.owner.id); // Remove the banned user from the list
      },
      error: (err) => {
        console.error("Error banning user:", err);
        this.alertService.toastError("There was an error banning the user");
      }
    })
    else this.alertService.toastInfo("Operation cancelled")
    })
  }
  sendBannerImage($event: Event) {
    const input = $event.target as HTMLInputElement;
    if (!input.files || !(input.files.length > 0)) return this.alertService.toastError("No file selected", "Please select a file to upload");
    const file = input.files[0];

    this.alertService.loading();
    this.muralService.checkAspectRatio(file, 5/1).subscribe((isGood) => {
      this.alertService.loaded()
      if (isGood)
        this.alertService.confirm("Are you sure you want to change the mural's banner?","Change banner", {accept:"Change", cancel:"Cancel"}).subscribe(accepted => {
          if (!accepted) return this.alertService.toastInfo("Operation cancelled", "No changes were made");
          this.muralService.changeBanner(this.id, file).subscribe({
            next: () => this.alertService.toastSuccess("Mural banner changed successfully"),
            error: (err) => {
              console.error("Error changing mural banner:", err);
              this.alertService.toastError("There was an error changing the mural banner");
            }
          });
        })
      else this.alertService.toastError("The thumbnail must be an image with an aspect ratio of 5:1", "File upload failed");

    })
  }
  sendThumbnailImage($event: Event) {
    const input = $event.target as HTMLInputElement;
    if (!input.files || !(input.files.length > 0)) return this.alertService.toastError("No file selected", "Please select a file to upload");
    const file = input.files[0];

    this.alertService.loading();
    this.muralService.checkAspectRatio(file, 3/2).subscribe((isGood) => {
      this.alertService.loaded()
      if (isGood)
        this.alertService.confirm("Are you sure you want to change the mural's thumbnail?","Change thumbnail", {accept:"Change", cancel:"Cancel"}).subscribe(accepted => {
          if (!accepted) return this.alertService.toastInfo("Operation cancelled", "No changes were made");
          this.muralService.changeThumbnail(this.id, file).subscribe({
            next: () => this.alertService.toastSuccess("Mural thumbnail changed successfully"),
            error: (err) => {
              console.error("Error changing mural thumbnail:", err);
              this.alertService.toastError("There was an error changing the mural thumbnail");
            }
          });
        })
      else this.alertService.toastError("The banner must be an image with an aspect ratio of 3:2", "File upload failed");
    })
  }

  code!: string;
  owner!: {name:string, id:number};
  description!: string;
  name!: string;
  inheritor!: string;
  id!: number;


  activityList: Activity[] = [];
  routeList: Route[] = [];
  modal: NgbModalRef |null = null;
  selectedActivities: Set<number> | null = null;
  selectedRoutes: Set<number> | null = null;
  isOwner: boolean = false;

  selectedInheritor: { id: number, name: string } | null = null;
  userList: { id: number, name: string }[] = [];

  newDescription: string = '';
  changingOwner: boolean = true;

  constructor(
    private userService: UserService,
    private activityService: ActivityService,
    private route: ActivatedRoute,
    private modalService: NgbModal,
    private alertService: AlertService,
    private muralService: MuralService,
    private routeService: RouteService,
    private router: Router
  ) { }

  ngOnInit(): void {
    // Initialization logic here
    this.id = Number(this.route.snapshot.paramMap.get('id'))
    this.muralService.getMural(this.id).subscribe({
      next: (mural) => {
        this.code = mural.code ?? "Not Visible";
        this.owner = mural.owner;
        this.description = mural.description;
        this.name = mural.name;
        this.isOwner = JSON.parse(localStorage.getItem("user")!).id == mural.owner.id;
        this.userList = mural.members;
        //remove current owner from userlist
        this.userList = this.userList.filter(user => user.id !== mural.owner.id);

      },
      error: (err) => {
        console.error("Error fetching mural details:", err);
        this.alertService.toastError("There was an error fetching the mural");
      }
    })

    this.reloadActivities();
    this.reloadRoutes();
  }

  @ViewChild('changeVisibilityOfSelected') changeVisibilityOfSelected!: TemplateRef<any>;
  @ViewChild('selectInheritor') selectInheritor!: TemplateRef<any>;

  submitAct = () => {
    if (this.selectedActivities==null || this.selectedActivities.size===0) {
      this.alertService.alert("You need to select an activity to change its visibility", "No activities selected")
      return;
    }
    this.alertService.confirm(
      "Are you sure you want to make these activities not visible to this mural?\nMURAL_SPECIFIC activities will remove the mural from their list.\nMURAL_PUBLIC activities will turn MURAL_SPECIFIC, allowing any mural you're part of, except this one, to see them.",
      "Changing visibility of "+this.selectedActivities.size+" activit"+ (this.selectedActivities.size > 1 ? "ies" : "y")
    ).subscribe(accepted => {
      console.log("(MuralSettingsComponent) accepted: ", accepted);

      if (accepted) this.activityService.makeActivitiesNotVisibleToMural(this.id, this.selectedActivities).subscribe({
        next: () => {
          this.alertService.toastSuccess("Visibility of selected activities changed successfully");
          this.reloadActivities()
          //activityselect component should load until activities are reloaded, to prevent intermediate calls
        },
        error: (err) => {
          console.error("Error changing activities visibility:", err);
          this.alertService.toastError("There was an error changing the visibility of the selected activities");
        }
      })
      else this.alertService.toastInfo("No changes were made", "Operation cancelled")
    })
  };

  submitRoute = () => {
    if (this.selectedRoutes==null || this.selectedRoutes.size===0) {
      this.alertService.alert("You need to select a route to change its visibility", "No routes selected")
      return;
    }
    this.alertService.confirm(
      "Are you sure you want to make these activities not visible to this mural?\nMURAL_SPECIFIC activities will remove the mural from their list.\nMURAL_PUBLIC activities will turn MURAL_SPECIFIC, allowing any mural you're part of, except this one, to see them.",
      "Changing visibility of "+this.selectedRoutes.size+" route"+ (this.selectedRoutes.size > 1 ? "s" : "")
    ).subscribe(accepted => {
      console.log("(MuralSettingsComponent) accepted: ", accepted);

      if (accepted) this.routeService.makeRoutesNotVisibleToMural(this.id, this.selectedRoutes).subscribe({
        next: () => {
          this.alertService.toastSuccess("Visibility of selected activities changed successfully");
          this.reloadRoutes()
          //activityselect component should load until activities are reloaded, to prevent intermediate calls
        },
        error: (err) => {
          console.error("Error changing activities visibility:", err);
          this.alertService.toastError("There was an error changing the visibility of the selected activities");
        }
      })
      else this.alertService.toastInfo("No changes were made", "Operation cancelled")
    })
  };

  reloadActivities() {
    this.userService.getActivitiesInMural(this.id).subscribe({
      next: (data:any[]) => {
        // Process the data received from the service
        console.log("Activities and routes in mural:", data);
        this.activityList = this.activityService.process(data)
      },
      error: (err) => {
        console.error("Error fetching activities and routes in mural:", err);
      }
    })
  }

  reloadRoutes() {
    this.userService.getRoutesInMural(this.id).subscribe({
      next: (data:Route[]) => {
        // Process the data received from the service
        console.log("Activities and routes in mural:", data);
        this.routeList = data
      },
      error: (err) => {
        console.error("Error fetching activities and routes in mural:", err);
      }
    })
  }

  //what if I call open twice in a row?
  open(content: TemplateRef<any>, options?:NgbModalOptions): void {
    this.modal = this.modalService.open(content, options??{ centered:true })
  }

  handleEmittedDataAct(data: Set<number>) {
    this.selectedActivities = data;
  }
  handleEmittedDataRoute(data: Set<number>) {
    this.selectedRoutes = data;
  }

  leave() {
    this.alertService.confirm("Are you sure you want to cotinue?", "Leaving the mural").subscribe(accepted => {
      if (!accepted) this.alertService.toastInfo("Operation cancelled", "No changes were made");
      else {
        if (!this.isOwner) {
          this.muralService.leave(this.id).subscribe({
            next: () => {
              this.alertService.alert("You will be redirected to the mural list", "You have left the mural successfully");
              this.router.navigate(['/murals'])
            },
            error: (err) => {
              console.error("Error leaving the mural:", err);
              this.alertService.toastError("There was an error leaving the mural");
            }
          })
        } else {
          if (this.userList.length>1)
            this.alertService.alert("You are the owner of the mural, you need to select an inheritor before leaving", "Before you leave",
              () => {
                this.changingOwner = false;
                this.open(this.selectInheritor, { centered:true, backdrop: 'static' })
              }
            );
          else
            this.alertService.confirm(
              "You are the only member in this mural. If you leave, it will be left with no members and be deleted.\nSelecting to leave will DELETE the mural",
              "If you leave, the mural will be deleted",
              {accept:"Leave and Delete", cancel:"Cancel"}
            ).subscribe(accepted => {
              if (accepted) {
                this.muralService.leave(this.id).subscribe({
                  next: () => {
                    this.alertService.alert("You will be redirected to the mural list", "You have left the mural successfully");
                    this.router.navigate(['/murals'])
                  },
                  error: (err) => {
                    console.error("Error leaving the mural:", err);
                    this.alertService.toastError("There was an error leaving the mural");
                  }
                })
              } else {
                this.alertService.toastInfo("Operation cancelled", "No changes were made");
              }
            })
        }
      }
    })


  }

  deleteMural() {
    this.alertService.confirm("Are you sure you want to delete this mural? This cannot be undone.", "You are about to delete this mural", {accept:"Delete", cancel:"Cancel"}).subscribe(accept=>{
      if (!accept) return this.alertService.toastInfo("Operation cancelled", "No changes were made");
      this.alertService.inputConfirm("To continue, you need to type the name of the mural", "Confirm deletion of mural", {accept:"Delete", cancel:"Cancel"}, this.name).subscribe(answer => {
        const accept = answer.accept
        const text = answer.text
        if (!accept) return this.alertService.toastInfo("Operation cancelled", "No changes were made");
        if (text!=this.name) return this.alertService.alert("You need to type the name of the mural to confirm deletion.\nThe operation has been cancelled.", "Wrong mural name");
        this.muralService.deleteMural(this.id).subscribe({
          next: () => {
            this.alertService.alert("You will be redirected to the mural list", "Mural deleted successfully",()=>this.router.navigate(['/murals']));
          },
          error: (err) => {
            console.error("Error deleting the mural:", err);
            this.alertService.alert("There was an error deleting the mural. It has not been properly deleted", "Something went wrong");
          }
        })
      })
    })
  }

  setInheritor() {
    this.modal?.close();
    if (this.changingOwner) this.muralService.changeOwner(this.id, this.selectedInheritor!.id).subscribe({
      next: (mural: Mural) => {
        this.modalService.dismissAll();
        this.alertService.toastSuccess("Mural owner changed successfully");
        this.owner = mural.owner;
        this.isOwner = JSON.parse(localStorage.getItem("user")!).id == mural.owner.id;
        this.userList = mural.members;
        //remove current owner from userlist
        this.userList = this.userList.filter(user => user.id !== mural.owner.id);
      },
      error: (err) => {
        console.error("Error changing mural owner:", err);
        this.alertService.toastError("There was an error changing the mural owner");
      }
    })

    else
    this.alertService.confirm("You are leaving the mural. The user '" +this.selectedInheritor!.name+ "' will receive ownership. If this is correct, click on leave." , "One last check", {accept:"Leave", cancel:"Cancel"}).subscribe(
      accepted => {
        if (!accepted) return this.alertService.toastInfo("Operation cancelled", "No changes were made");
        this.muralService.leave(this.id, this.selectedInheritor?.id).subscribe({
            next: () => {
              this.alertService.alert("You will be redirected to the mural list", "You have left the mural successfully");
              this.router.navigate(['/murals'])
            },
            error: (err) => {
              console.error("Error leaving the mural:", err);
              this.alertService.toastError("There was an error leaving the mural");
            }
          })
      }
    )
  }

  editName() {
    this.alertService.inputConfirm("What would you like the new name to be?","Change Mural name", {accept:"Change", cancel:"Cancel"},"").subscribe(answer => {
      if (!answer.accept) return this.alertService.toastInfo("Operation cancelled", "No changes were made");
      this.muralService.changeName(this.id, answer.text).subscribe({
        next: () => {
          this.alertService.toastSuccess("Mural name changed successfully");
          this.name = answer.text;
        },
        error: (err) => {
          console.error("Error changing mural name:", err);
          this.alertService.toastError("There was an error changing the mural name");
        }
      })
    })
  }

  editDesc(descModal: TemplateRef<any>) {
    this.newDescription = this.description;
    this.open(descModal, { centered:true, backdrop:'static' });
  }
  editOwner(ownerModal: TemplateRef<any>) {
    this.changingOwner = true;
    this.alertService.alert("This is an irreversible action. By selecting a different owner you are relinquishing all authority over this mural.\nYou will not be able to edit it or manage its members, though you will remain as a member.\nProceed with caution ","Before you change the owner",
      ()=>this.open(ownerModal, { centered:true, backdrop:'static' })
    )
  }
  editImage(input: HTMLInputElement): void {
    input.value = '';
    input.click();
  }
  submitChangeDesc() {
    this.muralService.changeDesc(this.id, this.newDescription).subscribe({
      next: () => {
        this.alertService.toastSuccess("Mural description changed successfully");
        this.description = this.newDescription;
        this.modal?.close();
      },
      error: (err) => {
        console.error("Error changing mural description:", err);
        this.alertService.toastError("There was an error changing the mural description");
      }
    })

  }

  a(){}


}
