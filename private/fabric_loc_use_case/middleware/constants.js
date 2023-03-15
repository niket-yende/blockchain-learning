/*
 * Copyright 2018 IBM All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var os = require("os");
var path = require("path");

var tempdir = "../fabric_loc_use_case/middleware/network/client-certs";
//path.join(os.tmpdir(), 'hfc');

// Frame the endorsement policy
var FOUR_ORG_MEMBERS_AND_ADMIN = [
  {
    role: {
      name: "member",
      mspId: "ExporterOrgMSP"
    }
  },
  {
    role: {
      name: "member",
      mspId: "ImporterOrgMSP"
    }
  },
  {
    role: {
      name: "member",
      mspId: "CarrierOrgMSP"
    }
  },
  {
    role: {
      name: "member",
      mspId: "RegulatorOrgMSP"
    }
  },
  {
    role: {
      name: "admin",
      mspId: "TradeOrdererMSP"
    }
  }
];

var FIVE_ORG_MEMBERS_AND_ADMIN = [
  {
    role: {
      name: "member",
      mspId: "ExporterOrgMSP"
    }
  },
  {
    role: {
      name: "member",
      mspId: "IssuingBankOrgMSP"
    }
  },
  {
    role: {
      name: "member",
      mspId: "ImporterOrgMSP"
    }
  },
  {
    role: {
      name: "member",
      mspId: "CarrierOrgMSP"
    }
  },
  {
    role: {
      name: "member",
      mspId: "AdvisingBankOrgMSP"
    }
  },
  {
    role: {
      name: "admin",
      mspId: "TradeOrdererMSP"
    }
  }
];

var ONE_OF_FOUR_ORG_MEMBER = {
  identities: FOUR_ORG_MEMBERS_AND_ADMIN,
  policy: {
    "1-of": [
      { "signed-by": 0 },
      { "signed-by": 1 },
      { "signed-by": 2 },
      { "signed-by": 3 }
    ]
  }
};

var ALL_FOUR_ORG_MEMBERS = {
  identities: FOUR_ORG_MEMBERS_AND_ADMIN,
  policy: {
    "4-of": [
      { "signed-by": 0 },
      { "signed-by": 1 },
      { "signed-by": 2 },
      { "signed-by": 3 }
    ]
  }
};

var ALL_FIVE_ORG_MEMBERS = {
  identities: FIVE_ORG_MEMBERS_AND_ADMIN,
  policy: {
    "5-of": [
      { "signed-by": 0 },
      { "signed-by": 1 },
      { "signed-by": 2 },
      { "signed-by": 3 },
      { "signed-by": 4 }
    ]
  }
};

var ALL_ORGS_EXCEPT_REGULATOR = {
  identities: FOUR_ORG_MEMBERS_AND_ADMIN,
  policy: {
    "3-of": [{ "signed-by": 0 }, { "signed-by": 1 }, { "signed-by": 2 }]
  }
};

var ACCEPT_ALL = {
  identities: [],
  policy: {
    "0-of": []
  }
};

var chaincodeLocation = "../chaincode";

var networkId = "net_loc";

var networkConfig = "./config/config.json";

var networkLocation = "../network";

var channelConfig = "channel-artifacts/channel.tx";

var IMPORTER_ORG = "importerorg";
var EXPORTER_ORG = "exporterorg";
var ISSUING_BANK_ORG = "issuingbankorg";
var CARRIER_ORG = "carrierorg";
var ADVISING_BANK_ORG = "advisingbankorg";

var CHANNEL_NAME = "locchannel";
var CHAINCODE_PATH = "/trade_workflow";
var CHAINCODE_ID = "tradeworkflow";
var CHAINCODE_VERSION = "v0";
var CHAINCODE_UPGRADE_PATH = "/trade_workflow_v1";
var CHAINCODE_UPGRADE_VERSION = "v1";

var TRANSACTION_ENDORSEMENT_POLICY = ACCEPT_ALL;

module.exports = {
  tempdir: tempdir,
  chaincodeLocation: chaincodeLocation,
  networkId: networkId,
  networkConfig: networkConfig,
  networkLocation: networkLocation,
  channelConfig: channelConfig,
  IMPORTER_ORG: IMPORTER_ORG,
  EXPORTER_ORG: EXPORTER_ORG,
  ISSUING_BANK_ORG: ISSUING_BANK_ORG,
  CARRIER_ORG: CARRIER_ORG,
  ADVISING_BANK_ORG: ADVISING_BANK_ORG,
  CHANNEL_NAME: CHANNEL_NAME,
  CHAINCODE_PATH: CHAINCODE_PATH,
  CHAINCODE_ID: CHAINCODE_ID,
  CHAINCODE_VERSION: CHAINCODE_VERSION,
  CHAINCODE_UPGRADE_PATH: CHAINCODE_UPGRADE_PATH,
  CHAINCODE_UPGRADE_VERSION: CHAINCODE_UPGRADE_VERSION,
  ALL_FOUR_ORG_MEMBERS: ALL_FOUR_ORG_MEMBERS,
  ALL_FIVE_ORG_MEMBERS: ALL_FIVE_ORG_MEMBERS,
  TRANSACTION_ENDORSEMENT_POLICY: TRANSACTION_ENDORSEMENT_POLICY
};
