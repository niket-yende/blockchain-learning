var Q = require('q');
var request = require('request');
var service = {};

service.post = post;
service.postFile = postFile;
service.get = get;
service.put = put;

module.exports = service;


function post(input) {

	var deferred = Q.defer();

	var request_obj = {
            "headers": { "content-type": "application/json", "PartyName": input.header}, 
			'url': input.url,
            'body' : JSON.stringify(input.params)
	}

	console.log("URL start",JSON.stringify(request_obj.url));
	console.log("data JSON",JSON.stringify(request_obj.body));

	request.post(request_obj, function (error, response, body) {
		var res_obj = {};
		console.log("response: ",JSON.stringify(response));

		if (error) {
			console.log("data err response:",error);
			deferred.reject(error.message);
		}
		else if (response.statusCode == 200) {

			if(body !== undefined){
				res_obj.message=response.body.message;
				res_obj.status=1;
			}
			else{
				res_obj.message='Rest Connection Error';
				res_obj.status=2;
			}
			console.log("data res_obj:",res_obj);
			deferred.resolve(res_obj);

		} else {
			res_obj.message='Blockchain error, Check connection details';
			res_obj.status=2;
			console.log("data err res_obj:",res_obj);
			deferred.resolve(res_obj);
		}

	});

	return deferred.promise;
}

function postFile(input) {

	var deferred = Q.defer();	

	var request_obj = {
			'headers': {"PartyName": input.header},      
			'url': input.url,
            'formData' : input.params
	}

	console.log("URL start",JSON.stringify(request_obj.url));
	console.log("data JSON",JSON.stringify(request_obj.body));

	request.post(request_obj, function (error, response, body) {
		var res_obj = {};
		console.log("response: ",JSON.stringify(response));

		if (error) {
			console.log("data err response:",error);
			deferred.reject(error.message);
		}
		else if (response.statusCode == 200) {

			if(body !== undefined){
				res_obj.message=response.body.message;
				res_obj.status=1;
			}
			else{
				res_obj.message='Rest Connection Error';
				res_obj.status=2;
			}
			console.log("data res_obj:",res_obj);
			deferred.resolve(res_obj);

		} else {
			res_obj.message='Blockchain error, Check connection details';
			res_obj.status=2;
			console.log("data err res_obj:",res_obj);
			deferred.resolve(res_obj);
		}

	});

	return deferred.promise;
}

function get(input) {

	var deferred = Q.defer();
    var res_obj = {};

	console.log("input.url######################",input.url);
	request.get({
        "headers": { "content-type": "application/json", "PartyName": input.header },
        "url": input.url,
	}
	, (error, response) => {

		if(error) {
			return console.log(error);
			deferred.reject(error.message);
		}
		else if (response.statusCode == 200) {
		
			if(response.body !== undefined){
				res_obj.message=response.body;
				res_obj.status=1;
			}
			else{
				res_obj.message='Rest Connection Error';
				res_obj.status=2;
			}
			deferred.resolve(res_obj);

		} else {
			res_obj.message='Blockchain error, Check connection details';
			res_obj.status=3;
			console.log("data err res_obj: ",res_obj);
			deferred.resolve(res_obj);
		}
		
	});
	
	
	return deferred.promise;
}

function put(input) {

	var deferred = Q.defer();

	var request_obj = {
            "headers": { "content-type": "application/json", "PartyName":input.header }, 
			'url': input.url,
            'body' : JSON.stringify(input.params)
	}

	console.log("URL start",JSON.stringify(request_obj.url));
	console.log("data JSON",JSON.stringify(request_obj.body));

	request.put(request_obj, function (error, response, body) {
		var res_obj = {};
		console.log("response: ",JSON.stringify(response));

		if (error) {
			console.log("data err response:",error);
			deferred.reject(error.message);
		}
		else if (response.statusCode == 200) {

			if(body !== undefined){
				res_obj.message=response.body.message;
				res_obj.status=1;
			}
			else{
				res_obj.message='Rest Connection Error';
				res_obj.status=2;
			}
			console.log("data res_obj:",res_obj);
			deferred.resolve(res_obj);

		} else {
			res_obj.message='Blockchain error, Check connection details';
			res_obj.status=2;
			console.log("data err res_obj:",res_obj);
			deferred.resolve(res_obj);
		}

	});

	return deferred.promise;
}