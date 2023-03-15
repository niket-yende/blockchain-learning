"use strict";
var ClientUtils = require("../middleware/ClientUtils");

module.exports = function(logger, config) {
    var funcs = {};

    funcs.login = function(username, orgName, password, token, cb){        
        ClientUtils.init();
        ClientUtils.getClientUser(orgName, username, password)
        .then(response => {
            logger.debug(
            "-- returned from registering (logging in) the username %s for organization %s",
            username,
            orgName
            );
    
            if (response && typeof response !== "string") {
            var resp = {};
            resp.token = token;
            resp.success = true;
            if (
                response._enrollmentSecret &&
                response._enrollmentSecret.length > 0
            ) {
                logger.debug(
                "Successfully registered the username %s for organization %s",
                username,
                orgName
                );
                resp.secret = response._enrollmentSecret;
                resp.message = "Registration successful";
            } else {
                logger.debug(
                "Successfully enrolled the username %s for organization %s",
                username,
                orgName
                );
                resp.message = "Login successful";
            }
            cb(null, resp);
            } else {
            logger.debug(
                "Failed to register the username %s for organization %s with::%s",
                username,
                orgName,
                response
            );
            var message = "Registration/login failed";
            if (response) {
                message = JSON.stringify(response);
            }
            cb(null, message);
            }            
        })
          .catch(err => {
            console.error(err);
            logger.debug(
              "Failed to register username %s for organization %s with::%s",
              username,
              orgName,
              err.message
            );
            cb(err, null);
          });
        logger.debug("Org name  : " + orgName);
    }
    return funcs;
}