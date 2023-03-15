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

  funcs.prepareShipment = function(details, cb) {
    var requestParams = {};
    // console.log("prepareShipment for tradeId ", tradeId);
    requestParams["funcName"] = "prepareShipment";
    requestParams["args"] = details.trade;
    requestParams["userOrg"] = details.userOrg;
    requestParams["userName"] = details.userName;
    console.log("requestParams : ", requestParams);
    // cb(null,requestParams);

    // networkClient.invokeClient(requestParams, function(err) {
    //   if (err) {
    //     cb(err, null);
    //   }

    // });
    networkClient.addingPeer(requestParams.userOrg, function(err, data) {
      if (err) {
        logger.info(" peer already added: ");
      } else {
        requestParams["storepath"] = data;
        networkClient.invokeClient(requestParams, function(err, data) {
          if (err) {
            cb(err, null);
          } else {
            logger.info("shipment prepared : ", data);
            cb(null, data);
          }
        });
      }
    });
  };

  funcs.acceptShipmentAndIssueBL = function(details, cb) {
    var requestParams = {};
    console.log("Shipment accepted for tradeId ", details.trade.tradeId);
    requestParams["funcName"] = "acceptShipmentAndIssueBL";

    requestParams["args"] = details.trade;
    requestParams["userOrg"] = details.userOrg;
    requestParams["userName"] = details.userName;

    console.log("requestParams : ", requestParams);
    // cb(null,requestParams);
    // networkClient.invokeClient(requestParams, function(err) {
    //   if (err) {
    //     cb(err, null);
    //   }

    // });
    networkClient.addingPeer(requestParams.userOrg, function(err, data) {
      if (err) {
        logger.info(" peer already added: ");
      } else {
        requestParams["storepath"] = data;
        networkClient.invokeClient(requestParams, function(err, data) {
          if (err) {
            cb(err, null);
          } else {
            logger.info("Shipment accepted and BL issued: ", data);
            cb(null, data);
          }
        });
      }
    });
  };

  funcs.makePayment = function(details, cb) {
    var requestParams = {};

    // console.log("Payment made for tradeId ", details.trade.tradeId);
    requestParams["funcName"] = "makePayment";

    requestParams["args"] = details.trade;
    requestParams["userOrg"] = details.userOrg;
    requestParams["userName"] = details.userName;
    console.log("requestParams : ", requestParams);
    // cb(null,requestParams);

    // networkClient.invokeClient(requestParams, function(err) {
    //   if (err) {
    //     cb(err, null);
    //   }

    // });

    networkClient.addingPeer(requestParams.userOrg, function(err, data) {
      if (err) {
        logger.info(" peer already added: ");
      } else {
        requestParams["storepath"] = data;
        networkClient.invokeClient(requestParams, function(err, data) {
          if (err) {
            cb(err, null);
          } else {
            logger.info("payment made: ", data);
            cb(null, data);
          }
        });
      }
    });
  };

  funcs.releasePayment = function(details, cb) {
    var requestParams = {};

    // console.log("Payment made for tradeId ", details.trade.tradeId);
    requestParams["funcName"] = "releasePayment";

    requestParams["args"] = details.trade;
    requestParams["userOrg"] = details.userOrg;
    requestParams["userName"] = details.userName;
    console.log("requestParams : ", requestParams);
    // cb(null,requestParams);

    // networkClient.invokeClient(requestParams, function(err) {
    //   if (err) {
    //     cb(err, null);
    //   }

    // });

    networkClient.addingPeer(requestParams.userOrg, function(err, data) {
      if (err) {
        logger.info(" peer already added: ");
      } else {
        requestParams["storepath"] = data;
        networkClient.invokeClient(requestParams, function(err, data) {
          if (err) {
            cb(err, null);
          } else {
            logger.info("payment made: ", data);
            cb(null, data);
          }
        });
      }
    });
  };

  funcs.requestPayment = function(details, cb) {
    var requestParams = {};
    console.log("payment requested for tradeId ", details.trade.tradeId);
    requestParams["funcName"] = "requestPayment";

    requestParams["args"] = details.trade;
    requestParams["userOrg"] = details.userOrg;
    requestParams["userName"] = details.userName;
    console.log("requestParams : ", requestParams);
    // cb(null,requestParams);
    // networkClient.invokeClient(requestParams, function(err) {
    //   if (err) {
    //     cb(err, null);
    //   }

    // });
    networkClient.addingPeer(requestParams.userOrg, function(err, data) {
      if (err) {
        logger.info(" peer already added: ");
      } else {
        requestParams["storepath"] = data;
        networkClient.invokeClient(requestParams, function(err, data) {
          if (err) {
            console.log("sc"+err);
            cb(err, null);
          } else {
            logger.info("Payment requested: ", data);
            cb(null, data);
          }
        
        });
      }
    });
  };

  funcs.updateShipmentLocation = function(details, cb) {
    var requestParams = {};
    console.log("prepareShipment for tradeId ", details.trade.tradeId);
    requestParams["funcName"] = "updateShipmentLocation";

    requestParams["args"] = details.trade;
    requestParams["userOrg"] = details.userOrg;
    requestParams["userName"] = details.userName;
    console.log("requestParams : ", requestParams);
    // cb(null,requestParams);

    // networkClient.invokeClient(requestParams, function(err) {
    //   if (err) {
    //     cb(err, null);
    //   }

    // });
    networkClient.addingPeer(requestParams.userOrg, function(err, data) {
      if (err) {
        logger.info(" peer already added: ");
      } else {
        requestParams["storepath"] = data;
        networkClient.invokeClient(requestParams, function(err, data) {
          if (err) {
            cb(err, null);
          } else {
            logger.info("Location updated: ", data);
            cb(null, data);
          }
        });
      }
    });
  };

  return funcs;
};
