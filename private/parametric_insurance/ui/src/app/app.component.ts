import { Component } from '@angular/core';




@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})

export class AppComponent {
  title = 'Parametric Insurance';
  constructor(){
    document.body.classList.add('bg-img')
    
  }

}

