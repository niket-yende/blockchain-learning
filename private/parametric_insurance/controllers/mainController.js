/*jslint node: true, nomen: true*/
"use strict";
var path = require("path");
var networkClient = require(path.join(
  "..",
  "lib",
  "invoke_client",
  "invokeNetworkClient.js"
));
var client = require(path.join(
  "..",
  "lib",
  "invoke_client",
  "queryNetworkClient.js"
));

module.exports = function(logger, config) {
  var funcs = {};

  funcs.getContractsByCompanyName = function(insuranceCompany, cb) {
    //In this case args passed is a query string
    // var requestParams =
    var selector = {
      selector: { docType: "contract", insuranceCompanyName: insuranceCompany },
      use_index:["_design/indexInsuranceCompanyContractDoc","indexInsuranceCompanyContract"]
    };
    var requestParams = {};
    requestParams["funcName"] = "queryState";
    requestParams["args"] = selector;
    console.log("requestParams : ", requestParams);

    client.queryClient(requestParams, function(err, contractList) {
      if (err) {
        cb(err, null);
      } else {
        logger.info("Returning contract list ", contractList);
        cb(null, contractList);
      }
    });
  };

  funcs.getContractsByCustomer = function(customerName, cb) {
    //In this case args passed is a query string
    // var requestParams =
    var selector = {
      selector: { docType: "contract", customerName: customerName },
      use_index:["_design/indexCustomerContractDoc","indexCustomerContract"]
    };
    var requestParams = {};
    requestParams["funcName"] = "queryState";
    requestParams["args"] = selector;
    console.log("requestParams : ", requestParams);

    client.queryClient(requestParams, function(err, contractList) {
      if (err) {
        cb(err, null);
      } else {
        logger.info("Returning contract list ", contractList);
        cb(null, contractList);
      }
    });
  };

  funcs.getWeatherDetailsByLocation = function(weatherRequestObj, cb) {
    //In this case args passed is a query string
    // var requestParams =
    var selector = {
      selector: { docType: "weatherObject", location: weatherRequestObj.location, dateTime:{"$gt":weatherRequestObj.startDate,"$lt":weatherRequestObj.endDate} },
      use_index:["_design/indexWeatherDoc","indexWeather"]
    };
    var requestParams = {};
    requestParams["funcName"] = "queryState";
    requestParams["args"] = selector;
    console.log("requestParams : ", requestParams);

    client.queryClient(requestParams, function(err, weatherList) {
      if (err) {
        cb(err, null);
      } else {
        logger.info("Returning weather list ", weatherList);
        cb(null, weatherList);
      }
    });
  };

  funcs.getHistoryDetails = function(contractID, cb) {
    //In this case args passed is a query string
    // var requestParams =
    
    var requestParams = {};
    requestParams["funcName"] = "getHistoryForContract";
    requestParams["args"] = contractID;
    console.log("requestParams : ", requestParams);

    client.queryClient(requestParams, function(err, historyList) {
      if (err) {
        cb(err, null);
      } else {
        logger.info("Returning historical records ", historyList);
        cb(null, historyList);
      }
    });
  };

  funcs.createCustomer = function(cusotmer, cb) {
    //Passing a customer object from UI to Backend
    var requestParams = {};
    console.log("customer1 ", cusotmer);
    requestParams["funcName"] = "createCustomer";
    requestParams["args"] = cusotmer;
    console.log("requestParams : ", requestParams);

    networkClient.invokeClient(requestParams, function(err, data) {
      if (err) {
        cb(err, null);
      } else {
        logger.info("New customer added: ", data);
        cb(null, data);
      }
    });
  };

  funcs.createInsuranceCompany = function(insuranceCompany, cb) {
    //Passing a customer object from UI to Backend
    var requestParams = {};
    console.log("InsuranceComapany1 ", insuranceCompany);
    requestParams["funcName"] = "createInsuranceCompany";
    requestParams["args"] = insuranceCompany;
    console.log("requestParams : ", requestParams);

    networkClient.invokeClient(requestParams, function(err, data) {
      if (err) {
        cb(err, null);
      } else {
        logger.info("New company added: ", data);
        cb(null, data);
      }
    });
  };

  funcs.registerInsuranceContract = function(insuranceContract, cb) {
    //Passing a customer object from UI to Backend
    var requestParams = {};
    console.log("insuranceContract ", insuranceContract);
    requestParams["funcName"] = "registerInsuranceContract";
    requestParams["args"] = insuranceContract;
    console.log("requestParams : ", requestParams);

    // cb(null, requestParams);
    networkClient.invokeClient(requestParams, function(err, data) {
      if (err) {
        cb(err, null);
        console.log("error");
      } else {
        logger.info("registered new contract : ", data);
        cb(null, data);
        console.log("success");
      }
    });
  };

  funcs.generateWeatherReport = function(weatherObject, cb) {
    //Pushing weather object from external api to Backend
    var requestParams = {};
    console.log("weatherObject ", weatherObject);
    requestParams["funcName"] = "generateWeatherReport";
    requestParams["args"] = weatherObject;
    console.log("requestParams : ", requestParams);

    networkClient.invokeClient(requestParams, function(err, data) {
      if (err) {
        cb(err, null);
      } else {
        logger.info("Pushed weather report : ", data);
        cb(null, data);
      }
    });
  };

  return funcs;
};
