/*jslint node: true, nomen: true*/
"use strict";
var path = require("path");
var networkClient = require(path.join(
  "..",
  "middleware",
  "queryNetworkClient.js"
));

module.exports = function(logger, config) {
  var funcs = {};

  funcs.fetchTradeStatus = function(details, cb) {
    var requestParams = {};
    requestParams["funcName"] = "getTradeStatus";
    requestParams["args"] = details.trade;
    requestParams["userOrg"] = details.userOrg;
    requestParams["userName"] = details.userName;
    console.log("requestParams : ", requestParams);
    networkClient.addingPeer(requestParams.userOrg, function(err, data) {
      if (err) {
        logger.info(" peer already added: ");
      } else {
        requestParams["storepath"] = data;
        networkClient.queryClient(requestParams, function(err, tradeStatus) {
          if (err) {
            cb(err, null);
          } else {
            logger.info("Returning trade status ", tradeStatus);
            cb(null, tradeStatus);
          }
        });
      }
    });
  };

  funcs.getLcStatus = function(details, cb) {
    var requestParams = {};
    requestParams["funcName"] = "getLCStatus";
    requestParams["args"] = details.trade;
    requestParams["userOrg"] = details.userOrg;
    requestParams["userName"] = details.userName;
    console.log("requestParams : ", requestParams);
    networkClient.addingPeer(requestParams.userOrg, function(err, data) {
      if (err) {
        logger.info(" peer already added: ");
      } else {
        requestParams["storepath"] = data;
        networkClient.queryClient(requestParams, function(err, lcStatus) {
          if (err) {
            cb(err, null);
          } else {
            logger.info("Returning letter of credit status ", lcStatus);
            cb(null, lcStatus);
          }
        });
      }
    });
  };

  funcs.getShipmentLocation = function(details, cb) {
    var requestParams = {};
    requestParams["funcName"] = "getShipmentLocation";
    requestParams["args"] = details.trade;
    requestParams["userOrg"] = details.userOrg;
    requestParams["userName"] = details.userName;
    console.log("requestParams : ", requestParams);
    networkClient.addingPeer(requestParams.userOrg, function(err, data) {
      if (err) {
        logger.info(" peer already added: ");
      } else {
        requestParams["storepath"] = data;
        networkClient.queryClient(requestParams, function(err, shipmentLoc) {
          if (err) {
            cb(err, null);
          } else {
            logger.info("Returning shipment location ", shipmentLoc);
            cb(null, shipmentLoc);
          }
        });
      }
    });
  };
  funcs.getPaymentDetails = function(details, cb) {
    var requestParams = {};
    requestParams["funcName"] = "getPaymentDetails";
    requestParams["args"] = details.trade;
    requestParams["userOrg"] = details.userOrg;
    requestParams["userName"] = details.userName;
    console.log("requestParams : ", requestParams);
    networkClient.addingPeer(requestParams.userOrg, function(err, data) {
      if (err) {
        logger.info(" peer already added: ");
      } else {
        requestParams["storepath"] = data;
        networkClient.queryClient(requestParams, function(err, PaymentDetails) {
          if (err) {
            cb(err, null);
          } else {
            logger.info("Returning PaymentDetails ", PaymentDetails);
            cb(null, PaymentDetails);
          }
        });
      }
    });
  };

  funcs.getBillOfLadding = function(details, cb) {
    var requestParams = {};
    requestParams["funcName"] = "getBillOfLading";
    requestParams["args"] = details.trade;
    requestParams["userOrg"] = details.userOrg;
    requestParams["userName"] = details.userName;
    console.log("requestParams : ", requestParams);
    networkClient.addingPeer(requestParams.userOrg, function(err, data) {
      if (err) {
        logger.info(" peer already added: ");
      } else {
        requestParams["storepath"] = data;
        networkClient.queryClient(requestParams, function(err, bl) {
          if (err) {
            cb(err, null);
          } else {
            logger.info("Returning bill of ladding ", bl);
            cb(null, bl);
          }
        });
      }
    });
  };

  funcs.getAccountBalance = function(details, cb) {
    // console.log('in fetch trades:' + trade);
    // var tradeId = trade.tradeId;
    // //Below selector for the query has to be altered
    // if (tradeId) {
    //   //fetchTradeTransactions(input, cb)
    //   var selector = {
    //         selector: {docType: "", }
    // };
    // } else {
    //   // fetchTrades(input, cb)
    //   var selector = {};
    // }
    var requestParams = {};
    requestParams["funcName"] = "getAccountBalance";
    requestParams["args"] = details.trade;
    requestParams["userOrg"] = details.userOrg;
    requestParams["userName"] = details.userName;
    console.log("requestParams : ", requestParams);
    networkClient.addingPeer(requestParams.userOrg, function(err, data) {
      if (err) {
        logger.info(" peer already added: ");
      } else {
        requestParams["storepath"] = data;
        networkClient.queryClient(requestParams, function(err, accBalance) {
          if (err) {
            cb(err, null);
          } else {
            logger.info("Returning bill of ladding ", accBalance);
            cb(null, accBalance);
          }
        });
      }
    });
  };

  funcs.getListOfTrades = function(details, cb) {
    var requestParams = {};
    requestParams["funcName"] = "queryState";
    requestParams["args"] = [
      JSON.stringify({
        selector: { doctype: "tradeAgreement" }
      })
    ];
    requestParams["userOrg"] = details.userOrg;
    requestParams["userName"] = details.userName;
    console.log("requestParams : ", requestParams);
    networkClient.addingPeer(requestParams.userOrg, function(err, data) {
      if (err) {
        logger.info(" peer already added: ");
      } else {
        requestParams["storepath"] = data;
        networkClient.queryClient(requestParams, function(err, lot) {
          if (err) {
            cb(err, null);
          } else {
            logger.info("List of Trades ", lot);
            cb(null, lot);
          }
        });
      }
    });
  };
  funcs.getListOfLC = function(details, cb) {
    var requestParams = {};
    requestParams["funcName"] = "queryState";
    requestParams["args"] = [
      JSON.stringify({
        selector: { doctype: "LOC" }
      })
    ];
    requestParams["userOrg"] = details.userOrg;
    requestParams["userName"] = details.userName;
    console.log("requestParams : ", requestParams);
    networkClient.addingPeer(requestParams.userOrg, function(err, data) {
      if (err) {
        logger.info(" peer already added: ");
      } else {
        requestParams["storepath"] = data;
        networkClient.queryClient(requestParams, function(err, lot) {
          if (err) {
            cb(err, null);
          } else {
            logger.info("List of LC ", lot);
            cb(null, lot);
          }
        });
      }
    });
  };
  funcs.getListOfBOL = function(details, cb) {
    var requestParams = {};
    requestParams["funcName"] = "queryState";
    requestParams["args"] = [
      JSON.stringify({
        selector: { doctype: "BOL" }
      })
    ];
    requestParams["userOrg"] = details.userOrg;
    requestParams["userName"] = details.userName;
    console.log("requestParams : ", requestParams);
    networkClient.addingPeer(requestParams.userOrg, function(err, data) {
      if (err) {
        logger.info(" peer already added: ");
      } else {
        requestParams["storepath"] = data;
        networkClient.queryClient(requestParams, function(err, lot) {
          if (err) {
            cb(err, null);
          } else {
            logger.info("List of BOL ", lot);
            cb(null, lot);
          }
        });
      }
    });
  };
  return funcs;
};
