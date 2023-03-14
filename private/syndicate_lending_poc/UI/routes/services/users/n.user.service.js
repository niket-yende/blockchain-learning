var config = require('../../../config.json');
var jwt = require('jsonwebtoken');
var Q = require('q');
var service = {};

service.authenticatedUser = authenticatedUser;
service.register = register;
module.exports = service;
function authenticatedUser(input) {
	console.log("authenticatedUser$$$$$$$$$$$$$$$$$$$");
    var deferred = Q.defer();
	var username = input.username;
    var password =  input.password;
    
    if(config.LoginDetails[username] != null){
        if(config.LoginDetails[username].PASSWORD == password){
            var	userData = {};
            userData.username = username;
            userData.password =config.LoginDetails[username].PASSWORD;
            userData.role = config.LoginDetails[username].ROLE;
            console.log("config.secret",config.secret);
            // authentication successful			
            deferred.resolve({"success": true,"Role":userData.role, "token": jwt.sign({ data: userData }, config.secret), "message": 'Authentication Successful'});	 //exp: Math.floor(Date.now() / 1000) + (60 * 60),    
        }else{
            deferred.resolve({'data': 'null', 'message': 'Username or password is incorrect', 'status': 2});		
        }
       
    } else {	
        deferred.resolve({'data': 'null', 'message': 'Username or password is incorrect', 'status': 2});		
    }
	
    return deferred.promise;
}


function register(userParam) {
    
}

