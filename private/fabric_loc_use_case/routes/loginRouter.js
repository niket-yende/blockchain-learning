var expressJWT = require("express-jwt");
var jwt = require("jsonwebtoken");
var bearerToken = require("express-bearer-token");

module.exports = function(express, logger, config) {
    var path = require("path"),
      router = express.Router(),
      loginController = require(path.join("..","controllers","loginController"))(
        logger, 
        config
      );
	var app = express();

	app.set("secret", "thisismysecret");
    app.use(
    expressJWT({
        secret: "thisismysecret"
    }).unless({
        path: ["/login"]
    })
    );
    app.use(bearerToken());

    function getErrorMessage(field) {
        var response = {
            success: false,
            message: field + " field is missing or Invalid in the request"
        };
        return response;
    }
  
    /* stop the scheduler job */
    router.post("/login", function(req, res) {
      var username = req.body.username;
      var orgName = req.body.orgName;
      var password = req.body.password;
        if (!username) {
        res.json(getErrorMessage("'username'"));
        return;
        }
        if (!orgName) {
        res.json(getErrorMessage("'orgName'"));
        return;
        }
        console.log("2");
        // Hardcode single 'admin' user per org for now
        if (username === "admin" && !password) {
        res.json(getErrorMessage("'password'"));
        return;
        }
        logger.debug("3");
        var token = jwt.sign(
        {
            // Make the token expire 60 seconds from now
            exp: Math.floor(Date.now() / 1000) + 69 * 60,
            username: username,
            orgName: orgName
        },
        app.get("secret")
        );
		loginController.login(username, orgName, password, token, function (err, data) {
        if (err) {
          res.statusCode = 500;
          res.json(err.message);
          res.end(err.message);
        } else {
        
          res.end(JSON.stringify(data));
        }
      });
	});
    return router;
}
