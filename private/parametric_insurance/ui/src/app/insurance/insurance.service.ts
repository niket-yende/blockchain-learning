
import { Injectable } from '@angular/core';
import {Response} from '@angular/http';
import {HttpClient,HttpHeaders,HttpErrorResponse} from '@angular/common/http'
import { Observable } from 'rxjs';
import {tap,map,filter,catchError} from 'rxjs/operators'
import {conf} from '../config/config'

@Injectable({
  providedIn: 'root'
})
export class InsuranceService {

  constructor(private http:HttpClient) {
    

  }

   private handleError (err: HttpErrorResponse) {
		console.error(err);
		return err;
  }

getContracts(id):Observable<any>{

  return this.http.get(conf.URL+"contracts/company/"+id).pipe(map((response: Response) => {
          console.log("Response Query: ",response);

          return response;
      }),tap(event => {}, this.handleError)) 
  }

getWeather(id,data):Observable<any>{
  console.log(data)
  const httpOptions = {
            headers: new HttpHeaders({
            'Content-Type': 'Application/json; charset=UTF-8',
  
            
            }),
          };
   let body=JSON.stringify(data);

return this.http.get(conf.URL+"weather",{params:{'location':data.location,'startDate':data.startDate, 'endDate':data.endDate}})
.pipe(map((response: Response) => {
        console.log("Response Query: ",JSON.parse(JSON.stringify(response)));

        return response;
    }),tap(event => {}, this.handleError)) 
   }

  
}
