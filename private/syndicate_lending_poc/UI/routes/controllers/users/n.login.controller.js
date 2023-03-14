var express = require('express');
var router = express.Router();
var slServices = require('./../../services/n.users.services');
router.post('/authenticate', userLogin);
function userLogin(req, res) {
	console.log("in authenticateUser controller ");
	var user = {
		username: req.body.username,
		password: req.body.password
		
	};
	slServices.callLocalService('user','authenticatedUser',user).then(function (response)  {
		if(response){
			res.status(200).json(response);
		}          
	})
	.catch(function (err) {
		console.log("Error: login.js ", err);
		res.status(200).json({"message": err.message, "status":3});
	});
	
}


module.exports = router;