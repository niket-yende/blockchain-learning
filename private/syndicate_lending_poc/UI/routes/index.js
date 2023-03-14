var express = require('express');
var path = require('path');
var router = express.Router();

module.exports = router;

router.get('/form-validation-messages.html',  function (req, res) { 
  res.sendFile(path.join(__dirname + "/../views/partials/form-validation-messages.html"));
});

router.get(/^\/(party-login|leadArranger-home.*|borrower-home.*|lender-home.*)$/, function (req, res) { 
  res.render('index.html', {title: 'Express'});
});
/* GET home page 
router.get('/', function(req, res, next) {
  res.render('login.html', {});
});*/

/* GET home page */
router.get('', function(req, res, next) {
  console.log("inside get")
  if(req.headers['cookies']){
    delete req.headers['cookies'];
  }
  res.render('index', {});
});
