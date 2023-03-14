import { Component, OnInit } from '@angular/core';
import { ActivatedRoute,Router } from "@angular/router";
import {HttpClient,HttpErrorResponse} from '@angular/common/http'
import { Chart } from 'chart.js';
import {InsuranceService} from './insurance.service';
import {MessageService} from 'primeng/api';



@Component({
  selector: 'app-insurance',
  templateUrl: './insurance.component.html',
  styleUrls: ['./insurance.component.css']
})
export class InsuranceComponent implements OnInit {
status:any;
value:any;
contracts:any;
contracts1:any;
unclaim:any;
claim:any;
in_active:any;
actv:any;
each_detail:boolean=false;
each_data:any;
visible:boolean=false;
atLoad:boolean=false;
st:string="Total";
each_weather:boolean=false;
public searchText:string;
public pieChartLabels:string[]=['Claimed','Unclaimed','Inactive','Active'];
public pieChartData:number[]=[0,0,0,0];
public pieChartType:string='pie';
data:any;
pie_chart=false;
weather:any;
reportId:any;
weather_criteria:any;
name:any;
pieChartColor:any = [
    {
        backgroundColor: ['rgba(45, 179, 0, 0.8)',
        'rgba(255, 102, 0, 0.9)',
        'rgba(139, 136, 136, 0.9)',
        'rgba(0, 136, 204, 0.9)',
        
        ]
    }
]

  
constructor(private route: ActivatedRoute,private http:HttpClient,private is:InsuranceService,private ms:MessageService) { 
    this.name=this.route.snapshot.paramMap.get('id')
    console.log(this.name)
  }

ngOnInit() {
    
      this.is.getContracts(this.name).subscribe(
      (res)=>{
        this.contracts=JSON.parse(res.data);
        console.log(this.contracts)
        if(this.contracts.length==0){
          alert("No Data Available")
          this.atLoad=true;
        }
        else{
          this.status="Total";
          this.value=this.contracts.length;
        
          this.unclaim=this.contracts.filter(data1=>
          data1.Record.status==="UNCLAIMED"
          )
          console.log(this.unclaim)
          this.claim=this.contracts.filter(data1=>
          data1.Record.status==="CLAIMED"
          )
          console.log(this.claim,this.claim.length)
          this.in_active=this.contracts.filter(data1=>
          data1.Record.status==="INACTIVE"
          )
          console.log(this.in_active)
          this.actv=this.contracts.filter(data1=>
          data1.Record.status==="ACTIVE"
          )
          console.log(this.actv)
          this.pieChartData[0]=this.claim.length;
          this.pieChartData[1]=this.unclaim.length;
          this.pieChartData[2]=this.in_active.length;
          this.pieChartData[3]=this.actv.length;
          console.log(this.pieChartData)

          this.contracts1=this.contracts;
          this.pie_chart=!this.pie_chart;
          this.atLoad=true;
      }
      },
      (err)=>{

        alert("Some Error Occured")
        this.atLoad=true;
      }
      
    ) 
  }

  
  public chartClicked(e:any):void {
    console.log(e);
    if (e.active.length > 0) {
    let i=e.active[0]._index;
    this.status=this.pieChartLabels[i];
    this.value=this.pieChartData[i];
    console.log(this.status,this.value);
    this.visible=true;
    this.ms.add({key: 'tl',severity:'info', summary: 'Info Message', detail:this.status +" Contracts : "+this.value});
    
    }
  }

  public chartHovered(e:any):void {
    console.log(e);
    this.data=e;
    console.log(this.data)
  }

  claimed(){
    console.log("claimed")
    this.contracts1=this.claim;
    this.status="Claimed";
    this.value=this.claim.length;
    this.st="Claimed";
  }

  unclaimed(){
     this.contracts1=this.unclaim;
     this.status="Unclaimed";
    this.value=this.unclaim.length;
    this.st="Unclaimed";

  }

  inactive(){
     this.contracts1=this.in_active;
     this.status="Inactive";
    this.value=this.in_active.length;
    this.st="Inactive";

  }

  active(){
  this.contracts1=this.actv;
  this.status="Active";
    this.value=this.actv.length;
    this.st="Active"
  }
  
  details(e:any){
    this.atLoad=false;
    console.log(e)
    let k=this.contracts1.filter(res=>res.Record.contractID==e)
    console.log(k);
    this.each_data=k;
    this.each_detail=true;
    this.atLoad=true;
  }

  weatherDetails(l,s,e){
    this.atLoad=false;
    let body={
      'location':l,
      'startDate':s,
      'endDate':e
    }
    this.is.getWeather(this.name,body).subscribe(
     (res)=>{
    
      console.log(res)
      this.weather=JSON.parse(res.data);
      console.log(this.weather)
      if(this.weather.length==0){
        alert("No Weather Record is available")
        this.atLoad=true;
      }
      else{
        let len=this.weather.length;
      this.reportId=this.weather[len-1].Record.reportID;
      this.weather_criteria=this.weather[len-1].Record.weatherCriterias;
      this.atLoad=true;
      this.each_weather=true;
      }
   },
   (err)=>{
        alert("Some Error Occured")
        this.atLoad=true;
        
      })
  }

 


}
