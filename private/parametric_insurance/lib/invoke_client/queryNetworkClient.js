"use strict";
/*
 * Copyright IBM Corp All Rights Reserved
 *
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * Chaincode query
 */

var Fabric_Client = require("fabric-client");
var path = require("path");
var util = require("util");
var os = require("os");
var cfg = require("./config/config.json");
//
var fabric_client = new Fabric_Client();

var user_id = cfg.user_id;
var channel_id = cfg.channel_id;
var chaincode_id = cfg.chaincode_id;
var peer_url = cfg.peer_url;
// setup the fabric network
var channel = fabric_client.newChannel(channel_id);
var peer = fabric_client.newPeer(peer_url);
channel.addPeer(peer);

//
var member_user = null;
var store_path = path.join(__dirname, "./amb_network/creds");
console.log("Store path:" + store_path);
var tx_id = null;
var data = new Object();

module.exports.queryClient = function(requestParams, callback) {
  console.log("Request params : ", requestParams);
  // create the key value store as defined in the fabric-client/config/default.json 'key-value-store' setting
  Fabric_Client.newDefaultKeyValueStore({ path: store_path })
    .then(state_store => {
      // assign the store to the fabric client
      fabric_client.setStateStore(state_store);
      var crypto_suite = Fabric_Client.newCryptoSuite();
      // use the same location for the state store (where the users' certificate are kept)
      // and the crypto store (where the users' keys are kept)
      var crypto_store = Fabric_Client.newCryptoKeyStore({ path: store_path });
      crypto_suite.setCryptoKeyStore(crypto_store);
      fabric_client.setCryptoSuite(crypto_suite);

      // get the enrolled user from persistence, this user will sign all requests
      return fabric_client.getUserContext(user_id, true);
    })
    .then(user_from_store => {
      if (user_from_store && user_from_store.isEnrolled()) {
        console.log("Successfully loaded user1 from persistence");
        member_user = user_from_store;
      } else {
        throw new Error("Failed to get user1.... run registerUser.js");
      }

      var args_data
      if (typeof requestParams.args !== "string") {
        args_data = JSON.stringify(requestParams.args);
      } else {
        console.log("String object found, no need to strigify");
        args_data = requestParams.args;
      }
      
      // queryCar chaincode function - requires 1 argument, ex: args: ['CAR4'],
      // queryAllCars chaincode function - requires no arguments , ex: args: [''],
      const request = {
        //targets : --- letting this default to the peers assigned to the channel
        chaincodeId: chaincode_id,
        fcn: requestParams.funcName,
        args: [args_data]
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
