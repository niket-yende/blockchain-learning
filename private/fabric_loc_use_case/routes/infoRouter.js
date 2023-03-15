/*jslint node: true, nomen: true*/
"use strict";

/**
 * Reference - https://github.com/node-cron/node-cron
 *           - https://stackoverflow.com/questions/9765215/global-variable-in-app-js-accessible-in-routes
 */

module.exports = function(express, logger, config) {
  var path = require("path"),
    router = express.Router(),
    infoController = require(path.join("..", "controllers", "infoController"))(
      logger,
      config
    );

  /* stop the scheduler job */
  router.get("/tradeStatus/:tradeId", function(req, res) {
    // console.log(req);
    var trade = [];
    trade[0] = req.params.tradeId;
    var details = {};
    details.trade = trade;
    details.userName = req.query.userName;
    details.userOrg = req.query.userOrg;

    infoController.fetchTradeStatus(details, function(err, data) {
      if (err) {
        res.statusCode = 500;
        res.end(err.message);
      } else {
        res.end(JSON.stringify(data));
      }
    });
  });

  router.get("/lcStatus/:tradeId", function(req, res) {
    // console.log(req);
    var trade = [];
    trade[0] = req.params.tradeId;
    var details = {};
    details.trade = trade;
    details.userName = req.query.userName;
    details.userOrg = req.query.userOrg;

    infoController.getLcStatus(details, function(err, data) {
      if (err) {
        res.statusCode = 500;
        res.end(err.message);
      } else {
        res.end(JSON.stringify(data));
      }
    });
  });

  router.get("/shipmentLocation/:tradeId", function(req, res) {
    // console.log(req);
    var trade = [];
    trade[0] = req.params.tradeId;
    var details = {};
    details.trade = trade;
    details.userName = req.query.userName;
    details.userOrg = req.query.userOrg;

    infoController.getShipmentLocation(details, function(err, data) {
      if (err) {
        res.statusCode = 500;
        res.end(err.message);
      } else {
        res.end(JSON.stringify(data));
      }
    });
  });
  router.get("/paymentDetails/:tradeId", function(req, res) {
    // console.log(req);
    var trade = [];
    trade[0] = req.params.tradeId;
    var details = {};
    details.trade = trade;
    details.userName = req.query.userName;
    details.userOrg = req.query.userOrg;

    infoController.getPaymentDetails(details, function(err, data) {
      if (err) {
        res.statusCode = 500;
        res.end(err.message);
      } else {
        res.end(JSON.stringify(data));
      }
    });
  });

  router.get("/billOfLadding/:tradeId", function(req, res) {
    // console.log(req);
    var trade = [];
    trade[0] = req.params.tradeId;
    var details = {};
    details.trade = trade;
    details.userName = req.query.userName;
    details.userOrg = req.query.userOrg;

    infoController.getBillOfLadding(details, function(err, data) {
      if (err) {
        res.statusCode = 500;
        res.end(err.message);
      } else {
        res.end(JSON.stringify(data));
      }
    });
  });

  router.get("/accountBalance/:entity", function(req, res) {
    // console.log(req);
    var arr = [];
    arr[0] = req.params.entity;
    

    var details = {};
    details.trade = arr;
    details.userName = req.query.userName;
    details.userOrg = req.query.userOrg;

    infoController.getAccountBalance(details, function(err, data) {
      if (err) {
        res.statusCode = 500;
        res.end(err.message);
      } else {
        res.end(JSON.stringify(data));
      }
    });
  });
  router.get("/getListOfTrades", function(req, res) {
    // console.log(req);

    var details = {};
    details.userName = req.query.userName;
    details.userOrg = req.query.userOrg;
    if (
      details.userOrg == "importerorg" ||
      details.userOrg == "exporterorg" ||
      details.userOrg == "issuingbankorg"
    ) {
      infoController.getListOfTrades(details, function(err, data) {
        if (err) {
          res.statusCode = 500;
          res.end(err.message);
        } else {
          res.end(JSON.stringify(data));
        }
      });
    } else {
      res.statusCode = 500;
      res.end("Access denied...Importer or Exporter can access the trade Data");
    }
  });

  router.get("/getListOfLC", function(req, res) {
    // console.log(req);

    var details = {};
    details.userName = req.query.userName;
    details.userOrg = req.query.userOrg;
    if (
      details.userOrg == "importerorg" ||
      details.userOrg == "issuingbankorg" ||
      details.userOrg == "advisingbankorg"
    ) {
      infoController.getListOfLC(details, function(err, data) {
        if (err) {
          res.statusCode = 500;
          res.end(err.message);
        } else {
          res.end(JSON.stringify(data));
        }
      });
    } else {
      res.statusCode = 500;
      res.end(
        "Access denied...Importer or IssuingBank or Advising bank can access the LC Data"
      );
    }
  });

  router.get("/getListOfBOL", function(req, res) {
    // console.log(req);

    var details = {};
    details.userName = req.query.userName;
    details.userOrg = req.query.userOrg;
    if (
      details.userOrg == "importerorg" ||
      details.userOrg == "carrierorg" ||
      details.userOrg == "exporterorg"
    ) {
      infoController.getListOfBOL(details, function(err, data) {
        if (err) {
          res.statusCode = 500;
          res.end(err.message);
        } else {
          res.end(JSON.stringify(data));
        }
      });
    } else {
      res.statusCode = 500;
      res.end(
        "Access denied...Only Carrier  can access the Bill of Lading Data"
      );
    }
  });

  return router;
};
