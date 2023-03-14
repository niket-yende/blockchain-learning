(function () {
    'use strict';

    angular
        .module('syndicatedloan')
        .factory('RESTService', Factory);
	 var masterdata = [];
    function Factory($http, $state, $rootScope, $timeout,$window) {
        var factory = {};

        factory.put = REST_put;
		factory.get = REST_get;
		factory.post = REST_post;
		factory.delete = REST_delete;
		factory.fileupload = REST_fileupload;

        return factory;

        function REST_put(url, data, config, callback) {
			$http.put(url, data, config)
                .then(function (response) {					
                    callback(response);
                }, function(error) {
					var key;
					if(error.message!==undefined){
						var ms = error.message.split("=");
						key = ms[ms.length-1];	
					}					
							
					if(	(error.status !== undefined && error.status ===401) || (key !== undefined  && key.trim()==='Unauthorized'))				
						$window.location.href = '/login';
					else
						callback(error);
					 
				}).catch(function(response){				
					callback(response);
				});

        }
		
		function REST_get(url, data, callback) {
			$http.get(url, { params:  data })
                .then(function (response) {	
                    callback(response);					
                }, function(error) {
					var key;
					if(error.message!==undefined){
						var ms = error.message.split("=");
						key = ms[ms.length-1];	
					}							
					if(	(error.status !== undefined && error.status ===401) || (key !== undefined  && key.trim()==='Unauthorized'))				
						$window.location.href = '/login';
					else
						callback(error);
					
				});

        }
		
		function REST_post(url, data, callback) {
			$http.post(url, data)
                .then(function (response) {									
                    callback(response);
                }, function(error) {
					var key;
					if(error.message!==undefined){
						var ms = error.message.split("=");
						key = ms[ms.length-1];	
					}				
							
					if(	(error.status !== undefined && error.status ===401) || (key !== undefined  && key.trim()==='Unauthorized'))				
						$window.location.href = '/login';
					else
						callback(error);
					
				}).catch(function(response){						
					callback(response);
				});

        }
		
		function REST_delete(url, data, callback) {
			$http.delete(url, { params:  data })
                .then(function (response) {						
                    callback(response);
                }, function(error) {
					var key;
					if(error.message!==undefined){
						var ms = error.message.split("=");
						key = ms[ms.length-1];	
					}					
							
					if(	(error.status !== undefined && error.status ===401) || (key !== undefined  && key.trim()==='Unauthorized'))				
						$window.location.href = '/login';
					else
						callback(error);
					
				}).catch(function(response){					
					callback(response);
				});

		}
		
		function REST_fileupload(url, data, callback) {
			$http.post(url, data,{				
					headers: {'Content-Type': undefined ,'Accept':'application/json'},
					transformRequest: angular.identity
				})
                .then(function (response) {						
                    callback(response);
                }, function(error) {
					var key;
					if(error.message!==undefined){
						var ms = error.message.split("=");
						key = ms[ms.length-1];	
					}					
							
					if(	(error.status !== undefined && error.status ===401) || (key !== undefined  && key.trim()==='Unauthorized'))				
						$window.location.href = '/login';
					else
						callback(error);
					
				}).catch(function(response){					
					callback(response);
				});

        }
    }
})();

