var express = require('express');
var router = express.Router();



	
var express = require('express');
var router = express.Router();

/* GET home page. */
router.get('/', function(req, res, next) {
	var user = req.session.user;
	
	if(user !== undefined ){		
		res.render('home', { user: user});      	
	} else {
		req.session.destroy();
		res.render('login', {});
	}
});

module.exports = router;