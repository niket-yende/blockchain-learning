/*jslint node: true, nomen: true*/
"use strict";

/**
 * Reference - https://github.com/node-cron/node-cron
 *           - https://stackoverflow.com/questions/9765215/global-variable-in-app-js-accessible-in-routes
 */

module.exports = function(express, logger, config) {
  var path = require("path"),
    router = express.Router(),
    tradeController = require(path.join(
      "..",
      "controllers",
      "tradeController"
    ))(logger, config);

  /* stop the scheduler job */
  // router.post("/trades/requestTrade", function(req, res) {
  //   // console.log(req);
  //   var tradeId = req.params.tradeId;
  //   console.log(tradeId);
  //   trade = tradeId ? { tradeId: tradeId } : {};
  //   tradeController.fetchTrades(trade, function(err, data) {
  //     if (err) {
  //       res.statusCode = 500;
  //       res.end(err.message);
  //     } else {
  //       res.end(JSON.stringify(data));
  //     }
  //   });
  // });
  router.post("/requestTrade", function(req, res) {
    var trade = [];
    trade[0] = req.body.id;
    trade[1] = req.body.amount;
    trade[2] = req.body.descr;
    var details = {};
    details.trade = trade;
    details.userName = req.body.userName;
    details.userOrg = req.body.userOrg;

    tradeController.requestTrade(details, function(err, data) {
      if (err) {
        res.status(err.status).json({
          message: err.message
        });
      } else {
        res.status(200).json(data);
      }
    });
  });
  router.post("/acceptTrade", function(req, res) {
    var trade = [];
    trade[0] = req.body.id;

    var details = {};
    details.trade = trade;
    details.userName = req.body.userName;
    details.userOrg = req.body.userOrg;

    tradeController.acceptTrade(details, function(err, data) {
      if (err) {
        res.status(err.status).json({
          message: err.message
        });
      } else {
        res.status(200).json(data);
      }
    });
  });
  router.post("/requestLC", function(req, res) {
    var trade = [];
    trade[0] = req.body.id;

    var details = {};
    details.trade = trade;
    details.userName = req.body.userName;
    details.userOrg = req.body.userOrg;
    console.log(details);
    tradeController.requestLC(details, function(err, data) {
      if (err) {
        res.status(err.status).json({
          message: err.message
        });
      } else {
        res.status(200).json(data);
      }
    });
  });

  router.post("/issueLC", function(req, res) {
    var trade = [];
    trade[0] = req.body.id;
    trade[1] = req.body.lcid;
    trade[2] = req.body.expiryDate;

    var details = {};
    details.trade = trade;
    details.userName = req.body.userName;
    details.userOrg = req.body.userOrg;

    tradeController.issueLC(details, function(err, data) {
      if (err) {
        res.status(err.status).json({
          message: err.message
        });
      } else {
        res.status(200).json(data);
      }
    });
  });

  router.post("/acceptLC", function(req, res) {
    var trade = [];
    trade[0] = req.body.id;

    var details = {};
    details.trade = trade;
    details.userName = req.body.userName;
    details.userOrg = req.body.userOrg;

    tradeController.acceptLC(details, function(err, data) {
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
