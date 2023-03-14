var chai = require('chai');
var chaiHttp = require('chai-http');
var should = chai.should();
var expect = require('chai').expect;
 
chai.use(chaiHttp);
   
var url = 'http://localhost:3000/api';
 
describe('Test get api', function(){
  it('Get all the contracts for an insurance company', function(done){
    chai.request(url)
        .get('/contracts/company/infosys')
        .end(function(err, res){
          // console.log(res.body);
          var data = JSON.parse(res.body.data);
          res.should.have.status(200);
          expect(res.body.status).to.equal('SUCCESS');
          
          // expect(data).to.not.be.empty;
         for(let i=0; i<data.length; i++){
          //  console.log(data[i].Key.length);
            expect(data[i].Key.length).to.be.above(0);
            expect(data[i].Record.contractID.length).to.be.above(0);
            expect(data[i].Record.customerName).not.null;
            expect(data[i].Record.docType.length).to.be.above(0);
            expect(data[i].Record.endDate.length).to.be.above(0);
            expect(data[i].Record.endDateTimestamp).to.be.above(0);
            expect(data[i].Record.insuranceCompanyName.length).to.be.above(0);
            expect(data[i].Record.insuranceCriterias).not.null;
            expect(data[i].Record.insuredAmount).to.be.above(0);
            expect(data[i].Record.issueDate.length).to.be.above(0);
            expect(data[i].Record.issueDateTimestamp).to.be.above(0);
            expect(data[i].Record.location.length).to.be.above(0);
            expect(data[i].Record.startDate.length).to.be.above(0);
            expect(data[i].Record.startDateTimestamp).to.be.above(0);
            expect(data[i].Record.status).to.be.oneOf(['ACTIVE','INACTIVE','CLAIMED','UNCLAIMED']);
          }
          done();
        });
 });
 
  it('Get all the contracts for a customer', function(done){
    var name = "Niket"
    chai.request(url)
        .get('/contracts/customer/'+name)
        .end(function(err, res){
          var data = JSON.parse(res.body.data); 
          res.should.have.status(200);
          expect(res.body.status).to.equal('SUCCESS');
          expect(data).to.not.be.empty;
          for(let i=0; i<data.length; i++){
            expect(data[i].Key.length).to.be.above(0);
            expect(data[i].Record.contractID.length).to.be.above(0);
            expect(data[i].Record.customerName.length).to.be.above(0);
            expect(data[i].Record.customerName).to.equal(name);
            expect(data[i].Record.docType.length).to.be.above(0);
            expect(data[i].Record.endDate.length).to.be.above(0);
            expect(data[i].Record.endDateTimestamp).to.be.above(0);
            expect(data[i].Record.insuranceCompanyName.length).to.be.above(0);
            expect(data[i].Record.insuranceCriterias).not.null;
            expect(data[i].Record.insuredAmount).to.be.above(0);
            expect(data[i].Record.issueDate.length).to.be.above(0);
            expect(data[i].Record.issueDateTimestamp).to.be.above(0);
            expect(data[i].Record.location.length).to.be.above(0);
            expect(data[i].Record.startDate.length).to.be.above(0);
            expect(data[i].Record.startDateTimestamp).to.be.above(0);
            expect(data[i].Record.status).to.be.oneOf(['ACTIVE','INACTIVE','CLAIMED','UNCLAIMED']);
          }
          done();
        });
  });
 
  it('Get weather related data for a location', function(done){
    chai.request(url)
        .get('/weather?location=Bangalore&startDate=2019-01-16T06:51:36Z&endDate=2019-01-19T06:51:36Z')
        .end(function(err, res){
          var data = JSON.parse(res.body.data);
          res.should.have.status(200);
          expect(res.body.status).to.equal('SUCCESS');
          expect(data).to.not.be.empty;
          for(let i=0; i<data.length; i++){
              expect(data[i].Key.length).to.be.above(0);
              expect(data[i].Record.dateTime.length).to.be.above(0);
              expect(data[i].Record.dateTimeTimestamp).to.be.above(0);
              expect(data[i].Record.docType.length).to.be.above(0);
              expect(data[i].Record.location.length).to.be.above(0);
              expect(data[i].Record.reportID.length).to.be.above(0);
              expect(data[i].Record.weatherCriterias).not.null;
          }
          done();
        });
  });

  it('history of a contract',function(){
    chai.request(url)
    .get('/contracts/history/CONT-NIK-INF-20190205061209')
    .end(function(err, res){
      var data = JSON.parse(res.body.data);
      res.should.have.status(200);
      expect(res.body.status).to.equal('SUCCESS');
      expect(data).to.not.be.empty;
      for(let i=0; i<data.length; i++){
          expect(data[i].TxId.length).to.be.above(0);
          expect(data[i].Value.contractID.length).to.be.above(0);
          expect(data[i].Value.docType.length).to.be.above(0);
          expect(data[i].Value.customerName.length).not.null;
          expect(data[i].Value.insuranceCompanyName.length).not.null;
          expect(data[i].Value.insuredAmount).to.be.above(0);
          expect(data[i].Value.insuranceCriterias).not.null;
          expect(data[i].Value.startDate.length).to.be.above(0);
          expect(data[i].Value.endDate.length).to.be.above(0);
          expect(data[i].Value.location.length).to.be.above(0);
          expect(data[i].Value.status).to.be.oneOf(['ACTIVE','INACTIVE','CLAIMED','UNCLAIMED']);
          expect(data[i].Value.issueDate.length).to.be.above(0);
          expect(data[i].Value.issueDateTimestamp).to.be.above(0);
          expect(data[i].Value.startDateTimeTimestamp).to.be.above(0);
          expect(data[i].Value.endDateTimeTimestamp).to.be.above(0);
          expect(data[i].Timestamp).to.be.above(0);
      }
      done();
    });
  });
});