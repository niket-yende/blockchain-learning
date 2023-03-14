var express = require('express');
var router = express.Router();

router.get('/', function (req, res) {
    // log user out
	console.log("logout /");
	req.session.destroy(function(err){
        if(err){
            console.log(err);
        } else {
            res.redirect('/login');
        }
    });
});

module.exports = router;