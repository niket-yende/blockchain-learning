/*jslint node: true, nomen: true*/
"use strict";

var express = require("express");
var cors = require("cors");
var path = require("path");
var morgan = require("morgan");
var winston = require("winston");
var cookieParser = require("cookie-parser");
var bodyParser = require("body-parser");
var fs = require("fs");

var routes = require("./routes/index");
var ClientUtils = require("./middleware/ClientUtils");

var tradeRouter = require("./routes/tradeRouter");
var infoRouter = require("./routes/infoRouter");
var shipmentRouter = require("./routes/shipmentRouter");
var loginRouter = require("./routes/loginRouter");

var host = process.env.HOST || "localhost";
var port = process.env.PORT || 3000;

var app = express();
app.options("*", cors());
app.use(cors());

var bodyParser = require("body-parser");
app.use(bodyParser.json()); // support json encoded bodies
app.use(bodyParser.urlencoded({ extended: true })); // support encoded bodies

// view engine setup (not included)

app.use(bodyParser.json());
app.use(
  bodyParser.urlencoded({
    extended: false
  })
);
app.use(cookieParser());

// get config

// pretend to return favicon
app.get("/favicon.ico", function(req, res) {
  res.send(200);
});


// Set the ENV variable to point to the right environment

switch (process.env.NODE_ENV) {
  case "development":
    app.set("env", "development");
    break;
  case "production":
    app.set("env", "production");
    break;
  case "test":
    app.set("env", "test");
    break;
  default:
    console.error(
      "NODE_ENV environment variable should have value 'development', 'test', or 'production' \nExiting"
    );
    process.exit();
}

//load the config variables depending on the environment

var config_file_name = app.get("env") + "_config.json";
var data = fs.readFileSync(path.join(__dirname, "config", config_file_name));
var myObj;
var configObject, property;
try {
  configObject = JSON.parse(data);
} catch (err) {
  console.log("There has been an error parsing the config file JSON.");
  console.log(err);
  process.exit();
}
app.config = {};
for (property in configObject) {
  if (configObject.hasOwnProperty(property)) {
    app.config[property] = configObject[property];
  }
}

var logLevel = process.env.LOGGING_LEVEL;
if (
  !(
    logLevel === "info" ||
    logLevel === "warn" ||
    logLevel === "error" ||
    logLevel === "debug"
  )
) {
  console.warn(
    "LOGGING_LEVEL environment variable not set to a valid logging level. Using default level info"
  );
  logLevel = "info";
}

try {
  fs.accessSync(app.config.LOGGING_DIRECTORY, fs.F_OK);
} catch (e) {
  console.error(
    "Could not access LOGGING_DIRECTORY that is set in config.\nExiting"
  );
  process.exit();
}

//logging using winston

var winstonTransports = [
  new winston.transports.File({
    name: "fileLog",
    level: logLevel,
    filename: path.join(
      app.config.LOGGING_DIRECTORY,
      app.config.LOG_FILE_NAME_PREFIX + ".log"
    ),
    handleExceptions: true,
    json: false,
    maxsize: 5242880, //5MB
    maxFiles: 5,
    colorize: false,
    timestamp: true
  })
];

if (logLevel === "debug") {
  winstonTransports.push(
    new winston.transports.Console({
      level: "debug",
      json: false,
      handleExceptions: true,
      colorize: true,
      timestamp: true
    })
  );
}

var logger = new winston.Logger({
  transports: winstonTransports,
  exitOnError: false
});

logger.level = logLevel;

logger.stream = {
  write: function(message, encoding) {
    logger.info(message);
  }
};

app.logger = logger;

app.use(
  require("morgan")("short", {
    stream: logger.stream
  })
);

app.use("/", routes);
// app.use("/api", mainRouter(express, logger, app.config));
app.use("/trade", tradeRouter(express, logger, app.config));
app.use("/info", infoRouter(express, logger, app.config));
app.use("/shipment", shipmentRouter(express, logger, app.config));
app.use("/login", loginRouter(express, logger, app.config));

//start
// Register and enroll user

// function getErrorMessage(field) {
//   var response = {
//     success: false,
//     message: field + " field is missing or Invalid in the request"
//   };
//   return response;
// }

// app.post("/login", function(req, res) {
//   var username = req.body.username;
//   var orgName = req.body.orgName;
//   var password = req.body.password;
//   logger.debug("User name for login/registration : " + username);
//   logger.debug("Org name  : " + orgName);
//   if (!username) {
//     res.json(getErrorMessage("'username'"));
//     return;
//   }
//   if (!orgName) {
//     res.json(getErrorMessage("'orgName'"));
//     return;
//   }
//   logger.debug("2");
//   // Hardcode single 'admin' user per org for now
//   if (username === "admin" && !password) {
//     res.json(getErrorMessage("'password'"));
//     return;
//   }
//   logger.debug("3");
//   var token = jwt.sign(
//     {
//       // Make the token expire 60 seconds from now
//       exp: Math.floor(Date.now() / 1000) + 69 * 60,
//       username: username,
//       orgName: orgName
//     },
//     app.get("secret")
//   );
//   logger.debug("4");
//   ClientUtils.init();
//   logger.debug("5");
//   ClientUtils.getClientUser(orgName, username, password)
//     .then(response => {
//       logger.debug(
//         "-- returned from registering (logging in) the username %s for organization %s",
//         username,
//         orgName
//       );

//       if (response && typeof response !== "string") {
//         var resp = {};
//         resp.token = token;
//         resp.success = true;
//         if (
//           response._enrollmentSecret &&
//           response._enrollmentSecret.length > 0
//         ) {
//           logger.debug(
//             "Successfully registered the username %s for organization %s",
//             username,
//             orgName
//           );
//           resp.secret = response._enrollmentSecret;
//           resp.message = "Registration successful";
//         } else {
//           logger.debug(
//             "Successfully enrolled the username %s for organization %s",
//             username,
//             orgName
//           );
//           resp.message = "Login successful";
//         }
//         res.json(resp);
//       } else {
//         logger.debug(
//           "Failed to register the username %s for organization %s with::%s",
//           username,
//           orgName,
//           response
//         );
//         var message = "Registration/login failed";
//         if (response) {
//           message = JSON.stringify(response);
//         }
//         res.json({ success: false, message: message });
//       }
//     })
//     .catch(err => {
//       console.error(err);
//       logger.debug(
//         "Failed to register username %s for organization %s with::%s",
//         username,
//         orgName,
//         err.message
//       );
//       res.json({ success: false, message: err.message });
//     });
//   logger.debug("Org name  : " + orgName);
// });
//end

app.use(function(req, res, next) {
  res.header("Access-Control-Allow-Origin", "*");
  res.header("Access-Control-Allow-Methods", "GET, PUT, POST, DELETE");
  res.header(
    "Access-Control-Allow-Headers",
    "Content-Type, authorization,X-Requested-With"
  );
  res.header("Access-Control-Allow-Credentials", true);
  next();
});

// catch 404 and forward to error handler
app.use(function(req, res, next) {
  var err = new Error("Not Found");
  err.status = 404;
  next(err);
});

// error handlers

// development error handler
// will print stacktrace
if (app.get("env") === "development") {
  app.use(function(err, req, res, next) {
    res.status(err.status || 500);
    res.json({
      message: err.message,
      error: err
    });
  });
}

// production error handler
// no stacktraces leaked to user
app.use(function(err, req, res, next) {
  res.status(err.status || 500);
  res.json({
    message: err.message,
    error: {}
  });
});

module.exports = app;
