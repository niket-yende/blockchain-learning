import { Component } from '@angular/core';
import { ActivatedRoute,Router} from '@angular/router';
import { Location} from '@angular/common'
import {HttpErrorResponse} from '@angular/common/http'
import { Login } from './login';
import {map,filter} from 'rxjs/operators'
import {LoginService} from './login.service'



@Component({
    templateUrl: './login.component.html',
    styleUrls: ['./login.component.css'],
    providers: []
})
export class LoginComponent {

    login = new Login();
    user: any[];
    valid = true;
    isLoggedIn = 'false';
    errormessage =""

    constructor(private router: Router,private location:Location,private loginService:LoginService) {
       
        this.router.navigate(['/'])
        this.location.replaceState('/');        
    }

    onSubmit() {
        this.valid = true;
        const name = this.login.userName;
        sessionStorage.setItem('username', this.login.userName);
        const password = this.login.password;
        console.log(this.login)
        
        this.loginService.validateUser(this.login).subscribe(res=>{
            console.log(res)
            if(Array.isArray(res) && res!=null){
                this.user=res.filter(
                    res=>res.userName==this.login.userName&&res.password==this.login.password 
                ); 
            }
            console.log(this.user)
            if(this.user.length!=0 && this.user[0].role=="organizer"){
                this.router.navigate(['/organizer',this.user[0].userName]);
            }
            else if(this.user.length!=0  && this.user[0].role=="insurance"){
                this.router.navigate(['/insuranceCompany',this.user[0].userName]);
            }
            else{
                this.valid=false;
                console.log("Invalid Credentials")
            }
        }),
        (err:HttpErrorResponse)=>{
            console.log(err);
        }
    }
}
