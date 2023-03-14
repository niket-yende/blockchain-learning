/*jslint node: true, nomen: true*/
"use strict";
var path = require("path");
var networkClient = require(path.join(
  "..",
  "lib",
  "invoke_client",
  "invokeNetworkClient.js"
));

module.exports = function(logger, config) {
  var funcs = {};

  funcs.scheduleJob = function(cb) {
    //In this case args passed is a query string
    // var requestParams =
    
    var requestParams = {};
    requestParams["funcName"] = "schedulerJob";
    requestParams["args"] = "";
    console.log("requestParams : ", requestParams);
    
    networkClient.invokeClient(requestParams, function(err, data) {
      if (err) {
        cb(err, null);
      } else {
        logger.info("Schedule job data ", data);
        cb(null, data);
      }
    });
  };

  return funcs;
};
