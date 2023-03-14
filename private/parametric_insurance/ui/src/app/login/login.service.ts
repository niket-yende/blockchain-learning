import { Injectable } from '@angular/core';
import { ActivatedRoute,Router} from '@angular/router';
import {Response} from '@angular/http'
import {HttpClient} from '@angular/common/http'
import { Login } from './login';
import { Observable } from 'rxjs';
import {map,filter,catchError} from 'rxjs/operators'

@Injectable({
  providedIn: 'root'
})
export class LoginService {

  constructor(private http:HttpClient) { }
  validateUser(login:Login):Observable<any>{
    console.log("Service:",login)
    return this.http.get('assets/users/users.json')
            .pipe(
              map(this.extractData),
              catchError(this.handleErrorObservable)
            )
            
  }
  private extractData(res: Response) {
	    let body = res;
        return body;
    }
   private handleErrorObservable (error: Response | any) {
		console.error(error.message || error);
		return Observable.throw(error.message || error);
    }


}
