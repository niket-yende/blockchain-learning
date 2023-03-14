import { Component, OnInit } from '@angular/core';
import { RegisterService} from './register.service';
import { ActivatedRoute,Router } from "@angular/router";



@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent implements OnInit {
  

model: any = {};
c_code:any="91";
invalid:boolean=false;
atSubmit:boolean=false;

  constructor(private registerService:RegisterService,private router:Router) { }

  ngOnInit() {
  }

  telInputObject(obj) {
    console.log("kkk:",obj);
    obj.intlTelInput('setCountry', 'in');
  }

  onCountryChange(obj){
    console.log(obj);
    this.c_code=obj["dialCode"];
    console.log(this.c_code)
    this.model.contact=null;
    
  }
  getNumber(obj){
    console.log(obj); 
    this.model.contact=obj;  
  }
 hasError(obj){
   console.log(obj)
   this.invalid=!obj;
   if(obj==false){
     console.log("invalid phone number")
      
      this.model.contact=null;

   }
 }
  onSubmit(){
    this.atSubmit=true;
     console.log(this.model)
     console.log(this.model.name.length)
     this.model.name=this.model.name.trim();
     console.log(this.model.name.length)
     this.registerService.register(this.model)
     .subscribe(
      (val)=>{
        console.log(val)
       
      alert("Successfully Registered")
       this.router.navigate(['login'])
        this.atSubmit=false;
   },
      (err)=>{
        console.log(err)
        this.atSubmit=false;
   }
   )
  }

}
