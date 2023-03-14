import { Component, OnInit } from '@angular/core';
import { ActivatedRoute,Router } from "@angular/router";



@Component({
  selector: 'app-organizer',
  templateUrl: './organizer.component.html',
  styleUrls: ['./organizer.component.css'],
})
export class OrganizerComponent implements OnInit {
name:any;
model: any = {};
history:boolean=false;

policy_no=1;
  constructor(private route: ActivatedRoute,private router:Router) {
   this.name=this.route.snapshot.paramMap.get('id')
   console.log(this.name)
   
   }




  ngOnInit() {
  }
  
  onEnter(){
    console.log(this.model)
    this.router.navigate(['/organizer/'+this.name+'/contractHistory',this.model.contractID])
    


  }
}
