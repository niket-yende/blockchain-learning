import { Component, OnInit } from '@angular/core';
import { ActivatedRoute,Router } from "@angular/router";
import {HttpClient,HttpErrorResponse} from '@angular/common/http'
import {OrganizerService} from '../organizer/organizer.service'

@Component({
  selector: 'app-policies',
  templateUrl: './policies.component.html',
  styleUrls: ['./policies.component.css']
})
export class PoliciesComponent implements OnInit {
policy:any;
name:any;
check:boolean=false;
each_data:any;
each_detail:boolean=false;
public searchText:string;
public customerData:any;
loader:boolean=false;
  constructor(private route: ActivatedRoute,private http:HttpClient,private os:OrganizerService ) { 
  this.name = this.route.parent.snapshot.paramMap.get( "id" );
    console.log(this.name)
  
   
  }

  ngOnInit() {
    
    this.loader=true;
    this.os.getPolicies(this.name).subscribe(
      res=>{
         this.policy=res.data;
         if(this.policy.length==0){
           alert('No Contracts Available')
         }
         else{
        this.customerData=JSON.parse(this.policy);
        console.log(this.customerData);
        if(this.customerData.length==0)
        alert("No Data Available")
        this.check=!this.check;
        // this.loader=false;
      }
    },
      (err)=>{
        alert("Some Error Occured");
        this.check=true;
      }
      // ,
      // ()=>{
      //   setTimeout(()=>{this.loader=false},3000);
      // }
    );
  }
  details(e:any){
    console.log(e)
    let k=this.customerData.filter(res=>res.Record.contractID==e)
    console.log(k);
    this.each_data=k;
    this.each_detail=true;
    
  }
}
