import { Component, OnInit } from '@angular/core';
import { ActivatedRoute,Router } from "@angular/router";
import { DatePipe } from '@angular/common';
import {Response} from '@angular/http'
import {HttpErrorResponse} from '@angular/common/http'
import {OrganizerService} from '../organizer/organizer.service'

@Component({
  selector: 'app-homeorg',
  templateUrl: './homeorg.component.html',
  styleUrls: ['./homeorg.component.css']
})
export class HomeorgComponent implements OnInit {
atSubmit:boolean=false;
name:any;
model: any = {};
temp:boolean=false;
snow:boolean=false;
rain:boolean=false;
check1:boolean=false;
min_temp='';
max_temp='';
min_rain='';
max_rain='';
policy_no=1;
  constructor(private route: ActivatedRoute,private organizationService:OrganizerService) {

    this.name = this.route.parent.snapshot.paramMap.get( "id" );
    console.log(this.name)
   this.model.insuranceCriterias={temp:null,rain:null,snow:false}
   }




  ngOnInit() {
  }
  check(){
    this.check1=true;
  }

  calcTemp(event){
    if(this.max_temp<this.min_temp && this.max_temp !=''){
      this.max_temp=''; 
    }
    this.model.insuranceCriterias.temp=this.min_temp+"-"+this.max_temp;
  }
  calcRain($event){
    if(this.max_rain<this.min_rain && this.max_rain !=''){
      this.max_rain='';
    }
    this.model.insuranceCriterias.rain=this.min_rain+"-"+this.max_rain;
  }
  onSubmit(){

    if(this.temp==false && this.rain==false && this.snow==false){
      alert("Please select one criteria")
    }else{
      this.atSubmit=true;
    console.log(this.model)
    if(!this.temp){
      delete this.model.insuranceCriterias["temp"];
    }
    if(!this.rain){
      delete this.model.insuranceCriterias["rain"];
    }
    if(!this.snow){
      delete this.model.insuranceCriterias["snow"];
    }
    console.log("dfg", typeof this.model.startDate)
    this.model.startDate = new DatePipe('en-US').transform(this.model.startDate, "yyyy-MM-ddTHH:mm:ss'Z'",'UTC')
    this.model.endDate = new DatePipe('en-US').transform(this.model.endDate, "yyyy-MM-ddTHH:mm:ss'Z'",'UTC')
   
    console.log(this.model.endDate)
    
    this.policy_no=this.policy_no+1;
    this.organizationService.createInsuranceContract(this.name,this.model)
   .subscribe((res)=>{
     console.log(res)
     alert("Successfully applied with Contract ID "+JSON.parse(res.payload))
     this.atSubmit=false;

   },
   (error)=>{
      console.log(error)
      alert(error)
      this.atSubmit=false;
   }
   )

    }
  }
  onTemp(){
    this.temp=!this.temp;
  
  }
  onRain(){
    this.rain=!this.rain;
  }
  onSnow(){
    this.snow=!this.snow;
    this.model.insuranceCriterias.snow=this.snow;
  }

}
