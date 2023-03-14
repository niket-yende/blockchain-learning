/*jslint node: true, nomen: true*/
"use strict";

module.exports = function(express, logger, config) {
  var path = require("path"),
    router = express.Router(),
    mainController = require(path.join("..", "controllers", "mainController"))(
      logger,
      config
    );

  /* GET all the contracts for a company */
  router.get("/contracts/company/:insuranceCompany", function(req, res) {
    // console.log(req);
    var insuranceCompany = req.params.insuranceCompany;
    console.log("insuranceCompany ", insuranceCompany);

    mainController.getContractsByCompanyName(insuranceCompany, function(err, data) {
      if (err) {
        res.status(err.status).json({
          message: err.message
        });
      } else {
        res.status(200).json(data);
      }
    });
  });

  /* GET all the contracts for a customer */
  router.get("/contracts/customer/:customerName", function(req, res) {
    // console.log(req);
    var customerName = req.params.customerName;
    console.log("customerName ", customerName);

    mainController.getContractsByCustomer(customerName, function(err, data) {
      if (err) {
        res.status(err.status).json({
          message: err.message
        });
      } else {
        res.status(200).json(data);
      }
    });
  });

  /* GET historical details of a contract */
  router.get("/contracts/history/:contractID", function(req, res) {
    // console.log(req);
    var contractID = req.params.contractID;
    console.log("contractID ", contractID);

    mainController.getHistoryDetails(contractID, function(err, data) {
      if (err) {
        res.status(err.status).json({
          message: err.message
        });
      } else {
        res.status(200).json(data);
      }
    });
  });

  /* GET weather related data for a location */
  router.get("/weather", function(req, res) {
    // console.log(req);
    var location = req.query.location;
    var startDate = req.query.startDate;
    var endDate = req.query.endDate;

    var weatherRequestObj = {};
    weatherRequestObj["location"] = location;
    weatherRequestObj["startDate"] = startDate;
    weatherRequestObj["endDate"] = endDate;

    mainController.getWeatherDetailsByLocation(weatherRequestObj, function(err, data) {
      if (err) {
        res.status(err.status).json({
          message: err.message
        });
      } else {
        res.status(200).json(data);
      }
    });
  });

  /* Create a new customer */

  router.post("/customer", function(req, res) {
    var customerName = req.body.name;
    var customerEmail = req.body.email;
    var customerAddress = req.body.address;
    var customerContact = req.body.contact;

    var customer = {};
    customer["customerName"] = customerName;
    customer["customerEmail"] = customerEmail;
    customer["customerAddress"] = customerAddress;
    customer["customerContact"] = customerContact;
    console.log("customer : ", customer);

    mainController.createCustomer(customer, function(err, data) {
      if (err) {
        res.status(err.status).json({
          message: err.message
        });
      } else {
        res.status(200).json(data);
      }
    });
  });

  /* Create a new insurance company */
  router.post("/insuranceCompany", function(req, res) {
    var companyName = req.body.name;
    var companyEmail = req.body.email;
    var companyContact = req.body.contact;
    var companyAddress = req.body.address;

    var insuranceCompany = {};
    insuranceCompany["companyName"] = companyName;
    insuranceCompany["companyEmail"] = companyEmail;
    insuranceCompany["companyContact"] = companyContact;
    insuranceCompany["companyAddress"] = companyAddress;

    console.log("insuranceCompany object : ", insuranceCompany);

    mainController.createInsuranceCompany(insuranceCompany, function(
      err,
      data
    ) {
      if (err) {
        res.status(err.status).json({
          message: err.message
        });
      } else {
        res.status(200).json(data);
      }
    });
  });

  /* Register a new insurance contract */
  router.post("/insuranceContract", function(req, res) {
    var customerName = req.body.customerName;
    var insuranceCompanyName = req.body.insuranceCompanyName;
    var insuranceAmount = req.body.amount;
    var insuranceCriterias = req.body.insuranceCriterias;
    var startDate = req.body.startDate;
    var endDate = req.body.endDate;
    var location = req.body.location;
    var currentStatus = "INACTIVE";

    var insuranceContract = {};
    insuranceContract["customerName"] = customerName;
    insuranceContract["companyName"] = insuranceCompanyName;
    insuranceContract["insuredAmount"] = insuranceAmount;
    insuranceContract["insuranceCriterias"] = insuranceCriterias;
    insuranceContract["startDate"] = startDate;
    insuranceContract["endDate"] = endDate;
    insuranceContract["location"] = location;
    insuranceContract["status"] = currentStatus;

    console.log("insuranceContract object : ", insuranceContract);

    mainController.registerInsuranceContract(insuranceContract, function(
      err,
      data
    ) {
      if (err) {
        res.status(err.status).json({
          message: err.message
        });
      } else {
        res.status(200).json(data);
      }
    });
  });

  /* Push weather report object for a stipulated time */
  /**
   * weatherObject = {"location":"Bangalore","weatherCriterias":{"rain":"10","snow":"false","temp":"32.00"}}
   */
  router.post("/weatherObject", function(req, res) {
    var location = req.body.location;
    var weatherCriterias = req.body.weatherCriterias;
    var dateTime = req.body.dateTime;

    var weatherObject = {};
    weatherObject["location"] = location;
    weatherObject["weatherCriterias"] = weatherCriterias;
    weatherObject["dateTime"] = dateTime;

    console.log("weatherObject object : ", weatherObject);

    mainController.generateWeatherReport(weatherObject, function(err, data) {
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
