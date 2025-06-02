import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Component, Input, OnInit } from '@angular/core';
import { ComponentType, GridItemService } from '../../../services/grid-item.service';

@Component({
  selector: 'app-grid-table',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './grid-table.component.html',
  styleUrl: './grid-table.component.css'
})
export class GridTableComponent implements OnInit{
  @Input() title!:string;
  @Input() type!:ComponentType;

  colNames!:string[];
  rowValues!:string[][];
  rowLinks!:string[] | null; //it should be such that adding the id of the clicked item at the end redirects

  constructor(private router:Router, private gridItemService: GridItemService){}

  ngOnInit(): void {
    this.colNames = this.gridItemService.getColNames(this.type);
    this.gridItemService.getRowValues(this.type).subscribe((values)=>{
      if (values == null) return;
      if (values.length === 0) console.warn('RecordsComponent received empty record list');
      this.rowValues = values;
    })
    this.gridItemService.getRowLinks(this.type).subscribe((links)=>{
      console.log(`GridTableComponent ${this.type} received row links:`, links);

      if (links == null) return;
      this.rowLinks = links;
    })
  }

  rowClicked(index: number) {
    if (this.rowLinks==null) return
    const parts = this.rowLinks[index].split('?');
    if (parts.length == 2) {
      return this.router.navigate([parts[0]], { queryParams: { selected: parts[1].split('=')[1] } });
    }
    return this.router.navigate([parts[0]]);
  }

}
