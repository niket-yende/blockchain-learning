var Q = require('q');
var userDataService = require('./users/n.user.service');

var service = {};

service.callLocalService = callLocalService;

module.exports = service;

function callLocalService(module, serviceName, params) {
	var deferred = Q.defer();
	console.log("callService :module:"+module+", serviceName:"+serviceName);
	if(module === 'user'){
		userDataService[serviceName](params).then(function (output) {
			deferred.resolve(output);				        
		})
		.catch(function (err) {								
			deferred.reject({message:'System Error at call service',status:2});
		});
	}
	
	
	return deferred.promise;
}

