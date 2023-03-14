var express = require('express');
var router = express.Router();
var slServices = require('./../../services/n.users.services');
router.post('/register', register);
function register(req, res) {
	console.log("in register controller ");
	var user = {
		username: req.body.username,
        password: req.body.password,
        role: req.body.role
		
	};
	slServices.callLocalService('user','register',user).then(function (response)  {
		if(response){
			res.status(200).json(response);
		}          
	})
	.catch(function (err) {
		console.log("Error: register.js ", err);
		res.status(200).json({"message": err.message, "status":3});
	});
	
}


module.exports = router;