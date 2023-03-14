/*jslint node: true, nomen: true*/
"use strict";

/**
 * Reference - https://github.com/node-cron/node-cron
 *           - https://stackoverflow.com/questions/9765215/global-variable-in-app-js-accessible-in-routes
 */

module.exports = function(express, logger, config) {
  var path = require("path"),
    router = express.Router(),
    schedulerController = require(path.join(
      "..",
      "controllers",
      "schedulerController"
    ))(logger, config);
  var cron = require("node-cron");

  /* start the scheduler job */
  router.get("/startJob", function(req, res) {
    // console.log(req);

    if (req.app.get("schedulerJob") == false) {
      var task = cron.schedule("*/2 * * * *", () => {
        console.log("running a task every minute");
        schedulerController.scheduleJob(function(err, data) {
          if (err) {
            console.log(err.message);
          } else {
            console.log(data);
          }
        });
      });
      req.app.set("schedulerJob", true);
      req.app.set("taskObj", task);
      task.start();
      console.log("Scheduler started!");
      res.status(200).json("Scheduler started!");
    } else {
      console.log("Scheduler is already running!");
      res.status(200).json("Scheduler is already running!");
    }
  });

  /* stop the scheduler job */
  router.get("/stopJob", function(req, res) {
    // console.log(req);

    if (req.app.get("schedulerJob") == true) {
      var task = req.app.get("taskObj");
      task.stop();
      req.app.set("schedulerJob", false);
      console.log("Scheduler stopped successfully!");
      res.status(200).json("Scheduler stopped successfully!");
    } else {
      console.log("Scheduler was not running!");
      res.status(200).json("Scheduler was not running!");
    }
  });

  return router;
};
