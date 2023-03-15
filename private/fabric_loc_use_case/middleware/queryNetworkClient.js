"use strict";
/*
 * Copyright IBM Corp All Rights Reserved
 *
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * Chaincode Invoke
 */
var utils = require("fabric-client/lib/utils.js");
var logger = utils.getLogger("ClientUtils");
var Fabric_Client = require("fabric-client");
var path = require("path");
var util = require("util");
var os = require("os");
var fs = require("fs");
var cfg = require("./config/config.json");

//

var fabric_client = new Fabric_Client();

var wallet_path;
// var user_id = cfg.loc - details.user_id;
var channel_id = cfg["loc-details"].channel_id;
var chaincode_id = cfg["loc-details"].chaincode_id;
// var config_user = cfg.loc - network[user];
// console.log("config_user ", config_user);
// var peer_url = config_user.peer1.requests;
var peer;
//var event_url = 'grpcs://fft‑zbc02c.4.secure.blockchain.ibm.com:20138';
var orderer_url = cfg["net_loc"].orderer.url;
// var orderer_cert = fs.readFileSync(path.join(__dirname, './amb_network/tls') + '/tlsca.example.com-cert.pem');
// var peer_cert = fs.readFileSync(path.join(__dirname, './amb_network/tls') + '/tlsca.org1.example.com-cert.pem');

// setup the fabric network
var channel = fabric_client.newChannel(channel_id);

var order = fabric_client.newOrderer(orderer_url);
channel.addOrderer(order);

//
var member_user = null;
//var store_path = path.join(__dirname, 'hfc-key-store');
var store_path =
path.join(__dirname, "./network/client-certs/hfc_");
console.log("Store path:" + store_path);
var tx_id = null;
var data = new Object();

// create the key value store as defined in the fabric-client/config/default.json 'key-value-store' setting
module.exports.addingPeer = function(userOrg, cb) {
  // var userOrg = userdetails.userOrg;
  console.log(1);
  wallet_path = path.join(__dirname, "./network/client-certs/hfc_");
  wallet_path = wallet_path + cfg.net_loc[userOrg].name;
  // store_path = wallet_path;

  var peer_url = cfg.net_loc[userOrg].peer1.requests;
  var peer_name=cfg.net_loc[userOrg].peer1.name;
  peer = fabric_client.newPeer(peer_url);

  try {
    // the synchronous code that we want to catch thrown errors on
    channel.getPeer(peer_name);
  } catch (err) {
    // handle the error safely
    console.log("error caught", err);
    channel.addPeer(peer);
  }

  cb(null, wallet_path);

  // logger.info(
  //   util.format(
  //     channel.getChannelPeer(cfg.net_loc[userOrg].peer1["server-hostname"])
  //   )
  // );

  //   channel.getChannelPeer("localhost:8051", function(message, data) {
  //     if (err) {
  //       console.log(message);

  //       peer = fabric_client.newPeer(peer_url);
  //       channel.addPeer(peer);
  //     } else {
  //       console.log(data);
  //       r = false;
  //     }
  //   });
  //   if (r) {
  //     cb(null, wallet_path);
  //   } else {
  //     cb(null, wallet_path);
  //   }
};
module.exports.queryClient = function(requestParams, callback) {
  console.log("Request params : ", requestParams);
  // create the key value store as defined in the fabric-client/config/default.json 'key-value-store' setting
  Fabric_Client.newDefaultKeyValueStore({ path: requestParams.storepath })
    .then(state_store => {
      // assign the store to the fabric client
      fabric_client.setStateStore(state_store);
      var crypto_suite = Fabric_Client.newCryptoSuite();
      // use the same location for the state store (where the users' certificate are kept)
      // and the crypto store (where the users' keys are kept)
      var crypto_store = Fabric_Client.newCryptoKeyStore({ path: requestParams.storepath });
      crypto_suite.setCryptoKeyStore(crypto_store);
      fabric_client.setCryptoSuite(crypto_suite);

      // get the enrolled user from persistence, this user will sign all requests
      return fabric_client.getUserContext(requestParams.userName, true);
    })
    .then(user_from_store => {
      if (user_from_store && user_from_store.isEnrolled()) {
        console.log("Successfully loaded user1 from persistence");
        member_user = user_from_store;
      } else {
        throw new Error("Failed to get user1.... run registerUser.js");
      }

      // var args_data;
      // if (typeof requestParams.args !== "string") {
      //   args_data = JSON.stringify(requestParams.args);
      // } else {
      //   console.log("String object found, no need to strigify");
      //   args_data = requestParams.args;
      // }

      // queryCar chaincode function - requires 1 argument, ex: args: ['CAR4'],
      // queryAllCars chaincode function - requires no arguments , ex: args: [''],
      const request = {
        //targets : --- letting this default to the peers assigned to the channel
        chaincodeId: chaincode_id,
        fcn: requestParams.funcName,
        args:requestParams.args
      };

      // send the query proposal to the peer
      return channel.queryByChaincode(request);
    })
    .then(query_responses => {
      console.log("Query has completed, checking results");
      // query_responses could have more than one  results if there multiple peers were used as targets
      if (query_responses && query_responses.length == 1) {
        if (query_responses[0] instanceof Error) {
          console.error("error from query = ", query_responses[0]);
        } else {
          console.log("Response is ", query_responses[0].toString());

          data.status = "SUCCESS";
          data.message = "Succesfully queried results";
        }
      } else {
        console.log("No payloads were returned from query");
        data.status = "SUCCESS";
        data.message = "No payloads were returned from query";
      }
      data.data = query_responses[0].toString();
      console.log("data ", data);
      callback(null, data);
    })
    .catch(err => {
      console.error("Failed to query successfully :: " + err);
      callback(err, null);
    });
};

// test purpose
// "use strict";
// /*
//  * Copyright IBM Corp All Rights Reserved
//  *
//  * SPDX-License-Identifier: Apache-2.0
//  */
// /*
//  * Chaincode query
//  */

// "use strict";

// var path = require("path");
// var fs = require("fs");

// var Constants = require("./constants.js");
// var Client = require("fabric-client");
// var ClientUtils = require("./clientUtils.js");

// //
// // Send chaincode query request to the peer
// //
// function queryClient(userOrg, version, funcName, argList, userName, constants) {
//   if (constants) {
//     Constants = constants;
//   }
//   ClientUtils.init(Constants);

//   var ORGS = JSON.parse(
//     fs.readFileSync(path.join(__dirname, Constants.networkConfig))
//   )[Constants.networkId];

//   Client.setConfigSetting("request-timeout", 60000);
//   var channel_name = Client.getConfigSetting(
//     "E2E_CONFIGTX_CHANNEL_NAME",
//     Constants.CHANNEL_NAME
//   );

//   // this is a transaction, will just use org's identity to
//   // submit the request. intentionally we are using a different org
//   // than the one that submitted the "move" transaction, although either org
//   // should work properly
//   var client = new Client();
//   var channel = client.newChannel(channel_name);

//   var orgName = ORGS[userOrg].name;
//   var cryptoSuite = Client.newCryptoSuite();
//   cryptoSuite.setCryptoKeyStore(
//     Client.newCryptoKeyStore({ path: ClientUtils.storePathForOrg(orgName) })
//   );
//   client.setCryptoSuite(cryptoSuite);

//   var targets = [];
//   // set up the channel to use each org's 'peer1' for
//   // both requests and events
//   for (let key in ORGS) {
//     if (ORGS.hasOwnProperty(key) && typeof ORGS[key].peer1 !== "undefined") {
//       let data = fs.readFileSync(
//         path.join(__dirname, ORGS[key].peer1["tls_cacerts"])
//       );
//       let peer = client.newPeer(ORGS[key].peer1.requests, {
//         pem: Buffer.from(data).toString(),
//         "ssl-target-name-override": ORGS[key].peer1["server-hostname"]
//       });
//       channel.addPeer(peer);
//     }
//   }

//   return Client.newDefaultKeyValueStore({
//     path: ClientUtils.storePathForOrg(orgName)
//   })
//     .then(store => {
//       client.setStateStore(store);
//       return ClientUtils.getSubmitter(client, false, userOrg, userName);
//     })
//     .then(
//       user => {
//         if (userName) {
//           console.log("Successfully enrolled user", userName);
//         } else {
//           console.log("Successfully enrolled user 'admin'");
//         }

//         // send query
//         var request = {
//           chaincodeId: Constants.CHAINCODE_ID,
//           fcn: funcName,
//           args: argList
//         };

//         return channel.queryByChaincode(request);
//       },
//       err => {
//         var errMesg = "Failed to get submitter ";
//         if (userName) {
//           errMesg = errMesg + userName + ". Error: " + err;
//         } else {
//           errMesg = errMesg + "admin. Error: " + err;
//         }
//         console.log(errMesg);
//         throw new Error(errMesg);
//       }
//     )
//     .then(
//       response_payloads => {
//         if (response_payloads) {
//           var value = "";
//           for (let i = 0; i < response_payloads.length; i++) {
//             if (value === "") {
//               value = response_payloads[i].toString("utf8");
//             } else if (value !== response_payloads[i].toString("utf8")) {
//               throw new Error("Responses from peers don't match");
//             }
//           }
//           return value;
//         } else {
//           console.log("response_payloads is null");
//           throw new Error("Failed to get response on query");
//         }
//       },
//       err => {
//         console.log(
//           "Failed to send query due to error: " + err.stack ? err.stack : err
//         );
//         throw new Error("Failed, got error on query");
//       }
//     );
// }

// module.exports.queryClient = queryClient;
