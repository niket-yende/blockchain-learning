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
  peer = fabric_client.newPeer(peer_url);
  var peer_name=cfg.net_loc[userOrg].peer1.name;
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
module.exports.invokeClient = function(requestParams, callback) {
  console.log("Request params : ", requestParams);
  Fabric_Client.newDefaultKeyValueStore({ path: requestParams.storepath })
    .then(state_store => {
      // assign the store to the fabric client
      fabric_client.setStateStore(state_store);
      var crypto_suite = Fabric_Client.newCryptoSuite();
      // use the same location for the state store (where the users' certificate are kept)
      // and the crypto store (where the users' keys are kept)
      console.log(requestParams.storepath);
      var crypto_store = Fabric_Client.newCryptoKeyStore({
        path: requestParams.storepath
      });
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

      // get a transaction id object based on the current user assigned to fabric client
      tx_id = fabric_client.newTransactionID();
      console.log("Assigning transaction_id: ", tx_id._transaction_id);

      var args_data;
      if (typeof requestParams.args !== "string") {
        args_data = JSON.stringify(requestParams.args);
      } else {
        console.log("String object found, no need to strigify");
        args_data = requestParams.args;
      }
      // createCar chaincode function - requires 5 args, ex: args: ['CAR12', 'Honda', 'Accord', 'Black', 'Tom'],
      // changeCarOwner chaincode function - requires 2 args , ex: args: ['CAR10', 'Dave'],
      // must send the proposal to endorsing peers
      var request = {
        //targets: let default to the peer assigned to the client
        chaincodeId: chaincode_id,
        fcn: requestParams.funcName,
        args: requestParams.args,
        chainId: channel_id,
        txId: tx_id
      };

      console.log("request object ", request);

      // send the transaction proposal to the peers
      return channel.sendTransactionProposal(request);
    })
    .then(results => {
      var proposalResponses = results[0];
      var proposal = results[1];
      let isProposalGood = false;
      if (
        proposalResponses &&
        proposalResponses[0].response &&
        proposalResponses[0].response.status === 200
      ) {
        isProposalGood = true;
        console.log("Transaction proposal was good");
      } else {
        console.error("Transaction proposal was bad"+proposalResponses[0]);
      }
      if (isProposalGood) {
        console.log(
          util.format(
            'Successfully sent Proposal and received ProposalResponse: Status - %s, message - "%s"',
            proposalResponses[0].response.status,
            proposalResponses[0].response.message
          )
        );

        // build up the request for the orderer to have the transaction committed
        var request = {
          proposalResponses: proposalResponses,
          proposal: proposal
        };

        // set the transaction listener and set a timeout of 30 sec
        // if the transaction did not get committed within the timeout period,
        // report a TIMEOUT status
        var transaction_id_string = tx_id.getTransactionID(); //Get the transaction ID string to be used by the event processing
        var promises = [];

        var sendPromise = channel.sendTransaction(request);
        promises.push(sendPromise); //we want the send transaction first, so that we know where to check status

        // get an eventhub once the fabric client has a user assigned. The user
        // is required bacause the event registration must be signed
        let event_hub = channel.newChannelEventHub(peer);

        // using resolve the promise so that result status may be processed
        // under the then clause rather than having the catch clause process
        // the status
        let txPromise = new Promise((resolve, reject) => {
          let handle = setTimeout(() => {
            event_hub.unregisterTxEvent(transaction_id_string);
            event_hub.disconnect();
            resolve({ event_status: "TIMEOUT" }); //we could use reject(new Error('Trnasaction did not complete within 30 seconds'));
          }, 3000);
          event_hub.registerTxEvent(
            transaction_id_string,
            (tx, code) => {
              // this is the callback for transaction event status
              // first some clean up of event listener
              clearTimeout(handle);

              // now let the application know what happened
              var return_status = {
                event_status: code,
                tx_id: transaction_id_string
              };
              if (code !== "VALID") {
                console.error("The transaction was invalid, code = " + code);
                resolve(return_status); // we could use reject(new Error('Problem with the tranaction, event status ::'+code));
              } else {
                console.log(
                  "The transaction has been committed on peer " +
                    event_hub.getPeerAddr()
                );
                resolve(return_status);
              }
            },
            err => {
              //this is the callback if something goes wrong with the event registration or processing
              reject(
                new Error("There was a problem with the eventhub ::" + err)
              );
            },
            { disconnect: true } //disconnect when complete
          );
          event_hub.connect();
        });
        promises.push(txPromise);

        let payloadPromise = new Promise((resolve, reject) => {
          resolve({
            payload: proposalResponses[0].response.payload.toString()
          });
        });
        promises.push(payloadPromise);

        return Promise.all(promises);
      } else {
        console.error(
          "Failed to send Proposal or receive valid response. Response null or status is not 200. exiting..."
        );
        throw new Error(
          proposalResponses[0]
        );
      }
    })
    .then(results => {
      console.log(
        "Send transaction promise and event listener promise have completed"
      );
      // check the results in the order the promises were added to the promise all list
      if (results && results[0] && results[0].status === "SUCCESS") {
        console.log("Successfully sent transaction to the orderer.");
        data.status = results[0].status;
        data.message =
          "Successfully committed the change to the ledger by the peer";
        data.payload = results[2].payload;
        callback(null, data);
      } else {
        console.error(
          "Failed to order the transaction. Error code: " + results[0].status
        );
        throw new Error(
          "Failed to order the transaction. Error code: " + results[0].status
        );
      }

      // if (results && results[1] && results[1].event_status === "VALID") {
      //   console.log(
      //     "Successfully committed the change to the ledger by the peer"
      //   );
      // } else {
      //   console.log(
      //     "Transaction failed to be committed to the ledger due to ::" +
      //       results[1].event_status
      //   );
      // }
    })
    .catch(err => {
      console.error("Failed to invoke successfully :: " + err);
      callback(err, null);
    });
};
