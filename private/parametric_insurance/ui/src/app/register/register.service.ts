import { Injectable } from '@angular/core';
import {Response} from '@angular/http'
import {HttpClient,HttpHeaders,HttpErrorResponse} from '@angular/common/http'
import { Observable } from 'rxjs';
import {tap,map,filter,catchError} from 'rxjs/operators';
import {conf} from '../config/config';

@Injectable({
  providedIn: 'root'
})
export class RegisterService {

formdata:any={};
  constructor(private http:HttpClient) { }

  register(data):Observable <any>{

    console.log("Register Service:",data);
    this.formdata.name=data.name;
     this.formdata.email=data.email;
     this.formdata.address=data.address;
     this.formdata.contact=data.contact;
     this.formdata.password=data.password;

     console.log("Rg:",this.formdata)
     const httpOptions = {
            headers: new HttpHeaders({
            'Content-Type': 'Application/json; charset=UTF-8',
           
            }),
            };
    return this.http.post(conf.URL+data.role,this.formdata,httpOptions)
    .pipe(map((response: Response) => {
        console.log("Extract Registered Data: ",response);

        return response;
    }),tap(event => {}, this.handleError)) 
  }
  
  // private extractData(res: Response) {
	//     let body = res;
  //     console.log("ExtractData:",body)
  //     return body;
  //   }
   private handleError (err: HttpErrorResponse) {
		console.error(err);
    alert("Something Went Wrong")
		return err;
    }
}
