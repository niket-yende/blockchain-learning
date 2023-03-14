import { Injectable } from '@angular/core';
import {Response} from '@angular/http';
import { ActivatedRoute} from "@angular/router";
import {HttpClient,HttpHeaders,HttpErrorResponse} from '@angular/common/http'
import { Observable } from 'rxjs';
import {tap,map,filter,catchError} from 'rxjs/operators';
import {conf} from '../config/config'

@Injectable({
  providedIn: 'root'
})
export class OrganizerService {
name:any;
  constructor(private http:HttpClient,private route:ActivatedRoute) {
    
   }
  

  createInsuranceContract(id,formdata):Observable <any>{
    // const url=conf.URL+'insuranceContract/';
    console.log("Org Service:",formdata);
     const httpOptions = {
            headers: new HttpHeaders({
            'Content-Type': 'Application/json; charset=UTF-8',
  
            
            }),
            };
            let data=JSON.stringify(formdata);
            console.log("data:",data)
      
    return this.http.post(conf.URL+'insuranceContract/',data,httpOptions)
        .pipe(map((response: Response) => {
        console.log("Response Query: ",response);

        return response;
    }),tap(event => {}, this.handleError)) 
  }

   private handleError (err: HttpErrorResponse) {
		console.error(err);
		return err;
    }


    getPolicies(id):Observable<any>{
      return this.http.get(conf.URL+"contracts/customer/"+id).pipe(map((response: Response) => {
        console.log("Response Query: ",response);

        return response;
    }),tap(event => {}, this.handleError)) 
   }

   getContractHistory(id):Observable<any>{
     return this.http.get(conf.URL+"contracts/history/"+id).pipe(map((response: Response) => {
        console.log("Contract History: ",response);

        return response;
    }),tap(event => {}, this.handleError)) 
   }

}
