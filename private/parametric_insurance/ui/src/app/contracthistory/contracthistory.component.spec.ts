import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ContracthistoryComponent } from './contracthistory.component';

describe('ContracthistoryComponent', () => {
  let component: ContracthistoryComponent;
  let fixture: ComponentFixture<ContracthistoryComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ContracthistoryComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ContracthistoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
