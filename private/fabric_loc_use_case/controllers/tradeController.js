/*jslint node: true, nomen: true*/
"use strict";
var path = require("path");
var networkClient = require(path.join(
  "..",
  "middleware",
  "invokeNetworkClient.js"
));

module.exports = function(logger, config) {
  var funcs = {};

  funcs.requestTrade = function(details, cb) {
    //Passing a customer object from UI to Backend
    var requestParams = {};
    console.log("requestTrade ", details.trade);
    requestParams["funcName"] = "requestTrade";
    requestParams["args"] = details.trade;
    requestParams["userOrg"] = details.userOrg;
    requestParams["userName"] = details.userName;

    console.log("requestParams : ", requestParams);
    // cb(null, requestParams);
    networkClient.addingPeer(requestParams.userOrg, function(err, data) {
      if (err) {
        logger.info(" peer already added: ");
      } else {
        requestParams["storepath"] = data;
        networkClient.invokeClient(requestParams, function(err, data) {
          if (err) {
            cb(err, null);
          } else {
            logger.info("New request Trade added: ", data);
            cb(null, data);
          }
        });
      }
    });
  };

  funcs.acceptTrade = function(details, cb) {
    //Passing a customer object from UI to Backend
    var requestParams = {};
    requestParams["funcName"] = "acceptTrade";
    requestParams["args"] = details.trade;
    requestParams["userOrg"] = details.userOrg;
    requestParams["userName"] = details.userName;
    console.log("requestParams : ", requestParams);

    networkClient.addingPeer(requestParams.userOrg, function(err, data) {
      if (err) {
        logger.info(" peer already added: ");
      } else {
        requestParams["storepath"] = data;
        networkClient.invokeClient(requestParams, function(err, data) {
          if (err) {
            cb(err, null);
          } else {
            logger.info("New request Trade added: ", data);
            cb(null, data);
          }
        });
      }
    });
  };

  funcs.requestLC = function(details, cb) {
    //Passing a customer object from UI to Backend
    var requestParams = {};
    requestParams["funcName"] = "requestLC";
    requestParams["args"] = details.trade;
    requestParams["userOrg"] = details.userOrg;
    requestParams["userName"] = details.userName;
    console.log("requestParams : ", requestParams);
    networkClient.addingPeer(requestParams.userOrg, function(err, data) {
      if (err) {
        logger.info(" peer already added: ");
      } else {
        requestParams["storepath"] = data;
        networkClient.invokeClient(requestParams, function(err, data) {
          if (err) {
            cb(err, null);
          } else {
            logger.info("LC Requested", data);
            cb(null, data);
          }
        });
      }
    });
  };
  funcs.issueLC = function(details, cb) {
    //Passing a customer object from UI to Backend
    var requestParams = {};
    requestParams["funcName"] = "issueLC";
    requestParams["args"] = details.trade;
    requestParams["userOrg"] = details.userOrg;
    requestParams["userName"] = details.userName;
    console.log("requestParams : ", requestParams);
    networkClient.addingPeer(requestParams.userOrg, function(err, data) {
      if (err) {
        logger.info(" peer already added: ");
      } else {
        requestParams["storepath"] = data;
        networkClient.invokeClient(requestParams, function(err, data) {
          if (err) {
            cb(err, null);
          } else {
            logger.info("LC Issued", data);
            cb(null, data);
          }
        });
      }
    });
  };
  funcs.acceptLC = function(details, cb) {
    //Passing a customer object from UI to Backend
    var requestParams = {};
    requestParams["funcName"] = "acceptLC";
    requestParams["args"] = details.trade;
    requestParams["userOrg"] = details.userOrg;
    requestParams["userName"] = details.userName;
    console.log("requestParams : ", requestParams);
    networkClient.addingPeer(requestParams.userOrg, function(err, data) {
      if (err) {
        logger.info(" peer already added: ");
      } else {
        requestParams["storepath"] = data;
        networkClient.invokeClient(requestParams, function(err, data) {
          if (err) {
            cb(err, null);
          } else {
            logger.info("LC Accepted", data);
            cb(null, data);
          }
        });
      }
    });
  };
  return funcs;
};
