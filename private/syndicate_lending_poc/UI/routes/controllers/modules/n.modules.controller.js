var config = require('../../../config.json');
var express = require('express');
var formidable = require('formidable');
var fs = require('fs');
var router = express.Router();

var corda_rest_services = require('./../../services/n.corda.rest.services');

router.post('/post', post);
router.post('/fileupload', fileupload);
router.get('/get', get);
router.put('/put', put);

function post(req, res){
	var username = req.decoded.data.username;
	var data = req.body;   
	console.log("data start",JSON.stringify(data)); 
	// var url=config.users[username].url+"/"+data.url; 
	var url=config.RestURL+"/"+data.url
	var header=config.users[username].PartyName;   

    corda_rest_services.post({url:url, params:data.params, header:header}).then(function (response) {
		console.log("post response###################",response);
		if(response){
			res.status(200).send(response);
		}
	})
	.catch(function (err) {
		res.status(200).send({"data": "" ,"message":err.message,"status":3});
	});
}

function fileupload(req, res){

	var form = new formidable.IncomingForm();
	var username = req.decoded.data.username; 
	
	form.parse(req, function (err, fields, files) {	
		if(err){
			console.error(err);
			res.status(400).send(err.message);
		}	
		// var url=config.users[username].url+"/"+fields.url;
		var url=config.RestURL+"/"+fields.url
		var header=config.users[username].PartyName;
		var formData = {
			loanRef:fields.loanRef,
			file: fs.createReadStream(files.file.path)
		};
		corda_rest_services.postFile({url:url, params:formData, header:header}).then(function (response) {
			console.log("post response###################",response);
			if(response){
				res.status(200).send(response);
			}
		})
		.catch(function (err) {
			res.status(200).send({"data": "" ,"message":err.message,"status":3});
		});			
	});	
}

function get(req, res){
	// var deferred = Q.defer();
    var username = req.decoded.data.username;
	var data = req.query;
	console.log("data start",JSON.stringify(data)); 
	// var url=config.users[username].url+"/"+data.url;
	var url=config.RestURL+"/"+data.url
	var header=config.users[username].PartyName;

    corda_rest_services.get({url:url, header:header}).then(function (response) {
		console.log("get response###################",response);
		if(response.status == 1){				
			if(response.message!="[]"){
				res.status(200).send({"data": response.message ,"message":"","status":1});
			}
			else{
				res.status(200).send({"data":"" ,"message":"No records found","status":2});
			}
		}
		else{	
			res.status(200).send({"data":"" ,"message":"Failed to load data","status":3});								
		}
	})
	.catch(function (err) {
		res.status(200).send({"data": "" ,"message":err.message,"status":2});
	});
}

function put(req, res){
	var username = req.decoded.data.username;
	var data = req.body;   
	console.log("data start",JSON.stringify(data)); 
	// var url=config.users[username].url+"/"+data.url;
	var url=config.RestURL+"/"+data.url
	var header=config.users[username].PartyName;    

    corda_rest_services.put({url:url, params:data.params, header:header}).then(function (response) {
		console.log("put response###################",response);
		if(response){
			res.status(200).send(response);
		}
	})
	.catch(function (err) {
		res.status(200).send({"data": "" ,"message":err.message,"status":3});
	});
}


module.exports = router;