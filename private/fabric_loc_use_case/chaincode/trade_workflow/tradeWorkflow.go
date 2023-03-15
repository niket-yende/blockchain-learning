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

package main

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"strconv"
	"strings"

	"github.com/hyperledger/fabric/core/chaincode/shim"
	pb "github.com/hyperledger/fabric/protos/peer"
)

// TradeWorkflowChaincode implementation
type TradeWorkflowChaincode struct {
	testMode bool
}

func (t *TradeWorkflowChaincode) Init(stub shim.ChaincodeStubInterface) pb.Response {
	fmt.Println("Initializing Trade Workflow")
	_, args := stub.GetFunctionAndParameters()
	var err error

	if len(args) != 7 {
		err = errors.New(fmt.Sprintf("Incorrect number of arguments. Expecting 8: {"+
			"Exporter, "+
			"Exporter's Bank, "+
			"Exporter's Account Balance, "+
			"Importer, "+
			"Importer's Bank, "+
			"Importer's Account Balance, "+
			"Carrier, "+
			// "Regulatory Authority"+
			"}. Found %d", len(args)))
		return shim.Error(err.Error())
	}

	// Type checks
	_, err = strconv.Atoi(string(args[2]))
	if err != nil {
		fmt.Printf("Exporter's account balance must be an integer. Found %s\n", args[2])
		return shim.Error(err.Error())
	}
	_, err = strconv.Atoi(string(args[5]))
	if err != nil {
		fmt.Printf("Importer's account balance must be an integer. Found %s\n", args[5])
		return shim.Error(err.Error())
	}

	fmt.Printf("Exporter: %s\n", args[0])
	fmt.Printf("Exporter's Bank: %s\n", args[1])
	fmt.Printf("Exporter's Account Balance: %s\n", args[2])
	fmt.Printf("Importer: %s\n", args[3])
	fmt.Printf("Importer's Bank: %s\n", args[4])
	fmt.Printf("Importer's Account Balance: %s\n", args[5])
	fmt.Printf("Carrier: %s\n", args[6])
	// fmt.Printf("Regulatory Authority: %s\n", args[7])

	// Map participant identities to their roles on the ledger
	roleKeys := []string{expKey, ebKey, expBalKey, impKey, ibKey, impBalKey, carKey}
	for i, roleKey := range roleKeys {
		err = stub.PutState(roleKey, []byte(args[i]))
		if err != nil {
			fmt.Errorf("Error recording key %s: %s\n", roleKey, err.Error())
			return shim.Error(err.Error())
		}
	}

	return shim.Success(nil)
}

func (t *TradeWorkflowChaincode) Invoke(stub shim.ChaincodeStubInterface) pb.Response {
	fmt.Println("TradeWorkflow Invoke")

	creator, err := stub.GetCreator()
	if err != nil {
		fmt.Errorf("Error getting transaction creator: %s\n", err.Error())
		return shim.Error(err.Error())
	}
	creatorOrg := ""
	creatorCertIssuer := ""
	if !t.testMode {
		creatorOrg, creatorCertIssuer, err = getTxCreatorInfo(creator)
		if err != nil {
			fmt.Errorf("Error extracting creator identity info: %s\n", err.Error())
			return shim.Error(err.Error())
		}
		fmt.Printf("TradeWorkflow Invoke by '%s', '%s'\n", creatorOrg, creatorCertIssuer)
	}

	function, args := stub.GetFunctionAndParameters()
	if function == "requestTrade" {
		// Importer requests a trade
		return t.requestTrade(stub, creatorOrg, creatorCertIssuer, args)
	} else if function == "acceptTrade" {
		// Exporter accepts a trade
		return t.acceptTrade(stub, creatorOrg, creatorCertIssuer, args)
	} else if function == "requestLC" {
		// Importer requests an L/C
		return t.requestLC(stub, creatorOrg, creatorCertIssuer, args)
	} else if function == "issueLC" {
		// Importer's Bank issues an L/C
		return t.issueLC(stub, creatorOrg, creatorCertIssuer, args)
	} else if function == "acceptLC" {
		// Exporter's Bank accepts an L/C
		return t.acceptLC(stub, creatorOrg, creatorCertIssuer, args)
		// } else if function == "requestEL" {
		// 	// Exporter requests an E/L
		// 	return t.requestEL(stub, creatorOrg, creatorCertIssuer, args)
		// } else if function == "issueEL" {
		// 	// Regulatory Authority issues an E/L
		// 	return t.issueEL(stub, creatorOrg, creatorCertIssuer, args)
	} else if function == "prepareShipment" {
		// Exporter prepares a shipment
		return t.prepareShipment(stub, creatorOrg, creatorCertIssuer, args)
	} else if function == "acceptShipmentAndIssueBL" {
		// Carrier validates the shipment and issues a B/L
		return t.acceptShipmentAndIssueBL(stub, creatorOrg, creatorCertIssuer, args)
	} else if function == "requestPayment" {
		// Exporter's Bank requests a payment
		return t.requestPayment(stub, creatorOrg, creatorCertIssuer, args)
	} else if function == "releasePayment" {
		// Exporter's Bank requests a payment
		return t.releasePayment(stub, creatorOrg, creatorCertIssuer, args)
	} else if function == "makePayment" {
		// Importer's Bank makes a payment
		return t.makePayment(stub, creatorOrg, creatorCertIssuer, args)
	} else if function == "updateShipmentLocation" {
		// Carrier updates the shipment location
		return t.updateShipmentLocation(stub, creatorOrg, creatorCertIssuer, args)
	} else if function == "getTradeStatus" {
		// Get status of trade agreement
		return t.getTradeStatus(stub, creatorOrg, creatorCertIssuer, args)
	} else if function == "getLCStatus" {
		// Get the L/C status
		return t.getLCStatus(stub, creatorOrg, creatorCertIssuer, args)
		// } else if function == "getELStatus" {
		// 	// Get the E/L status
		// 	return t.getELStatus(stub, creatorOrg, creatorCertIssuer, args)
	} else if function == "getShipmentLocation" {
		// Get the shipment location
		return t.getShipmentLocation(stub, creatorOrg, creatorCertIssuer, args)
	} else if function == "getBillOfLading" {
		// Get the bill of lading
		return t.getBillOfLading(stub, creatorOrg, creatorCertIssuer, args)
	} else if function == "getAccountBalance" {
		// Get account balance: Exporter/Importer
		return t.getAccountBalance(stub, creatorOrg, creatorCertIssuer, args)
		/*} else if function == "delete" {
		// Deletes an entity from its state
		return t.delete(stub, creatorOrg, creatorCertIssuer, args)*/
	} else if function == "queryState" {
		// Get account balance: Exporter/Importer
		return t.queryState(stub, creatorOrg, creatorCertIssuer, args)
	} else if function == "getPaymentDetails" {
		// Get account balance: Exporter/Importer
		return t.getPaymentDetails(stub, creatorOrg, creatorCertIssuer, args)
	}

	return shim.Error("Invalid invoke function name")
}

// Request a trade agreement
func (t *TradeWorkflowChaincode) requestTrade(stub shim.ChaincodeStubInterface, creatorOrg string, creatorCertIssuer string, args []string) pb.Response {
	var tradeKey string
	var tradeAgreement *TradeAgreement
	var tradeAgreementBytes []byte
	var amount int
	var err error

	// Access control: Only an Importer Org member can invoke this transaction
	// if !t.testMode && !authenticateImporterOrg(creatorOrg, creatorCertIssuer) {
	// 	return shim.Error("Caller not a member of Importer Org. Access denied.")
	// }

	if len(args) != 3 {
		err = errors.New(fmt.Sprintf("Incorrect number of arguments. Expecting 3: {ID, Amount, Description of Goods}. Found %d", len(args)))
		return shim.Error(err.Error())
	}

	amount, err = strconv.Atoi(string(args[1]))
	if err != nil {
		return shim.Error(err.Error())
	}

	tradeAgreement = &TradeAgreement{amount, args[2], REQUESTED, 0, "tradeAgreement"}
	tradeAgreementBytes, err = json.Marshal(tradeAgreement)
	if err != nil {
		return shim.Error("Error marshaling trade agreement structure")
	}

	// Write the state to the ledger
	tradeKey, err = getTradeKey(stub, args[0])
	if err != nil {
		return shim.Error(err.Error())
	}
	err = stub.PutState(tradeKey, tradeAgreementBytes)
	if err != nil {
		return shim.Error(err.Error())
	}
	fmt.Printf("Trade %s request recorded\n", args[0])

	return shim.Success(nil)
}

// Accept a trade agreement
func (t *TradeWorkflowChaincode) acceptTrade(stub shim.ChaincodeStubInterface, creatorOrg string, creatorCertIssuer string, args []string) pb.Response {
	var tradeKey string
	var tradeAgreement *TradeAgreement
	var tradeAgreementBytes []byte
	var err error

	// Access control: Only an Exporter Org member can invoke this transaction
	if !t.testMode && !authenticateExporterOrg(creatorOrg, creatorCertIssuer) {
		return shim.Error("Caller not a member of Exporter Org. Access denied.")
	}

	if len(args) != 1 {
		err = errors.New(fmt.Sprintf("Incorrect number of arguments. Expecting 1: {ID}. Found %d", len(args)))
		return shim.Error(err.Error())
	}

	// Get the state from the ledger
	tradeKey, err = getTradeKey(stub, args[0])
	if err != nil {
		return shim.Error(err.Error())
	}
	tradeAgreementBytes, err = stub.GetState(tradeKey)
	if err != nil {
		return shim.Error(err.Error())
	}

	if len(tradeAgreementBytes) == 0 {
		err = errors.New(fmt.Sprintf("No record found for trade ID %s", args[0]))
		return shim.Error(err.Error())
	}

	// Unmarshal the JSON
	err = json.Unmarshal(tradeAgreementBytes, &tradeAgreement)
	if err != nil {
		return shim.Error(err.Error())
	}

	if tradeAgreement.Status == ACCEPTED {
		fmt.Printf("Trade %s already accepted", args[0])
	} else {
		tradeAgreement.Status = ACCEPTED
		tradeAgreementBytes, err = json.Marshal(tradeAgreement)
		if err != nil {
			return shim.Error("Error marshaling trade agreement structure")
		}
		// Write the state to the ledger
		err = stub.PutState(tradeKey, tradeAgreementBytes)
		if err != nil {
			return shim.Error(err.Error())
		}
	}
	fmt.Printf("Trade %s acceptance recorded\n", args[0])

	return shim.Success(nil)
}

// Request an L/C
func (t *TradeWorkflowChaincode) requestLC(stub shim.ChaincodeStubInterface, creatorOrg string, creatorCertIssuer string, args []string) pb.Response {
	var tradeKey, lcKey string
	var tradeAgreementBytes, letterOfCreditBytes, exporterBytes []byte
	var tradeAgreement *TradeAgreement
	var letterOfCredit *LetterOfCredit
	var err error

	// Access control: Only an Importer Org member can invoke this transaction
	if !t.testMode && !authenticateImporterOrg(creatorOrg, creatorCertIssuer) {
		return shim.Error("Caller not a member of Importer Org. Access denied.")
	}

	if len(args) != 1 {
		err = errors.New(fmt.Sprintf("Incorrect number of arguments. Expecting 1: {Trade ID}. Found %d", len(args)))
		return shim.Error(err.Error())
	}

	// Lookup trade agreement from the ledger
	tradeKey, err = getTradeKey(stub, args[0])
	if err != nil {
		return shim.Error(err.Error())
	}
	tradeAgreementBytes, err = stub.GetState(tradeKey)
	if err != nil {
		return shim.Error(err.Error())
	}

	if len(tradeAgreementBytes) == 0 {
		err = errors.New(fmt.Sprintf("No record found for trade ID %s", args[0]))
		return shim.Error(err.Error())
	}

	// Unmarshal the JSON
	err = json.Unmarshal(tradeAgreementBytes, &tradeAgreement)
	if err != nil {
		return shim.Error(err.Error())
	}

	// Verify that the trade has been agreed to
	if tradeAgreement.Status != ACCEPTED {
		return shim.Error("Trade has not been accepted by the parties")
	}

	// Lookup exporter (L/C beneficiary)
	exporterBytes, err = stub.GetState(expKey)
	if err != nil {
		return shim.Error(err.Error())
	}

	letterOfCredit = &LetterOfCredit{"", "", string(exporterBytes), tradeAgreement.Amount, []string{}, REQUESTED, "LOC"}
	letterOfCreditBytes, err = json.Marshal(letterOfCredit)
	if err != nil {
		return shim.Error("Error marshaling letter of credit structure")
	}

	// Write the state to the ledger
	lcKey, err = getLCKey(stub, args[0])
	if err != nil {
		return shim.Error(err.Error())
	}
	err = stub.PutState(lcKey, letterOfCreditBytes)
	if err != nil {
		return shim.Error(err.Error())
	}
	fmt.Printf("Letter of Credit request for trade %s recorded\n", args[0])

	return shim.Success(nil)
}

// Issue an L/C
// We don't need to check the trade status if the L/C request has already been recorded
func (t *TradeWorkflowChaincode) issueLC(stub shim.ChaincodeStubInterface, creatorOrg string, creatorCertIssuer string, args []string) pb.Response {
	var lcKey string
	var letterOfCreditBytes []byte
	var letterOfCredit *LetterOfCredit
	var err error

	// Access control: Only an IssuingBankOrg member can invoke this transaction
	if !t.testMode && !authenticateIssuingBankOrg(creatorOrg, creatorCertIssuer) {
		return shim.Error("Caller not a member of IssuingBankOrg. Access denied.")
	}

	if len(args) < 3 {
		err = errors.New(fmt.Sprintf("Incorrect number of arguments. Expecting at least 3: {Trade ID, L/C ID, Expiry Date} [List of Documents]. Found %d", len(args)))
		return shim.Error(err.Error())
	}

	// Lookup L/C from the ledger
	lcKey, err = getLCKey(stub, args[0])
	if err != nil {
		return shim.Error(err.Error())
	}
	letterOfCreditBytes, err = stub.GetState(lcKey)
	if err != nil {
		return shim.Error(err.Error())
	}

	// Unmarshal the JSON
	err = json.Unmarshal(letterOfCreditBytes, &letterOfCredit)
	if err != nil {
		return shim.Error(err.Error())
	}

	if letterOfCredit.Status == ISSUED {
		fmt.Printf("L/C for trade %s already issued", args[0])
	} else if letterOfCredit.Status == ACCEPTED {
		fmt.Printf("L/C for trade %s already accepted", args[0])
	} else {
		letterOfCredit.Id = args[1]
		letterOfCredit.ExpirationDate = args[2]
		letterOfCredit.Documents = args[3:]
		letterOfCredit.Status = ISSUED
		letterOfCreditBytes, err = json.Marshal(letterOfCredit)
		if err != nil {
			return shim.Error("Error marshaling L/C structure")
		}
		// Write the state to the ledger
		err = stub.PutState(lcKey, letterOfCreditBytes)
		if err != nil {
			return shim.Error(err.Error())
		}
	}
	fmt.Printf("L/C issuance for trade %s recorded\n", args[0])

	return shim.Success(nil)
}

// Accept an L/C
func (t *TradeWorkflowChaincode) acceptLC(stub shim.ChaincodeStubInterface, creatorOrg string, creatorCertIssuer string, args []string) pb.Response {
	var lcKey string
	var letterOfCreditBytes []byte
	var letterOfCredit *LetterOfCredit
	var err error

	// Access control: Only an Exporter Org member can invoke this transaction
	if !t.testMode && !authenticateAdvisingBankOrg(creatorOrg, creatorCertIssuer) {
		return shim.Error("Caller not a member of Exporter Org. Access denied.")
	}

	if len(args) != 1 {
		err = errors.New(fmt.Sprintf("Incorrect number of arguments. Expecting 1: {Trade ID}. Found %d", len(args)))
		return shim.Error(err.Error())
	}

	// Lookup L/C from the ledger
	lcKey, err = getLCKey(stub, args[0])
	if err != nil {
		return shim.Error(err.Error())
	}
	letterOfCreditBytes, err = stub.GetState(lcKey)
	if err != nil {
		return shim.Error(err.Error())
	}

	// Unmarshal the JSON
	err = json.Unmarshal(letterOfCreditBytes, &letterOfCredit)
	if err != nil {
		return shim.Error(err.Error())
	}

	if letterOfCredit.Status == ACCEPTED {
		fmt.Printf("L/C for trade %s already accepted", args[0])
	} else if letterOfCredit.Status == REQUESTED {
		fmt.Printf("L/C for trade %s has not been issued", args[0])
		return shim.Error("L/C not issued yet")
	} else {
		letterOfCredit.Status = ACCEPTED
		letterOfCreditBytes, err = json.Marshal(letterOfCredit)
		if err != nil {
			return shim.Error("Error marshaling L/C structure")
		}
		// Write the state to the ledger
		err = stub.PutState(lcKey, letterOfCreditBytes)
		if err != nil {
			return shim.Error(err.Error())
		}
	}
	fmt.Printf("L/C acceptance for trade %s recorded\n", args[0])

	return shim.Success(nil)
}

// Prepare a shipment; preparation is indicated by setting the location as SOURCE
func (t *TradeWorkflowChaincode) prepareShipment(stub shim.ChaincodeStubInterface, creatorOrg string, creatorCertIssuer string, args []string) pb.Response {
	var blKey, jsonResp, tradeKey string
	var tradeAgreementBytes, billOfLadingBytes, exporterBytes, carrierBytes, beneficiaryBytes []byte
	// var exportLicense *ExportLicense
	var billOfLading *BillOfLading
	var tradeAgreement *TradeAgreement
	var err error

	// Access control: Only an Exporter Org member can invoke this transaction
	if !t.testMode && !authenticateExporterOrg(creatorOrg, creatorCertIssuer) {
		return shim.Error("Caller not a member of Exporter Org. Access denied.")
	}
	fmt.Println("Pb3")
	if len(args) != 3 {
		err = errors.New(fmt.Sprintf("Incorrect number of arguments. Expecting 3: {Trade ID,Source,Destination}. Found %d", len(args)))
		return shim.Error(err.Error())
	}
	fmt.Println("Pb4")
	// Lookup shipment location from the ledger
	blKey, err = getBLKey(stub, args[0])
	if err != nil {
		return shim.Error(err.Error())
	}
	billOfLadingBytes, err = stub.GetState(blKey)
	if err != nil {
		jsonResp = "{\"Error\":\"Failed to get state for " + blKey + "\"}"
		return shim.Error(jsonResp)
	}

	fmt.Println("Pb1")
	if len(billOfLadingBytes) != 0 {
		err = json.Unmarshal(billOfLadingBytes, &billOfLading)
		if err != nil {
			return shim.Error(err.Error())
		}
		if billOfLading.Status == "REQUESTED" {
			jsonResp = "{\"Error\":\"Shipment is Already Requested\"}"
			return shim.Error(jsonResp)
		} else if billOfLading.Status == "ACCEPTED" {
			jsonResp = "{\"Error\":\"Shipment is Already Accepted\"}"
			return shim.Error(jsonResp)
		} else if billOfLading.Status == "STARTED" {
			jsonResp = "{\"Error\":\"Shipment is Already inprogress\"}"
			return shim.Error(jsonResp)
		} else {
			jsonResp = "{\"Error\":\"Shipment is Already delivered\"}"
			return shim.Error(jsonResp)
		}
	}
	fmt.Println("P1")
	tradeKey, err = getTradeKey(stub, args[0])
	if err != nil {
		return shim.Error(err.Error())
	}
	tradeAgreementBytes, err = stub.GetState(tradeKey)
	if err != nil {
		return shim.Error(err.Error())
	}

	if len(tradeAgreementBytes) == 0 {
		err = errors.New(fmt.Sprintf("No record found for trade ID %s", args[0]))
		return shim.Error(err.Error())
	}

	// Unmarshal the JSON
	err = json.Unmarshal(tradeAgreementBytes, &tradeAgreement)
	if err != nil {
		return shim.Error(err.Error())
	}

	// Lookup exporter
	exporterBytes, err = stub.GetState(expKey)
	if err != nil {
		return shim.Error(err.Error())
	}

	// Lookup carrier
	carrierBytes, err = stub.GetState(carKey)
	if err != nil {
		return shim.Error(err.Error())
	}

	// Lookup importer's bank (beneficiary of the title to goods after paymen tis made)
	beneficiaryBytes, err = stub.GetState(ibKey)
	if err != nil {
		return shim.Error(err.Error())
	}
	billOfLading = &BillOfLading{}
	billOfLading.Exporter = string(exporterBytes)
	billOfLading.Carrier = string(carrierBytes)
	billOfLading.DescriptionOfGoods = tradeAgreement.DescriptionOfGoods
	billOfLading.Amount = tradeAgreement.Amount
	billOfLading.Beneficiary = string(beneficiaryBytes)
	billOfLading.Status = "REQUESTED"
	billOfLading.SourcePort = args[1]
	billOfLading.DestinationPort = args[2]
	billOfLading.Id = blKey
	billOfLading.TradeId = args[0]
	billOfLading.ShipmentLocation = args[1]
	billOfLading.Doctype = "BOL"
	billOfLadingBytes, err = json.Marshal(billOfLading)
	if err != nil {
		return shim.Error("Error marshaling bill of lading structure")
	}
	fmt.Printf("P2")
	err = stub.PutState(blKey, billOfLadingBytes)
	if err != nil {
		return shim.Error(err.Error())
	}
	fmt.Printf("P3")
	fmt.Printf("Shipment preparation for trade %s recorded\n", args[0])

	return shim.Success(nil)
}

// Accept a shipment and issue a B/L
func (t *TradeWorkflowChaincode) acceptShipmentAndIssueBL(stub shim.ChaincodeStubInterface, creatorOrg string, creatorCertIssuer string, args []string) pb.Response {
	var blKey, jsonResp string
	var billOfLadingBytes []byte
	var billOfLading *BillOfLading
	var err error

	// Access control: Only an Carrier Org member can invoke this transaction
	if !t.testMode && !authenticateCarrierOrg(creatorOrg, creatorCertIssuer) {
		return shim.Error("Caller not a member of Carrier Org. Access denied.")
	}

	if len(args) != 3 {
		err = errors.New(fmt.Sprintf("Incorrect number of arguments. Expecting 5: {Trade ID, B/L ID, Expiration Date}. Found %d", len(args)))
		return shim.Error(err.Error())
	}

	// Lookup shipment location from the ledger
	blKey, err = getBLKey(stub, args[0])
	if err != nil {
		return shim.Error(err.Error())
	}
	billOfLadingBytes, err = stub.GetState(blKey)
	if err != nil {
		jsonResp = "{\"Error\":\"Failed to get state for " + blKey + "\"}"
		return shim.Error(jsonResp)
	}

	if len(billOfLadingBytes) != 0 {
		err = json.Unmarshal(billOfLadingBytes, &billOfLading)
		if err != nil {
			return shim.Error(err.Error())
		}
		if billOfLading.Status == "REQUESTED" {

			billOfLading.Status = "ACCEPTED"
			billOfLading.ExpirationDate = args[2]

			billOfLadingBytes, err = json.Marshal(billOfLading)
			if err != nil {
				return shim.Error("Error marshaling bill of lading structure")
			}

			// Write the state to the ledger
			err = stub.PutState(blKey, billOfLadingBytes)
			if err != nil {
				return shim.Error(err.Error())
			}
			fmt.Printf("Bill of Lading for trade %s recorded\n", args[0])

			return shim.Success(nil)

		} else if billOfLading.Status == "ACCEPTED" {
			jsonResp = "{\"Error\":\"Shipment is Already Accepted\"}"
			return shim.Error(jsonResp)
		} else if billOfLading.Status == "STARTED" {
			jsonResp = "{\"Error\":\"Shipment is Already inprogress\"}"
			return shim.Error(jsonResp)
		} else {
			jsonResp = "{\"Error\":\"Shipment is Already delivered\"}"
			return shim.Error(jsonResp)
		}
	} else {
		err = errors.New(fmt.Sprintf("No record found for BILL OF LADING  ID%s", args[1]))
		return shim.Error(err.Error())
	}

	// Lookup trade agreement from the ledger

}

// Request a payment
func (t *TradeWorkflowChaincode) requestPayment(stub shim.ChaincodeStubInterface, creatorOrg string, creatorCertIssuer string, args []string) pb.Response {
	var blKey, paymentKey, tradeKey, jsonResp string
	var paymentBytes, tradeAgreementBytes, billOfLadingBytes []byte
	var tradeAgreement *TradeAgreement
	var billOfLading *BillOfLading
	var err error
	var paymentAmount int

	// Access control: Only an AdvisingBankOrg member can invoke this transaction
	if !t.testMode && !authenticateExporterOrg(creatorOrg, creatorCertIssuer) {
		return shim.Error("Caller not a member of exporterOrg. Access denied.")
	}

	if len(args) != 1 {
		err = errors.New(fmt.Sprintf("Incorrect number of arguments. Expecting 1: {Trade ID}. Found %d", len(args)))
		return shim.Error(err.Error())
	}

	// Lookup trade agreement from the ledger
	tradeKey, err = getTradeKey(stub, args[0])
	if err != nil {
		return shim.Error(err.Error())
	}
	tradeAgreementBytes, err = stub.GetState(tradeKey)
	if err != nil {
		return shim.Error(err.Error())
	}

	if len(tradeAgreementBytes) == 0 {
		err = errors.New(fmt.Sprintf("No record found for trade ID %s", args[0]))
		return shim.Error(err.Error())
	}

	// Unmarshal the JSON
	err = json.Unmarshal(tradeAgreementBytes, &tradeAgreement)
	if err != nil {
		return shim.Error(err.Error())
	}

	// Lookup shipment location from the ledger
	blKey, err = getBLKey(stub, args[0])
	if err != nil {
		return shim.Error(err.Error())
	}

	billOfLadingBytes, err = stub.GetState(blKey)
	if err != nil {
		jsonResp = "{\"Error\":\"Failed to get state for " + blKey + "\"}"
		return shim.Error(jsonResp)
	}
	if len(billOfLadingBytes) == 0 {
		fmt.Printf("Shipment for trade %s has not been prepared yet", args[0])
		return shim.Error("Shipment not prepared yet")
	}
	err = json.Unmarshal(billOfLadingBytes, &billOfLading)
	if err != nil {
		return shim.Error(err.Error())
	}
	if (billOfLading.Status != "ACCEPTED") && (billOfLading.Status != "DELIVERED") {
		if billOfLading.Status == "STARTED" {
			jsonResp = "{\"Error\":\"Shipment is Already inprogress.So you cannot request for a payment\"}"
			return shim.Error(jsonResp)
		} else if billOfLading.Status == "REQUESTED" {
			jsonResp = "{\"Error\":\"Shipment is Not yet Accepted.So you cannot request for a payment\"}"
			return shim.Error(jsonResp)
		}

	}

	// Check if there's already a pending payment request
	paymentKey, err = getPaymentKey(stub, args[0])
	if err != nil {
		return shim.Error(err.Error())
	}
	paymentBytes, err = stub.GetState(paymentKey)
	if err != nil {
		return shim.Error(err.Error())
	}

	if len(paymentBytes) != 0 { // The value doesn't matter as this is a temporary key used as a marker
		fmt.Printf("Payment request already pending for trade %s\n", args[0])
	} else {
		// Check what has been paid up to this point
		fmt.Printf("Amount paid thus far for trade %s = %d; total required = %d\n", args[0], tradeAgreement.Payment, tradeAgreement.Amount)
		if tradeAgreement.Amount == tradeAgreement.Payment { // Payment has already been settled
			fmt.Printf("Payment already settled for trade %s\n", args[0])
			return shim.Error("Payment already settled")
		}
		if billOfLading.ShipmentLocation == billOfLading.SourcePort && tradeAgreement.Payment != 0 { // Suppress duplicate requests for partial payment
			fmt.Printf("Partial payment already made for trade %s\n", args[0])
			return shim.Error("Partial payment already made")
		}
		if billOfLading.ShipmentLocation == billOfLading.SourcePort {
			paymentAmount = tradeAgreement.Amount / 2
		} else {
			paymentAmount = tradeAgreement.Amount - tradeAgreement.Payment
		}
		tid, _ := strconv.Atoi(args[0])
		// Record request on ledger
		paymentDetails := &Payment{tid, paymentAmount, "PAYMENTREQUESTED"}
		paymentDetailsBytes, err := json.Marshal(paymentDetails)
		if err != nil {
			return shim.Error("Error marshaling bill of lading structure")
		}
		err = stub.PutState(paymentKey, paymentDetailsBytes)
		if err != nil {
			return shim.Error(err.Error())
		}
		fmt.Printf("Payment request for trade %s recorded\n", args[0])
	}
	return shim.Success(nil)
}
func (t *TradeWorkflowChaincode) getPaymentDetails(stub shim.ChaincodeStubInterface, creatorOrg string, creatorCertIssuer string, args []string) pb.Response {
	var paymentKey string
	var paymentBytes []byte
	var err error
	if !t.testMode && !(authenticateImporterOrg(creatorOrg, creatorCertIssuer) || authenticateIssuingBankOrg(creatorOrg, creatorCertIssuer)) {
		return shim.Error("Caller not a member of IssuingBankOrg or ImporterOrg. Access denied.")
	}
	if len(args) != 1 {
		err = errors.New(fmt.Sprintf("Incorrect number of arguments. Expecting 1: {Trade ID}. Found %d", len(args)))
		return shim.Error(err.Error())
	}
	paymentKey, err = getPaymentKey(stub, args[0])
	if err != nil {
		return shim.Error(err.Error())
	}
	paymentBytes, err = stub.GetState(paymentKey)
	if err != nil {
		return shim.Error(err.Error())
	}

	if len(paymentBytes) == 0 {
		fmt.Printf("No payment request found for trade %s", args[0])
		return shim.Error("No payment request found")
	} else {
		return shim.Success(paymentBytes)
	}

}
func (t *TradeWorkflowChaincode) releasePayment(stub shim.ChaincodeStubInterface, creatorOrg string, creatorCertIssuer string, args []string) pb.Response {
	var paymentKey string
	var payment *Payment
	if len(args) != 1 {
		err := errors.New(fmt.Sprintf("Incorrect number of arguments. Expecting 1: {Trade ID}. Found %d", len(args)))
		return shim.Error(err.Error())
	}
	paymentKey, err := getPaymentKey(stub, args[0])
	if err != nil {
		return shim.Error(err.Error())
	}
	paymentBytes, err := stub.GetState(paymentKey)
	if err != nil {
		return shim.Error(err.Error())
	}
	if len(paymentBytes) == 0 {
		err = errors.New(fmt.Sprintf("No record found for Payment ID %s", args[0]))
		return shim.Error(err.Error())
	}

	// Unmarshal the JSON
	err = json.Unmarshal(paymentBytes, &payment)
	if err != nil {
		return shim.Error(err.Error())
	}
	if payment.Status == "PAYMENTREQUESTED" {
		payment.Status = "PAYMENTACCEPTED"
		paymentDetailsBytes, err := json.Marshal(payment)
		if err != nil {
			return shim.Error("Error marshaling bill of lading structure")
		}
		err = stub.PutState(paymentKey, paymentDetailsBytes)
		if err != nil {
			return shim.Error(err.Error())
		}
		return shim.Success(nil)
	} else {
		return shim.Error("Already Accepted")
	}

}

// Make a payment
func (t *TradeWorkflowChaincode) makePayment(stub shim.ChaincodeStubInterface, creatorOrg string, creatorCertIssuer string, args []string) pb.Response {
	var paymentKey, tradeKey, jsonResp, blKey string
	var paymentAmount, expBal, impBal int
	var tradeAgreementBytes, impBalBytes, expBalBytes, billOfLadingBytes []byte
	var tradeAgreement *TradeAgreement
	var billOfLading *BillOfLading
	var payment *Payment
	var err error

	// Access control: Only an IssuingBankOrg member can invoke this transaction
	if !t.testMode && !authenticateIssuingBankOrg(creatorOrg, creatorCertIssuer) {
		return shim.Error("Caller not a member of IssuingBankOrg. Access denied.")
	}

	if len(args) != 2 {
		err = errors.New(fmt.Sprintf("Incorrect number of arguments. Expecting 1: {Trade ID, paymentAmount}. Found %d", len(args)))
		return shim.Error(err.Error())
	}

	// Lookup trade agreement from the ledger
	tradeKey, err = getTradeKey(stub, args[0])
	if err != nil {
		return shim.Error(err.Error())
	}
	tradeAgreementBytes, err = stub.GetState(tradeKey)
	if err != nil {
		return shim.Error(err.Error())
	}

	if len(tradeAgreementBytes) == 0 {
		err = errors.New(fmt.Sprintf("No record found for trade ID %s", args[0]))
		return shim.Error(err.Error())
	}

	// Unmarshal the JSON
	err = json.Unmarshal(tradeAgreementBytes, &tradeAgreement)
	if err != nil {
		return shim.Error(err.Error())
	}

	// Lookup shipment location from the ledger
	blKey, err = getBLKey(stub, args[0])
	if err != nil {
		return shim.Error(err.Error())
	}

	billOfLadingBytes, err = stub.GetState(blKey)
	if err != nil {
		jsonResp = "{\"Error\":\"Failed to get state for " + blKey + "\"}"
		return shim.Error(jsonResp)
	}
	if len(billOfLadingBytes) == 0 {
		fmt.Printf("Shipment for trade %s has not been prepared yet", args[0])
		return shim.Error("Shipment not prepared yet")
	}
	err = json.Unmarshal(billOfLadingBytes, &billOfLading)
	if err != nil {
		return shim.Error(err.Error())
	}

	if (billOfLading.Status != "ACCEPTED") && (billOfLading.Status != "DELIVERED") {
		if billOfLading.Status == "STARTED" {
			jsonResp = "{\"Error\":\"Shipment is Already inprogress\"}"
			return shim.Error(jsonResp)
		} else if billOfLading.Status == "REQUESTED" {
			jsonResp = "{\"Error\":\"Shipment is Not yet Accepted\"}"
			return shim.Error(jsonResp)
		}

	}
	paymentKey, err = getPaymentKey(stub, args[0])
	if err != nil {
		return shim.Error(err.Error())
	}
	paymentBytes, err := stub.GetState(paymentKey)
	if err != nil {
		return shim.Error(err.Error())
	}
	if len(paymentBytes) == 0 {
		err = errors.New(fmt.Sprintf("No record found for Payment ID %s", args[0]))
		return shim.Error(err.Error())
	}

	// Unmarshal the JSON
	err = json.Unmarshal(paymentBytes, &payment)
	if err != nil {
		return shim.Error(err.Error())
	}
	if payment.Status != "PAYMENTACCEPTED" {
		return shim.Error("Payment request is not yet Released by Importer Org")
	}
	// Lookup account balances
	expBalBytes, err = stub.GetState(expBalKey)
	if err != nil {
		return shim.Error(err.Error())
	}
	expBal, err = strconv.Atoi(string(expBalBytes))
	if err != nil {
		return shim.Error(err.Error())
	}
	impBalBytes, err = stub.GetState(impBalKey)
	if err != nil {
		return shim.Error(err.Error())
	}
	impBal, err = strconv.Atoi(string(impBalBytes))
	if err != nil {
		return shim.Error(err.Error())
	}

	// Record transfer of funds

	paymentAmount, _ = strconv.Atoi(args[1])

	tradeAgreement.Payment += paymentAmount

	if impBal < paymentAmount {
		return shim.Error("Importer's bank balance  is insufficient to cover payment amount ")
	}
	expBal += paymentAmount
	impBal -= paymentAmount

	if tradeAgreement.Payment == tradeAgreement.Amount {
		tradeAgreement.Status = "COMPLETED"
	}

	// Update ledger state
	tradeAgreementBytes, err = json.Marshal(tradeAgreement)
	if err != nil {
		return shim.Error("Error marshaling trade agreement structure")
	}
	err = stub.PutState(tradeKey, tradeAgreementBytes)
	if err != nil {
		return shim.Error(err.Error())
	}
	err = stub.PutState(expBalKey, []byte(strconv.Itoa(expBal)))
	if err != nil {
		return shim.Error(err.Error())
	}
	err = stub.PutState(impBalKey, []byte(strconv.Itoa(impBal)))
	if err != nil {
		return shim.Error(err.Error())
	}

	// Delete request key from ledger
	err = stub.DelState(paymentKey)
	if err != nil {
		fmt.Println(err.Error())
		return shim.Error("Failed to delete payment request from ledger")
	}

	return shim.Success(nil)
}

// Update shipment location; we will only allow SOURCE and DESTINATION as valid locations for this contract
func (t *TradeWorkflowChaincode) updateShipmentLocation(stub shim.ChaincodeStubInterface, creatorOrg string, creatorCertIssuer string, args []string) pb.Response {
	var blKey, jsonResp string
	var billOfLadingBytes []byte
	var billOfLading *BillOfLading
	var err error

	// Access control: Only a Carrier Org member can invoke this transaction
	if !t.testMode && !authenticateCarrierOrg(creatorOrg, creatorCertIssuer) {
		return shim.Error("Caller not a member of Carrier Org. Access denied.")
	}

	if len(args) != 2 {
		err = errors.New(fmt.Sprintf("Incorrect number of arguments. Expecting 1: {Trade ID, Location}. Found %d", len(args)))
		return shim.Error(err.Error())
	}
	blKey, err = getBLKey(stub, args[0])
	if err != nil {
		return shim.Error(err.Error())
	}

	billOfLadingBytes, err = stub.GetState(blKey)
	if err != nil {
		jsonResp = "{\"Error\":\"Failed to get state for " + blKey + "\"}"
		return shim.Error(jsonResp)
	}
	if len(billOfLadingBytes) == 0 {
		fmt.Printf("Shipment for trade %s has not been prepared yet", args[0])
		return shim.Error("Shipment not prepared yet")
	}
	err = json.Unmarshal(billOfLadingBytes, &billOfLading)
	if err != nil {
		return shim.Error(err.Error())
	}
	// Lookup shipment location from the ledger

	if billOfLading.ShipmentLocation == args[1] {
		fmt.Printf("Shipment for trade %s is already in location %s", args[0], args[1])
	} else {
		billOfLading.ShipmentLocation = args[1]
		billOfLading.Status = "STARTED"
	}

	if billOfLading.DestinationPort == args[1] {
		billOfLading.Status = "DELIVERED"
	}

	// Write the state to the ledger
	billOfLadingBytes, err = json.Marshal(billOfLading)
	if err != nil {
		return shim.Error("Error marshaling bill of lading structure")
	}

	// Write the state to the ledger
	err = stub.PutState(blKey, billOfLadingBytes)
	if err != nil {
		return shim.Error(err.Error())
	}
	fmt.Printf("Bill of Lading for trade %s recorded\n", args[0])
	return shim.Success(nil)
}

// Get current state of a trade agreement
func (t *TradeWorkflowChaincode) getTradeStatus(stub shim.ChaincodeStubInterface, creatorOrg string, creatorCertIssuer string, args []string) pb.Response {
	var tradeKey, jsonResp string
	var tradeAgreement TradeAgreement
	var tradeAgreementBytes []byte
	var err error

	// Access control: Only an Importer or Exporter Org member can invoke this transaction
	if !t.testMode && !(authenticateImporterOrg(creatorOrg, creatorCertIssuer) || authenticateExporterOrg(creatorOrg, creatorCertIssuer)) {
		return shim.Error("Caller not a member of Importer or Exporter Org. Access denied.")
	}

	if len(args) != 1 {
		return shim.Error("Incorrect number of arguments. Expecting 1: <trade ID>")
	}

	// Get the state from the ledger
	tradeKey, err = getTradeKey(stub, args[0])
	if err != nil {
		return shim.Error(err.Error())
	}
	tradeAgreementBytes, err = stub.GetState(tradeKey)
	if err != nil {
		jsonResp = "{\"Error\":\"Failed to get state for " + tradeKey + "\"}"
		return shim.Error(jsonResp)
	}

	if len(tradeAgreementBytes) == 0 {
		jsonResp = "{\"Error\":\"No record found for " + tradeKey + "\"}"
		return shim.Error(jsonResp)
	}

	// Unmarshal the JSON
	err = json.Unmarshal(tradeAgreementBytes, &tradeAgreement)
	if err != nil {
		return shim.Error(err.Error())
	}

	jsonResp = "{\"Status\":\"" + tradeAgreement.Status + "\"}"
	fmt.Printf("Query Response:%s\n", jsonResp)
	return shim.Success([]byte(jsonResp))
}

// Get current state of a Letter of Credit
func (t *TradeWorkflowChaincode) getLCStatus(stub shim.ChaincodeStubInterface, creatorOrg string, creatorCertIssuer string, args []string) pb.Response {
	var lcKey, jsonResp string
	var letterOfCredit LetterOfCredit
	var letterOfCreditBytes []byte
	var err error

	// Access control: Only an Importer or Exporter Org member can invoke this transaction
	if !t.testMode && !(authenticateImporterOrg(creatorOrg, creatorCertIssuer) || authenticateExporterOrg(creatorOrg, creatorCertIssuer)) {
		return shim.Error("Caller not a member of Importer or Exporter Org. Access denied.")
	}

	if len(args) != 1 {
		return shim.Error("Incorrect number of arguments. Expecting 1: <trade ID>")
	}

	// Get the state from the ledger
	lcKey, err = getLCKey(stub, args[0])
	if err != nil {
		return shim.Error(err.Error())
	}
	letterOfCreditBytes, err = stub.GetState(lcKey)
	if err != nil {
		jsonResp = "{\"Error\":\"Failed to get state for " + lcKey + "\"}"
		return shim.Error(jsonResp)
	}

	if len(letterOfCreditBytes) == 0 {
		jsonResp = "{\"Error\":\"No record found for " + lcKey + "\"}"
		return shim.Error(jsonResp)
	}

	// Unmarshal the JSON
	err = json.Unmarshal(letterOfCreditBytes, &letterOfCredit)
	if err != nil {
		return shim.Error(err.Error())
	}

	jsonResp = "{\"Status\":\"" + letterOfCredit.Status + "\"}"
	fmt.Printf("Query Response:%s\n", jsonResp)
	return shim.Success([]byte(jsonResp))
}

// Get current state of an Export License
// func (t *TradeWorkflowChaincode) getELStatus(stub shim.ChaincodeStubInterface, creatorOrg string, creatorCertIssuer string, args []string) pb.Response {
// 	var elKey, jsonResp string
// 	var exportLicense ExportLicense
// 	var exportLicenseBytes []byte
// 	var err error

// 	// Access control: Only an Exporter or Regulator Org member can invoke this transaction
// 	if !t.testMode && !(authenticateExporterOrg(creatorOrg, creatorCertIssuer) || authenticateRegulatorOrg(creatorOrg, creatorCertIssuer)) {
// 		return shim.Error("Caller not a member of Exporter or Regulator Org. Access denied.")
// 	}

// 	if len(args) != 1 {
// 		return shim.Error("Incorrect number of arguments. Expecting 1: <trade ID>")
// 	}

// 	// Get the state from the ledger
// 	elKey, err = getELKey(stub, args[0])
// 	if err != nil {
// 		return shim.Error(err.Error())
// 	}
// 	exportLicenseBytes, err = stub.GetState(elKey)
// 	if err != nil {
// 		jsonResp = "{\"Error\":\"Failed to get state for " + elKey + "\"}"
// 		return shim.Error(jsonResp)
// 	}

// 	if len(exportLicenseBytes) == 0 {
// 		jsonResp = "{\"Error\":\"No record found for " + elKey + "\"}"
// 		return shim.Error(jsonResp)
// 	}

// 	// Unmarshal the JSON
// 	err = json.Unmarshal(exportLicenseBytes, &exportLicense)
// 	if err != nil {
// 		return shim.Error(err.Error())
// 	}

// 	jsonResp = "{\"Status\":\"" + exportLicense.Status + "\"}"
// 	fmt.Printf("Query Response:%s\n", jsonResp)
// 	return shim.Success([]byte(jsonResp))
// }

// Get current location of a shipment
func (t *TradeWorkflowChaincode) getShipmentLocation(stub shim.ChaincodeStubInterface, creatorOrg string, creatorCertIssuer string, args []string) pb.Response {
	var blKey, jsonResp string
	var billOfLadingBytes []byte
	var billOfLading *BillOfLading
	var err error

	// Access control: Only an Importer or Exporter or Carrier Org member can invoke this transaction
	if !t.testMode && !(authenticateImporterOrg(creatorOrg, creatorCertIssuer) || authenticateExporterOrg(creatorOrg, creatorCertIssuer) || authenticateCarrierOrg(creatorOrg, creatorCertIssuer)) {
		return shim.Error("Caller not a member of Importer or Exporter or Carrier Org. Access denied.")
	}

	if len(args) != 1 {
		return shim.Error("Incorrect number of arguments. Expecting 1: <trade ID>")
	}

	// Get the state from the ledger
	billOfLadingBytes, err = stub.GetState(blKey)
	if err != nil {
		jsonResp = "{\"Error\":\"Failed to get state for " + blKey + "\"}"
		return shim.Error(jsonResp)
	}
	if len(billOfLadingBytes) == 0 {
		fmt.Printf("Shipment for trade %s has not been prepared yet", args[0])
		return shim.Error("No Records Found")
	}
	err = json.Unmarshal(billOfLadingBytes, &billOfLading)
	if err != nil {
		return shim.Error(err.Error())
	}
	jsonResp = "{\"Location\":\"" + billOfLading.ShipmentLocation + "\"}"
	fmt.Printf("Query Response:%s\n", jsonResp)
	return shim.Success([]byte(jsonResp))
}

// Get Bill of Lading
func (t *TradeWorkflowChaincode) getBillOfLading(stub shim.ChaincodeStubInterface, creatorOrg string, creatorCertIssuer string, args []string) pb.Response {
	var blKey, jsonResp string
	var billOfLadingBytes []byte
	var err error

	// Access control: Only an Importer or Exporter or Carrier Org member can invoke this transaction
	if !t.testMode && !(authenticateImporterOrg(creatorOrg, creatorCertIssuer) || authenticateExporterOrg(creatorOrg, creatorCertIssuer) || authenticateCarrierOrg(creatorOrg, creatorCertIssuer)) {
		return shim.Error("Caller not a member of Importer or Exporter or Carrier Org. Access denied.")
	}

	if len(args) != 1 {
		return shim.Error("Incorrect number of arguments. Expecting 1: <trade ID>")
	}

	// Get the state from the ledger
	blKey, err = getBLKey(stub, args[0])
	if err != nil {
		return shim.Error(err.Error())
	}
	billOfLadingBytes, err = stub.GetState(blKey)
	if err != nil {
		jsonResp = "{\"Error\":\"Failed to get state for " + blKey + "\"}"
		return shim.Error(jsonResp)
	}

	if len(billOfLadingBytes) == 0 {
		jsonResp = "{\"Error\":\"No record found for " + blKey + "\"}"
		return shim.Error(jsonResp)
	}
	fmt.Printf("Query Response:%s\n", string(billOfLadingBytes))
	return shim.Success(billOfLadingBytes)
}

// Get current account balance for a given participant
func (t *TradeWorkflowChaincode) getAccountBalance(stub shim.ChaincodeStubInterface, creatorOrg string, creatorCertIssuer string, args []string) pb.Response {
	var entity, balanceKey, jsonResp string
	var balanceBytes []byte
	var err error

	if len(args) != 1 {
		return shim.Error("Incorrect number of arguments. Expecting 1: { Entity}")
	}

	entity = strings.ToLower(args[0])
	if entity == "exporter" {
		// Access control: Only an Exporter Org member can invoke this transaction
		if !t.testMode && !authenticateExporterOrg(creatorOrg, creatorCertIssuer) {
			return shim.Error("Caller not a member of Exporter Org. Access denied.")
		}
		balanceKey = expBalKey
	} else if entity == "importer" {
		// Access control: Only an Importer Org member can invoke this transaction
		if !t.testMode && !authenticateImporterOrg(creatorOrg, creatorCertIssuer) {
			return shim.Error("Caller not a member of Importer Org. Access denied.")
		}
		balanceKey = impBalKey
	} else {
		err = errors.New(fmt.Sprintf("Invalid entity %s; Permissible values: {exporter, importer}", args[1]))
		return shim.Error(err.Error())
	}

	// Get the account balances from the ledger
	balanceBytes, err = stub.GetState(balanceKey)
	if err != nil {
		jsonResp = "{\"Error\":\"Failed to get state for " + balanceKey + "\"}"
		return shim.Error(jsonResp)
	}

	if len(balanceBytes) == 0 {
		jsonResp = "{\"Error\":\"No record found for " + balanceKey + "\"}"
		return shim.Error(jsonResp)
	}
	jsonResp = "{\"Balance\":\"" + string(balanceBytes) + "\"}"
	fmt.Printf("Query Response:%s\n", jsonResp)
	return shim.Success([]byte(jsonResp))
}
func constructQueryResponseFromIterator(resultsIterator shim.StateQueryIteratorInterface) (*bytes.Buffer, error) {
	// buffer is a JSON array containing QueryResults
	var buffer bytes.Buffer
	buffer.WriteString("[")

	bArrayMemberAlreadyWritten := false
	for resultsIterator.HasNext() {
		queryResponse, err := resultsIterator.Next()
		if err != nil {
			return nil, err
		}
		// Add a comma before array members, suppress it for the first array member
		if bArrayMemberAlreadyWritten == true {
			buffer.WriteString(",")
		}
		buffer.WriteString("{\"Key\":")
		buffer.WriteString("\"")
		buffer.WriteString(queryResponse.Key)
		buffer.WriteString("\"")

		buffer.WriteString(", \"Record\":")
		// Record is a JSON object, so we write as-is
		buffer.WriteString(string(queryResponse.Value))
		buffer.WriteString("}")
		bArrayMemberAlreadyWritten = true
	}
	buffer.WriteString("]")

	return &buffer, nil
}

// =======Rich queries =========================================================================
// Two examples of rich queries are provided below (parameterized query and ad hoc query).
// Rich queries pass a query string to the state database.
// Rich queries are only supported by state database implementations
//  that support rich query (e.g. CouchDB).
// The query string is in the syntax of the underlying state database.
// With rich queries there is no guarantee that the result set hasn't changed between
//  endorsement time and commit time, aka 'phantom reads'.
// Therefore, rich queries should not be used in update transactions, unless the
// application handles the possibility of result set changes between endorsement and commit time.
// Rich queries can be used for point-in-time queries against a peer.
// ============================================================================================

// ===== Example: Ad hoc rich query ========================================================
// queryState uses a query string to perform a query for contracts.
// Query string matching state database syntax is passed in and executed as is.
// Supports ad hoc queries that can be defined at runtime by the client.
// Only available on state databases that support rich query (e.g. CouchDB)
// =========================================================================================
func (t *TradeWorkflowChaincode) queryState(stub shim.ChaincodeStubInterface, creatorOrg string, creatorCertIssuer string, args []string) pb.Response {

	//   0
	// "queryString"

	if len(args) < 1 {
		return shim.Error("Incorrect number of arguments. Expecting 1")
	}

	queryString := args[0]
	resultsIterator, err := stub.GetQueryResult(queryString)
	if err != nil {
		return shim.Error("unable to GetQueryResult ")
	}
	defer resultsIterator.Close()
	queryResults, err := constructQueryResponseFromIterator(resultsIterator)
	if err != nil {
		return shim.Error("unable to construct query Response ")
	}

	fmt.Printf("- getQueryResultForQueryString queryResult:\n%s\n", queryResults.String())

	return shim.Success(queryResults.Bytes())
}

func main() {
	twc := new(TradeWorkflowChaincode)
	twc.testMode = false
	err := shim.Start(twc)
	if err != nil {
		fmt.Printf("Error starting Trade Workflow chaincode: %s", err)
	}
}
