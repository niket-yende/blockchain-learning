import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from '../login/login.component';
import {RegisterComponent } from '../register/register.component'
import {OrganizerComponent} from '../organizer/organizer.component';
import {InsuranceComponent} from '../insurance/insurance.component';
import {PoliciesComponent} from '../policies/policies.component';
import {HomeorgComponent} from '../homeorg/homeorg.component';
import {ContracthistoryComponent} from '../contracthistory/contracthistory.component'


const appRoutes: Routes = [
  
    { path: '', redirectTo: '/login', pathMatch: 'full' },
   
    { path:'login',component:LoginComponent},
    {path:'register',component:RegisterComponent},
    { 
        path:'organizer/:id',
        component:OrganizerComponent,
        children:[
            {path:'',redirectTo:'home',pathMatch:'full'},
            {path:'home',component:HomeorgComponent},
            {path:'policies',component:PoliciesComponent},
            {path:'contractHistory/:key',component:ContracthistoryComponent}
        ],
      
        runGuardsAndResolvers:'always',
    },
    { path:'insuranceCompany/:id',component:InsuranceComponent},
    // {path:'organizer/:id/policies',component:PoliciesComponent}

    ];

    @NgModule({
    imports: [
        RouterModule.forRoot(appRoutes,{onSameUrlNavigation:'reload'})
    ],
    exports: [
        RouterModule
    ]
})
export class AppRoutingModule { }