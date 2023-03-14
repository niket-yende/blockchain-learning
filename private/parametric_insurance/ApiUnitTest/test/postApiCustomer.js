var chai = require('chai');
var chaiHttp = require('chai-http');
var should = chai.should();
var customer = require('../customer.json');
var insuranceCompany = require('../insurance_company.json');
var insuranceContract = require('../insurance_contract.json');
var weather = require('../weather.json');
chai.use(chaiHttp);

var url = 'http://localhost:3000';

describe("Customer", function() {
    var erorr="";
    before("suit",function(){
        if(customer.name.length<2){
            erorr+="length of name is less than 2\n";  
        }
        if(!(customer.email.match("^.+@[a-zA-Z]+\.[a-zA-Z]+$"))){
            erorr+="invalid email id \n";
        }
        if(customer.address.length<1){
            erorr+="invalid address\n";
        }
        if(!(customer.contact.match("^[+0-9]{3}[0-9]{10}$"))){
            erorr+="invalid contact number\n";
        }
        if(erorr.length>0){
            throw new Error(erorr);
        }
    });
    
    
    it("create new customer", function(done) {
        this.timeout(15000);
        chai.request(url)
            .post('/api/customer')
            .send(customer)
            .end(function (err, res) {
                //console.log(res.body);
                // console.log(Date().date);
                res.should.have.status(200);              
                done();
            });
    });
});

describe("insurance company",function(){
    
    before("suit",function(){
        if(insuranceCompany.name.length<2){
            throw new Error("length of name is less than 2");  
        }
        if(!(insuranceCompany.email.match("^.+@[a-zA-Z]+\.[a-zA-Z]+$"))){
            throw new Error("invalid email id ");
        }
        if(insuranceCompany.address<1){
            throw new Error("invalid address");
        }else if(!(insuranceCompany.contact.match("^[+0-9]{3}[0-9]{10}$"))){
            throw new Error("invalid contact number");
        }
    });
    it("create new insurance company", function(done) {
            this.timeout(15000);
            chai.request(url)
                .post('/api/insuranceCompany')
                .send(insuranceCompany)
                .end(function (err, res) {
                    res.should.have.status(200);              
                    done();
                });
        });
    
});

describe("insurance contract",function(){
    
    before("suit",function(){
        if(insuranceContract.customerName.length<2){
            throw new Error("invalid name");
        }else if(insuranceContract.insuranceCompanyName.length<2){
            throw new Error("invalid company name");
        }else if(!(insuranceContract.amount.match("^[0-9]+(.[0-9]+)*$"))){
            throw new Error("invalid amount");
        }else if(insuranceContract.startDate<= new Date()){
            throw new Error("invalid start date");
        }else if(insuranceContract.endDate<=new Date()){
            throw new Error("invalid end date");
        }else if(insuranceContract.location.length<1){
            throw new Error("invalid location");
        }
    });
    it("register new insurance contract", function(done) {
            this.timeout(15000);
            chai.request(url)
                .post('/api/insuranceContract')
                .send(insuranceContract)
                .end(function (err, res) {
                    if(err){
                        console.log(err);
                    }
                    res.should.have.status(200);              
                    done();
                });
        });
});
describe("weather report",function(){
    
    before("suit",function(){
       
        if(weather.location.length<1){
            throw new Error("invalid location");
        }
        if(weather.weatherCriterias == null){
            throw new Error("invalid criterias");
        }
       
    });
       it("push weather report object", function(done) {
        this.timeout(15000);
        chai.request(url)
            .post('/api/weatherObject')
            .send(weather)

            .end(function (err, res) {
                res.should.have.status(200);              
                done();
            });
    });
});