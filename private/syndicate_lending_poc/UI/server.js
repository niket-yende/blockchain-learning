var express = require('express');
var fs = require('fs');
var uuid = require('uuid');
var config = require('./config.json');
var token = require('./token.json');

var app = express();

changeToken();

function changeToken(){
  var key = token.key;
  if(key == config.secret){
    config.secret = uuid.v1();
    fs.writeFileSync('./config.json', JSON.stringify(config));
  }
  token.key = config.secret;
  fs.writeFileSync('./token.json', JSON.stringify(token));

  console.log("Token history saved successfully");

  var path = require('path');
  var favicon = require('serve-favicon');
  var cookieParser = require('cookie-parser');
  var bodyParser = require('body-parser');
  var jwt = require('jsonwebtoken');

  var index = require('./routes/index');
  
 
  app.use('/views', express.static(__dirname + '/views'));
  app.set('view engine', 'html');
  app.engine('html', require('ejs').renderFile);

  app.use(bodyParser.json());
  app.use(bodyParser.urlencoded({ extended: false }));
  app.use(cookieParser());

  app.use(express.static(path.join(__dirname, 'node_modules')));
  app.use(express.static(path.join(__dirname, 'public')));

  app.post('/authenticate', require('./routes/controllers/users/n.login.controller'));

  app.get('/token-authentication-failed', function (req, res) {
    res.render('token-not-found.html', {loginUrl: req.query.url});
  });

  app.get(/^\/(party-login)$/, function (req, res) { 
    res.render('index.html', {});
  });

  app.use(function(req, res, next) {
    //var user = req.session.user;
    console.log("jwt middleware")
    // check header or url parameters or post parameters for token
    var token = req.body.token || req.query.token || req.headers['authorization'];
    //var token;
    //console.log("token1 is "+token);
    if(req.headers['cookie']){
      token = req.headers['cookie'].split("=")[1];
    }

   
    let url = '/party-login';        
    if (token) {           
      jwt.verify(token, config.secret, function(err, decoded) {      
        if (err) { 
          console.log('Failed to authenticate token.', err);
          res.status(403); 
          return res.render('token-not-found.html', {loginUrl: url});
        }
        // if everything is good, save to request for use in other routes
        req.decoded = decoded; 
        console.log("decoded: ", decoded)	;	
        next();
      });
    } else {
      res.status(403);
      return res.render('token-not-found.html', {loginUrl: url});
      
    }
  });

  app.use('/users', require('./routes/users'));
  app.use('/modules', require('./routes/controllers/modules/n.modules.controller'));
  app.use('/', index);
 
  // catch 404 and forward to error handler
  app.use(function(req, res, next) {
    var err = new Error('Not Found');
    err.status = 404;
    next(err);
  });

  // error handler
  app.use(function(err, req, res, next) {
    // set locals, only providing error in development
    res.locals.message = err.message;
    res.locals.error = req.app.get('env') === 'development' ? err : {};

    // render the error page
    res.status(err.status || 500);
    //res.render('error');
    console.log("err.status: "+err.status);
    console.log("err.message: "+err.message);
    //res.render('error', {title: 'ejs'});
    res.render('error', {title: 'Express'});
  });
}
  


module.exports = app;
