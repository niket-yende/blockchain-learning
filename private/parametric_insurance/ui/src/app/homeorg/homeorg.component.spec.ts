import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { HomeorgComponent } from './homeorg.component';

describe('HomeorgComponent', () => {
  let component: HomeorgComponent;
  let fixture: ComponentFixture<HomeorgComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ HomeorgComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(HomeorgComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
