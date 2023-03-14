import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { LoginComponent } from './login/login.component';
import { HttpClientModule }     from '@angular/common/http';
import { HttpModule } from '@angular/http';
import { AppRoutingModule } from './app-routing/app-routing.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MaterialModule } from './material.module';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { AppComponent } from './app.component';
import { OrganizerComponent } from './organizer/organizer.component';
import { InsuranceComponent } from './insurance/insurance.component';
import { ChartsModule } from 'ng2-charts/ng2-charts';
import { RegisterComponent } from './register/register.component';
import { PoliciesComponent } from './policies/policies.component';
import { OwlDateTimeModule, OwlNativeDateTimeModule } from 'ng-pick-datetime';
import { HomeorgComponent } from './homeorg/homeorg.component';
import { MustMatchDirective } from './_helpers/must-match.directive';
import {Ng2TelInputModule} from 'ng2-tel-input';

import {AccordionModule} from 'primeng/accordion';     //accordion and accordion tab
import {MenuItem} from 'primeng/api';                 //api

import {DialogModule} from 'primeng/dialog';
import {PaginatorModule} from 'primeng/paginator';
import {TableModule} from 'primeng/table';
import {ToastModule} from 'primeng/toast';
import {OverlayPanelModule} from 'primeng/overlaypanel';
import {MessageService} from 'primeng/api';
import { ContracthistoryComponent } from './contracthistory/contracthistory.component';



@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    OrganizerComponent,
    InsuranceComponent,
    RegisterComponent,
    PoliciesComponent,
    HomeorgComponent,
    MustMatchDirective,
    ContracthistoryComponent,
     
    
  ],
  imports: [
    Ng2TelInputModule,
    ChartsModule,
    BrowserModule,
    BrowserAnimationsModule,
    MaterialModule,
    HttpModule,
    AppRoutingModule,
    HttpClientModule,
    FormsModule,
    ReactiveFormsModule,
    OwlDateTimeModule, 
    OwlNativeDateTimeModule,
    DialogModule,
    AccordionModule,
    TableModule,
    PaginatorModule,
    ToastModule,
    OverlayPanelModule
    


    // NgxCountrySelectModule

  ],
  providers: [Ng2TelInputModule,MessageService],
  bootstrap: [AppComponent]
})
export class AppModule { }
