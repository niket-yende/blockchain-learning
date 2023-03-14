import { Component, OnInit,OnDestroy } from '@angular/core';
import { ActivatedRoute,Router,NavigationEnd } from "@angular/router";
import {OrganizerService} from '../organizer/organizer.service'

@Component({
  selector: 'app-contracthistory',
  templateUrl: './contracthistory.component.html',
  styleUrls: ['./contracthistory.component.css']
})
export class ContracthistoryComponent implements OnInit,OnDestroy {
model:any={};
name:String='';
load:boolean=false;
contract:any;
inAct:String='';
act:String='';
claim:String='';
data_is_there:boolean=false;
inactive:any=[];
active:any=[];
claimed:any=[];
unclaimed:any=[];
statusClick:boolean=false;
each_detail:any;
len:number;
each_status_detail:any;
navigationSubscription;

constructor(private route:ActivatedRoute,private router:Router,private os:OrganizerService )
 {
   this.navigationSubscription=this.router.events.subscribe((e:any)=>
    {
      if(e instanceof NavigationEnd)
      {
        this.load=true;
        this.route.params.subscribe(params=>this.name=params.key);
        console.log(this.name)
        this.os.getContractHistory(this.name).subscribe(
          res=>{
                this.contract=JSON.parse(res.data);
                console.log(this.contract,this.contract.length)
                this.len=this.contract.length;
                console.log(this.len)
                if(this.contract.length==0)
                {
                  this.data_is_there=false;
                  alert("No Data Available for ContractID:"+this.name)
                  this.load=false;
                }
                else
                {
                  this.data_is_there=true;
                  if(this.contract.length==1)
                  {
                    this.inactive=this.contract[0];
                    this.inAct="INACTIVE";
                  }
                  else if(this.contract.length==2)
                  {
                    this.inactive=this.contract[0];
                    this.active=this.contract[1];
                    this.inAct="INACTIVE";
                    this.act="ACTIVE";
                  }
                  else if(this.contract.length==3)
                  {
                    this.inactive=this.contract[0];
                    this.active=this.contract[1];
                    this.inAct="INACTIVE";
                    this.act="ACTIVE";
                    if(this.contract[2].Value.status=="CLAIMED")
                    {
                      this.claimed=this.contract[2];
                      this.claim="CLAIMED";
                    }
                    else if(this.contract[2].Value.status=="UNCLAIMED")
                    {
                      this.unclaimed=this.contract[2];
                      this.claim="UNCLAIMED";
                    }
                  }
                this.load=false;
              }     
            },
            (err)=>
            {
              alert("Some Error Occured")
              this.load=false;
            });
    }
  })
}

ngOnInit() {

}
  
ngOnDestroy(){
     if (this.navigationSubscription) {  
       this.navigationSubscription.unsubscribe();
    }
 }
  

details(val){
  this.each_detail=val;
  console.log(val)
}

seperate_details(stat){
    console.log(stat)
    this.each_status_detail=this.contract.filter(res=>res.Value.status==stat)
    console.log(this.each_status_detail)
    this.statusClick=true;
 }
}
