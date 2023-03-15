/*jslint node: true, nomen: true*/
"use strict";

/**
 * Reference - https://github.com/node-cron/node-cron
 *           - https://stackoverflow.com/questions/9765215/global-variable-in-app-js-accessible-in-routes
 */

module.exports = function(express, logger, config) {
  var path = require("path"),
    router = express.Router(),
    shipmentController = require(path.join(
      "..",
      "controllers",
      "shipmentController"
    ))(logger, config);

  /* stop the scheduler job */
  router.post("/prepareShipment", function(req, res) {
    var trade = [];
    trade[0] = req.body.tradeId;
    trade[1] = req.body.source;
    trade[2] = req.body.destination;
    // var tradeId = req.body.tradeId;
    var details = {};
    details.trade = trade;
    details.userName = req.body.userName;
    details.userOrg = req.body.userOrg;
    shipmentController.prepareShipment(details, function(err, data) {
      if (err) {
        res.status(err.status).json({
          message: err.message
        });
      } else {
        res.status(200).json(data);
      }
    });
  });

  router.post("/acceptShipmentAndIssueBL", function(req, res) {
    var trade = [];
    trade[0] = req.body.tradeId;
    trade[1] = req.body.blId;
    trade[2] = req.body.expDate;

    var details = {};
    details.trade = trade;
    details.userName = req.body.userName;
    details.userOrg = req.body.userOrg;

    shipmentController.acceptShipmentAndIssueBL(details, function(err, data) {
      if (err) {
        res.status(err.status).json({
          message: err.message
        });
      } else {
        res.status(200).json(data);
      }
    });
  });

  router.post("/makePayment", function(req, res) {
    var trade = [];
    trade[0] = req.body.tradeId;
    trade[1] = req.body.payment;
    // var tradeId = req.body.tradeId;
    var details = {};
    details.trade = trade;
    details.userName = req.body.userName;
    details.userOrg = req.body.userOrg;
    shipmentController.makePayment(details, function(err, data) {
      if (err) {
        res.status(err.status).json({
          message: err.message
        });
      } else {
        res.status(200).json(data);
      }
    });
  });
  router.post("/releasePayment", function(req, res) {
    var trade = [];
    trade[0] = req.body.tradeId;
    // var tradeId = req.body.tradeId;
    var details = {};
    details.trade = trade;
    details.userName = req.body.userName;
    details.userOrg = req.body.userOrg;
    shipmentController.releasePayment(details, function(err, data) {
      if (err) {
        res.status(err.status).json({
          message: err.message
        });
      } else {
        res.status(200).json(data);
      }
    });
  });

  router.post("/requestPayment", function(req, res) {
    var trade = [];
    trade[0] = req.body.tradeId;
    // var tradeId = req.body.tradeId;
    var details = {};
    details.trade = trade;
    details.userName = req.body.userName;
    details.userOrg = req.body.userOrg;
    shipmentController.requestPayment(details, function(err, data) {
      if (err) {
        console.log("sr"+err.message);
        res.send({
          error: err.message
        });
        
      } else {
        res.status(200).json(data);
      }
    });
  });

  router.post("/updateShipmentLocation", function(req, res) {
    var trade = [];
    trade[0] = req.body.tradeId;
    trade[1] = req.body.location;

    var details = {};
    details.trade = trade;
    details.userName = req.body.userName;
    details.userOrg = req.body.userOrg;

    shipmentController.updateShipmentLocation(details, function(err, data) {
      if (err) {
        res.status(err.status).json({
          message: err.message
        });
      } else {
        res.status(200).json(data);
      }
    });
  });

  return router;
};
