/*a
SPDX-License-Identifier: Apache-2.0
*/

// ====CHAINCODE EXECUTION SAMPLES (CLI) ==================

// ==== Invoke marbles ====
// peer chaincode invoke -C myc1 -n marbles -c '{"Args":["initMarble","marble1","blue","35","tom"]}'
// peer chaincode invoke -C myc1 -n marbles -c '{"Args":["initMarble","marble2","red","50","tom"]}'
// peer chaincode invoke -C myc1 -n marbles -c '{"Args":["initMarble","marble3","blue","70","tom"]}'
// peer chaincode invoke -C myc1 -n marbles -c '{"Args":["transferMarble","marble2","jerry"]}'
// peer chaincode invoke -C myc1 -n marbles -c '{"Args":["transferMarblesBasedOnColor","blue","jerry"]}'
// peer chaincode invoke -C myc1 -n marbles -c '{"Args":["delete","marble1"]}'

// ==== Query marbles ====
// peer chaincode query -C myc1 -n marbles -c '{"Args":["readMarble","marble1"]}'
// peer chaincode query -C myc1 -n marbles -c '{"Args":["getMarblesByRange","marble1","marble3"]}'
// peer chaincode query -C myc1 -n marbles -c '{"Args":["getHistoryForMarble","marble1"]}'

// Rich Query (Only supported if CouchDB is used as state database):
// peer chaincode query -C myc1 -n marbles -c '{"Args":["queryMarblesByOwner","tom"]}'
// peer chaincode query -C myc1 -n marbles -c '{"Args":["queryMarbles","{\"selector\":{\"owner\":\"tom\"}}"]}'

// Rich Query with Pagination (Only supported if CouchDB is used as state database):
// peer chaincode query -C myc1 -n marbles -c '{"Args":["queryMarblesWithPagination","{\"selector\":{\"owner\":\"tom\"}}","3",""]}'

// INDEXES TO SUPPORT COUCHDB RICH QUERIES
//
// Indexes in CouchDB are required in order to make JSON queries efficient and are required for
// any JSON query with a sort. As of Hyperledger Fabric 1.1, indexes may be packaged alongside
// chaincode in a META-INF/statedb/couchdb/indexes directory. Each index must be defined in its own
// text file with extension *.json with the index definition formatted in JSON following the
// CouchDB index JSON syntax as documented at:
// http://docs.couchdb.org/en/2.1.1/api/database/find.html#db-index
//
// This marbles02 example chaincode demonstrates a packaged
// index which you can find in META-INF/statedb/couchdb/indexes/indexOwner.json.
// For deployment of chaincode to production environments, it is recommended
// to define any indexes alongside chaincode so that the chaincode and supporting indexes
// are deployed automatically as a unit, once the chaincode has been installed on a peer and
// instantiated on a channel. See Hyperledger Fabric documentation for more details.
//
// If you have access to the your peer's CouchDB state database in a development environment,
// you may want to iteratively test various indexes in support of your chaincode queries.  You
// can use the CouchDB Fauxton interface or a command line curl utility to create and update
// indexes. Then once you finalize an index, include the index definition alongside your
// chaincode in the META-INF/statedb/couchdb/indexes directory, for packaging and deployment
// to managed environments.
//
// In the examples below you can find index definitions that support marbles02
// chaincode queries, along with the syntax that you can use in development environments
// to create the indexes in the CouchDB Fauxton interface or a curl command line utility.
//

//Example hostname:port configurations to access CouchDB.
//
//To access CouchDB docker container from within another docker container or from vagrant environments:
// http://couchdb:5984/
//
//Inside couchdb docker container
// http://127.0.0.1:5984/

// Index for docType, owner.
//
// Example curl command line to define index in the CouchDB channel_chaincode database
// curl -i -X POST -H "Content-Type: application/json" -d "{\"index\":{\"fields\":[\"docType\",\"owner\"]},\"name\":\"indexOwner\",\"ddoc\":\"indexOwnerDoc\",\"type\":\"json\"}" http://hostname:port/myc1_marbles/_index
//

// Index for docType, owner, size (descending order).
//
// Example curl command line to define index in the CouchDB channel_chaincode database
// curl -i -X POST -H "Content-Type: application/json" -d "{\"index\":{\"fields\":[{\"size\":\"desc\"},{\"docType\":\"desc\"},{\"owner\":\"desc\"}]},\"ddoc\":\"indexSizeSortDoc\", \"name\":\"indexSizeSortDesc\",\"type\":\"json\"}" http://hostname:port/myc1_marbles/_index

// Rich Query with index design doc and index name specified (Only supported if CouchDB is used as state database):
//   peer chaincode query -C myc1 -n marbles -c '{"Args":["queryMarbles","{\"selector\":{\"docType\":\"marble\",\"owner\":\"tom\"}, \"use_index\":[\"_design/indexOwnerDoc\", \"indexOwner\"]}"]}'

// Rich Query with index design doc specified only (Only supported if CouchDB is used as state database):
//   peer chaincode query -C myc1 -n marbles -c '{"Args":["queryMarbles","{\"selector\":{\"docType\":{\"$eq\":\"marble\"},\"owner\":{\"$eq\":\"tom\"},\"size\":{\"$gt\":0}},\"fields\":[\"docType\",\"owner\",\"size\"],\"sort\":[{\"size\":\"desc\"}],\"use_index\":\"_design/indexSizeSortDoc\"}"]}'

package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"reflect"
	"strconv"
	"strings"
	"time"

	"github.com/hyperledger/fabric/core/chaincode/shim"
	pb "github.com/hyperledger/fabric/protos/peer"
)

// Date format
const RFC3339 string = "2006-01-02T15:04:05Z07:00"

// CustomChaincode example simple Chaincode implementation
type CustomChaincode struct {
}

type marble struct {
	ObjectType string `json:"docType"` //docType is used to distinguish the various types of objects in state database
	Name       string `json:"name"`    //the fieldtags are needed to keep case from bouncing around
	Color      string `json:"color"`
	Size       int    `json:"size"`
	Owner      string `json:"owner"`
}

type customer struct {
	CustomerID      string `json:"customerID"`
	CustomerName    string `json:"customerName"`
	CustomerEmail   string `json:"customerEmail"`
	CustomerAddress string `json:"customerAddress"`
	CustomerContact string `json:"customerContact"`
}

type insuranceCompany struct {
	CompanyID      string `json:"companyID"`
	ObjectType     string `json:"docType"` //docType is used to distinguish the various types of objects in state database
	CompanyName    string `json:"companyName"`
	CompanyAddress string `json:"companyAddress"`
	CompanyEmail   string `json:"companyEmail"`
	CompanyContact string `json:"companyContact"`
}

type insuranceContract struct {
	ContractID           string                 `json:"contractID"`
	ObjectType           string                 `json:"docType"` //docType is used to distinguish the various types of objects in state database
	CustomerName         string                 `json:"customerName"`
	InsuranceCompanyName string                 `json:"insuranceCompanyName"`
	InsuredAmount        float64                `json:"insuredAmount"`
	InsuranceCriterias   map[string]interface{} `json:"insuranceCriterias"`
	StartDate            time.Time              `json:"startDate"`
	EndDate              time.Time              `json:"endDate"`
	Location             string                 `json:"location"`
	CurrentStatus        string                 `json:"status"` //ACTIVE, INACTIVE, CLAIMED, UNCLAIM
	IssueDate            time.Time              `json:"issueDate"`
	IssueDateTimestamp   int64                  `json:"issueDateTimestamp"`
	StartDateTimestamp   int64                  `json:"startDateTimestamp"` //These times are epoch times for performing range query operations over datetime
	EndDateTimestamp     int64                  `json:"endDateTimestamp"`
}

type weatherReport struct {
	ReportID          string                 `json:"reportID"`
	ObjectType        string                 `json:"docType"` //docType is used to distinguish the various types of objects in state database
	Location          string                 `json:"location"`
	DateTime          time.Time              `json:"dateTime"`
	WeatherCriterias  map[string]interface{} `json:"weatherCriterias"`
	DateTimeTimestamp int64                  `json:"dateTimeTimestamp"` //This time is epoch time
}

// ===================================================================================
// Main
// ===================================================================================
func main() {
	err := shim.Start(new(CustomChaincode))
	if err != nil {
		fmt.Printf("Error starting Simple chaincode: %s", err)
	}

}

// Init initializes chaincode
// ===========================
func (t *CustomChaincode) Init(stub shim.ChaincodeStubInterface) pb.Response {

	return shim.Success(nil)
}

// Invoke - Our entry point for Invocations
// ========================================
func (t *CustomChaincode) Invoke(stub shim.ChaincodeStubInterface) pb.Response {
	function, args := stub.GetFunctionAndParameters()
	fmt.Println("invoke is running " + function)

	if function == "createCustomer" { //create a new customer entity
		return t.createCustomer(stub, args)
	} else if function == "generateWeatherReport" { //generate a weather report
		return t.generateWeatherReport(stub, args)
	} else if function == "createInsuranceCompany" { //create a new insurance company entity
		return t.createInsuranceCompany(stub, args)
	} else if function == "registerInsuranceContract" { //register a new insurance contract
		return t.registerInsuranceContract(stub, args)
	} else if function == "queryState" { //generic method to perform rich query
		return t.queryState(stub, args)
	} else if function == "getWeatherDetailsByLocation" { //Returns weather details of a location
		return t.getWeatherDetailsByLocation(stub, args)
	} else if function == "schedulerJob" { //schedulerJob updates the status of the contract
		return t.schedulerJob(stub, args)
	} else if function == "getHistoryForContract" { //find history details of a contract
		return t.getHistoryForContract(stub, args)
	}

	fmt.Println("invoke did not find func: " + function) //error
	return shim.Error("Received unknown function invocation")
}

// ============================================================
// createCustomer - create a new customer, store into chaincode state
// ============================================================
func (t *CustomChaincode) createCustomer(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	jsonString := []byte(args[0])

	var customerObject map[string]interface{}
	err := json.Unmarshal(jsonString, &customerObject)
	if err != nil {
		panic(err)
	}
	fmt.Println(customerObject)

	var customerID string //auto generated from backend
	customerName := customerObject["customerName"].(string)
	customerEmail := customerObject["customerEmail"].(string)
	customerAddress := customerObject["customerAddress"].(string)
	customerContact := customerObject["customerContact"].(string)
	//customerID is a combination of customerName & customerContact
	strippedCustomerName := strings.Replace(customerName, " ", "", 10)
	currentTime := time.Now().UTC()
	customerID = "CUST-" + strings.ToUpper(strippedCustomerName[0:3]) + "-" + currentTime.Format("20060102150405")
	fmt.Println("customerID : ", customerID)

	customer := &customer{customerID, customerName, customerEmail, customerAddress, customerContact}

	customerObjectJSONasBytes, err := json.Marshal(customer)
	if err != nil {
		return shim.Error(err.Error())
	}
	// === Save customer to state ===

	err = stub.PutState(customerID, customerObjectJSONasBytes)
	if err != nil {
		return shim.Error(err.Error())
	}

	return shim.Success(nil)
}

// =====================================================================================================
// generate a weather report
// Note: We need to identify how a contract is triggered based on location & customer info
// waetherObject coming from the node client should look like below:
// weatherObject = {"location":"Bangalore","weatherCriterias":{"rain":"10","snow":"false","temp":"32.00"}}
// Note - we may have to handle fix set of weather criterias as it is very difficult to get the
// dynamic data type for an object passed from the weathercriteria
// ======================================================================================================
func (t *CustomChaincode) generateWeatherReport(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	jsonString := []byte(args[0]) //data coming from the IOT device or WeatherReport apis

	fmt.Println("weather report object ", jsonString)

	var weatherObject map[string]interface{}
	if err := json.Unmarshal(jsonString, &weatherObject); err != nil {
		panic(err)
	}
	fmt.Println(weatherObject)

	objectType := "weatherObject" //This field will be useful to get all the weather reports for a particular location
	location := weatherObject["location"].(string)
	// RFC3339 := "2006-01-02T15:04:05Z07:00"
	dateObject := weatherObject["dateTime"].(string)
	dateTime, e1 := time.Parse(RFC3339, dateObject)

	if e1 != nil {
		fmt.Println("Error caught while parsing startDate ", e1)
		return shim.Error("Error caught while parsing startDate " + e1.Error())
	}
	dateTimeTimestamp := makeTimestampInMS(dateTime)
	fmt.Println("dateTimeTimestamp ", dateTimeTimestamp)
	reportID := "WEATH-" + strings.ToUpper(location[0:3]) + "-" + dateTime.Format("20060102150405")
	fmt.Println("reportID : ", reportID)
	fmt.Println("test1")
	weatherCriterias := weatherObject["weatherCriterias"].(map[string]interface{})
	fmt.Println("test2")

	weatherReport := &weatherReport{reportID, objectType, location, dateTime, weatherCriterias, dateTimeTimestamp}
	weatherReportJSONasBytes, err := json.Marshal(weatherReport)
	if err != nil {
		return shim.Error(err.Error())
	}
	// === Save weather report to state ===

	err = stub.PutState(reportID, weatherReportJSONasBytes)
	if err != nil {
		return shim.Error(err.Error())
	}

	//iterate through all the active contracts from a location
	//check the weather criteria for each active criteria
	//if the conditions exceed threshold then auto trigger the contract for a specific customer
	queryString := fmt.Sprintf("{\"selector\":{\"docType\":\"contract\",\"location\":\"%s\",\"status\":\"ACTIVE\"}}", location)

	fmt.Println("queryString ", queryString)

	resultsIterator, err := stub.GetQueryResult(queryString)
	if err != nil {
		return shim.Error(err.Error())
	}
	defer resultsIterator.Close()
	fmt.Println("test3")

	for resultsIterator.HasNext() {
		fmt.Println("test4")
		queryResponse, err := resultsIterator.Next()
		if err != nil {
			return shim.Error(err.Error())
		}

		fmt.Println("test5")
		//Key - contract id
		fmt.Println("key", queryResponse.Key)
		//Value - contract agreement doc
		fmt.Println("value", string(queryResponse.Value))

		insuranceContract := insuranceContract{}
		queryResponseAsBytes := queryResponse.Value
		if err := json.Unmarshal(queryResponseAsBytes, &insuranceContract); err != nil {
			panic(err)
		}
		fmt.Println("retrieved insuranceContract ", insuranceContract)

		//maintaining a map variable
		var msg string
		criteriaMap := insuranceContract.InsuranceCriterias
		fmt.Println("criteriaMap ", criteriaMap)
		//The datatype of the values for the criteriaMap will be either a string or a boolean

		triggerFlag := false

		for key, value := range criteriaMap {
			fmt.Println("key ", key)
			fmt.Println("value type ", reflect.TypeOf(value))

			switch t := value.(type) {
			// case int:
			case string:
				fmt.Printf("String : %v\n", t)
				fmt.Println("key weather ", weatherCriterias[key])
				if weatherCriterias[key] != nil {
					weatherCriteriaVal := weatherCriterias[key].(float64)
					stringValue := value.(string)
					parsedValArray := strings.Split(stringValue, "-")

					fmt.Printf("Array: %v\n", parsedValArray)
					fmt.Printf("Array: %v\n", parsedValArray[0])
					fmt.Printf("Array: %v\n", parsedValArray[1])
					parsedValFloat1, _ := strconv.ParseFloat(parsedValArray[0], 2)
					parsedValFloat2, _ := strconv.ParseFloat(parsedValArray[1], 2)
					if weatherCriteriaVal < parsedValFloat1 || weatherCriteriaVal > parsedValFloat2 {
						triggerFlag = true
					}
				}

			case float64:
				fmt.Printf("Float64: %v\n", t)
				weatherCriteriaVal := weatherCriterias[key].(float64)

				parsedVal := value.(float64)

				if weatherCriteriaVal > parsedVal {
					triggerFlag = true
				}

			// case string:
			// 	fmt.Printf("String: %v\n", t)
			case bool:
				fmt.Printf("Bool: %v\n", t)
				if weatherCriterias[key] != nil {
					weatherCriteriaVal := weatherCriterias[key].(bool)
					parsedVal := value.(bool)

					if weatherCriteriaVal == parsedVal {
						triggerFlag = true
					}
				}
			default:
				var r = reflect.TypeOf(t)
				fmt.Printf("Other:%v\n", r)
				fmt.Printf("Different dataType found")
			}
		}
		if triggerFlag {
			fmt.Println("Trigger criteria matched, update status of the contract")
			msg, err = triggerInsuranceContract(stub, insuranceContract)
			if err != nil {
				return shim.Error(err.Error())
			}
			fmt.Println(msg)
		} else {
			fmt.Println("triggerFlag found to be false")
		}
	}

	return shim.Success(nil)
}

// ============================================================
// create a new insurance company entity
// ============================================================
func (t *CustomChaincode) createInsuranceCompany(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	jsonString := []byte(args[0]) //data coming from the IOT device or WeatherReport apis
	var insuranceCompanyInterface map[string]interface{}
	if err := json.Unmarshal(jsonString, &insuranceCompanyInterface); err != nil {
		panic(err)
	}
	fmt.Println(insuranceCompanyInterface)

	var companyID string //auto generated companyID
	objectType := "insurancecompany"
	companyName := insuranceCompanyInterface["companyName"].(string)
	companyAddress := insuranceCompanyInterface["companyAddress"].(string)
	companyEmail := insuranceCompanyInterface["companyEmail"].(string)
	companyContact := insuranceCompanyInterface["companyContact"].(string)
	strippedCompanyName := strings.Replace(companyName, " ", "", 10)
	currentTime := time.Now().UTC()
	companyID = "COMP-" + strings.ToUpper(strippedCompanyName[0:3]) + "-" + currentTime.Format("20060102150405")
	fmt.Println("companyID : ", companyID)

	insuranceCompany := &insuranceCompany{companyID, objectType, companyName, companyAddress, companyEmail, companyContact}
	insuranceCompanyJSONasBytes, err := json.Marshal(insuranceCompany)
	if err != nil {
		return shim.Error(err.Error())
	}
	// === Save company to state ===
	err = stub.PutState(companyID, insuranceCompanyJSONasBytes)
	if err != nil {
		return shim.Error(err.Error())
	}

	return shim.Success(nil)
}

func (t *CustomChaincode) registerInsuranceContract(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	jsonString := []byte(args[0]) //data coming from the IOT device or WeatherReport apis
	// RFC3339 := "2006-01-02T15:04:05Z07:00"

	fmt.Println("insurance contract object ", jsonString)

	var insuranceObject map[string]interface{}
	if err := json.Unmarshal(jsonString, &insuranceObject); err != nil {
		panic(err)
	}
	fmt.Println("insuranceObject ", insuranceObject)

	customerName := insuranceObject["customerName"].(string)
	fmt.Println("customerName ", customerName)
	//get customerObject based on customerName
	// customerObjectBytes, err := stub.GetState(customerName)
	// if err != nil {
	// 	return shim.Error("Failed to get customer ")
	// }

	fmt.Println("first pass")

	/*customerObject := customer{}
	err = json.Unmarshal(customerObjectBytes, &customerObject) //unmarshal it aka JSON.parse()
	if err != nil {
		return shim.Error(err.Error())
	} */

	fmt.Println("second pass")

	companyName := insuranceObject["companyName"].(string)
	//get insuranceCompany based on companyName
	// insuranceCompanyAsBytes, err := stub.GetState(companyName)
	// if err != nil {
	// 	return shim.Error("Failed to get customer ")
	// }

	fmt.Println("third pass")

	// insuranceCompanyObject := insuranceCompany{}
	// err = json.Unmarshal(insuranceCompanyAsBytes, &insuranceCompanyObject) //unmarshal it aka JSON.parse()
	// if err != nil {
	// 	return shim.Error(err.Error())
	// }

	fmt.Println("forth pass")

	insuredAmount, err := strconv.ParseFloat(insuranceObject["insuredAmount"].(string), 64)
	if err != nil {
		return shim.Error(err.Error())
	}
	fmt.Println("insuredAmount ", insuredAmount)
	insuranceCriterias := insuranceObject["insuranceCriterias"].(map[string]interface{})
	fmt.Println("insuranceCriterias ", insuranceCriterias)
	//Expected date string from UI should be in format: 2019-01-29T06:59:23Z
	//This date string is RFC3339 golang time format string
	//Parse the date string
	startDate, e1 := time.Parse(RFC3339, insuranceObject["startDate"].(string))
	if e1 != nil {
		fmt.Println("Error caught while parsing startDate ", e1)
		return shim.Error("Error caught while parsing startDate " + e1.Error())
	}
	fmt.Println("startDate ", startDate)
	endDate, e2 := time.Parse(RFC3339, insuranceObject["endDate"].(string))
	if e2 != nil {
		fmt.Println("Error caught while parsing endDate ", e2)
		return shim.Error("Error caught while parsing endDate " + e2.Error())
	}
	fmt.Println("endDate ", endDate)
	location := insuranceObject["location"].(string)
	fmt.Println("location ", location)
	currentStatus := insuranceObject["status"].(string)
	fmt.Println("currentStatus ", currentStatus)
	issueDate := time.Now().UTC()
	fmt.Println("issueDate ", issueDate)
	issueDateTimestamp := makeTimestampInMS(issueDate)
	fmt.Println("issueDateTimestamp ", issueDateTimestamp)

	startDateTimestamp := makeTimestampInMS(startDate)
	fmt.Println("startDateTimestamp ", startDateTimestamp)
	endDateTimestamp := makeTimestampInMS(endDate)
	fmt.Println("endDateTimestamp ", endDateTimestamp)

	var contractID string
	objectType := "contract"
	strippedcustomerName := strings.Replace(customerName, " ", "", 10)
	strippedCompanyName := strings.Replace(companyName, " ", "", 10)
	currentTime := time.Now().UTC()
	contractID = "CONT-" + strings.ToUpper(strippedcustomerName[0:3]) + "-" + strings.ToUpper(strippedCompanyName[0:3]) + "-" + currentTime.Format("20060102150405")
	insuranceContract := &insuranceContract{contractID, objectType, customerName, companyName, insuredAmount, insuranceCriterias, startDate, endDate, location, currentStatus, issueDate, issueDateTimestamp, startDateTimestamp, endDateTimestamp}
	contractJSONasBytes, err := json.Marshal(insuranceContract)
	if err != nil {
		return shim.Error(err.Error())
	}

	// === Save contract to state ===
	err = stub.PutState(contractID, contractJSONasBytes)
	if err != nil {
		return shim.Error(err.Error())
	}

	//change to array of bytes
	contractIDAsBytes, _ := json.Marshal(contractID)
	fmt.Println("contractIDAsBytes ", contractIDAsBytes)
	return shim.Success(contractIDAsBytes)
}

// Return keys of the given map
/*Orphan function - useful for future
func Keys(m map[string]interface{}) (keys []string) {
    for k := range m {
        keys = append(keys, k)
    }
    return keys
}*/

// ============================================================
// insurance claim will be auto triggered on a set of criterias
// ============================================================
func triggerInsuranceContract(stub shim.ChaincodeStubInterface, insuranceContract insuranceContract) (string, error) {
	var err error

	status := "CLAIMED"

	err = updateInsuranceContract(stub, insuranceContract, status)
	var msg string
	if err != nil {
		msg = "Could not update the contract"
		return msg, err
	}
	msg = "Successfully updated the contract " + insuranceContract.ContractID
	return msg, nil
}

//Update status of the insurance contract
func updateInsuranceContract(stub shim.ChaincodeStubInterface, insuranceContract insuranceContract, status string) error {
	var err error
	updatedInsuranceContract := insuranceContract
	updatedInsuranceContract.CurrentStatus = status
	issueDate := time.Now().UTC()
	updatedInsuranceContract.IssueDate = issueDate
	updatedInsuranceContract.IssueDateTimestamp = makeTimestampInMS(issueDate)

	updatedInsuranceContractJSONasBytes, _ := json.Marshal(updatedInsuranceContract)
	err = stub.PutState(updatedInsuranceContract.ContractID, updatedInsuranceContractJSONasBytes) //update the contract status
	if err != nil {
		return err
	}
	return nil
}

//set today's contracts as active
func setContractsAsActive(stub shim.ChaincodeStubInterface) (string, error) {
	var err error

	currentTime := time.Now().UTC()
	// fmt.Println(currentTime.Format(time.RFC822))

	fmt.Println(currentTime)
	currentTimestamp := makeTimestampInMS(currentTime)
	fmt.Println("currentTimestamp ", currentTimestamp)

	//This query will give all the contract which fall under the startDate & endDate

	//queryString := "{\"selector\":{\"docType\":\"contract\",\"status\":\"INACTIVE\",\"startDate\":{\"$lt\": " + currentTime + " }, \"endDate\":{\"$gt\":" + currentTime + "}}"
	queryString := fmt.Sprintf("{\"selector\":{\"docType\":\"contract\",\"status\":\"INACTIVE\",\"startDateTimestamp\":{\"$lt\": %d }, \"endDateTimestamp\":{\"$gt\":%d }}}", currentTimestamp, currentTimestamp)
	fmt.Println("queryString in setContractsAsActive ", queryString)

	resultsIterator, err := stub.GetQueryResult(queryString)
	var msg string
	if err != nil {
		msg = "Error performing the query operation"
		return msg, err
	}
	defer resultsIterator.Close()
	fmt.Println("Pass 1")

	for resultsIterator.HasNext() {
		queryResponse, err := resultsIterator.Next()
		if err != nil {
			msg = "Caught error while getting the queryResponse"
			return msg, err
		}

		//Key - contract id
		fmt.Println("key" + queryResponse.Key)
		//Value - contract agreement doc
		fmt.Println("value" + string(queryResponse.Value))

		insuranceContract := insuranceContract{}
		if err := json.Unmarshal(queryResponse.Value, &insuranceContract); err != nil {
			panic(err)
		}
		fmt.Println(insuranceContract)

		//Check if the currentStatus flag is alreadt set to ACTIVE, skip such contracts
		if insuranceContract.CurrentStatus != "ACTIVE" {
			status := "ACTIVE"

			err = updateInsuranceContract(stub, insuranceContract, status)
			var msg string
			if err != nil {
				msg = "Could not update the contract"
				return msg, err
			}
			msg = "Successfully updated the contract " + insuranceContract.ContractID
			return msg, nil
		}
	}
	msg = "Successfully updated status of all the contracts for the day"
	return msg, nil
}

//set contracts as unclaimed
func setContractsAsUnclaimed(stub shim.ChaincodeStubInterface) (string, error) {
	var err error

	currentTime := time.Now().UTC()
	// fmt.Println(currentTime.Format(time.RFC822))
	fmt.Println(currentTime)
	currentTimestamp := makeTimestampInMS(currentTime)
	fmt.Println("currentTimestamp ", currentTimestamp)

	//This query will give all the contract which fall under the startDate & endDate

	//queryString := "{\"selector\":{\"docType\":\"contract\",\"status\":\"INACTIVE\",\"startDate\":{\"$lt\": " + currentTime + " }, \"endDate\":{\"$gt\":" + currentTime + "}}"
	queryString := fmt.Sprintf("{\"selector\":{\"docType\":\"contract\",\"status\":\"ACTIVE\",\"endDateTimestamp\":{\"$lt\":%d}}}", currentTimestamp)
	fmt.Println("queryString in setContractsAsUnclaimed ", queryString)

	resultsIterator, err := stub.GetQueryResult(queryString)
	var msg string
	if err != nil {
		msg = "Error performing the query operation"
		return msg, err
	}
	defer resultsIterator.Close()

	fmt.Println("Pass 11")

	for resultsIterator.HasNext() {
		queryResponse, err := resultsIterator.Next()
		if err != nil {
			msg = "Caught error while getting the queryResponse"
			return msg, err
		}

		//Key - contract id
		fmt.Println("key" + queryResponse.Key)
		//Value - contract agreement doc
		fmt.Println("value" + string(queryResponse.Value))

		insuranceContract := insuranceContract{}
		if err := json.Unmarshal(queryResponse.Value, &insuranceContract); err != nil {
			panic(err)
		}
		fmt.Println(insuranceContract)

		//Check if the currentStatus flag is already set to CLAIMED, skip such contracts
		if insuranceContract.CurrentStatus != "CLAIMED" {
			status := "UNCLAIMED"

			err = updateInsuranceContract(stub, insuranceContract, status)
			var msg string
			if err != nil {
				msg = "Could not update the contract"
				return msg, err
			}
			msg = "Successfully updated the contract " + insuranceContract.ContractID
			return msg, nil
		}
	}
	msg = "Successfully updated status of all the contracts for the day"
	return msg, nil
}

// ===========================================================================================
// constructQueryResponseFromIterator constructs a JSON array containing query results from
// a given result iterator
// ===========================================================================================
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
func (t *CustomChaincode) queryState(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	//   0
	// "queryString"
	if len(args) < 1 {
		return shim.Error("Incorrect number of arguments. Expecting 1")
	}

	queryString := args[0]

	queryResults, err := getQueryResultForQueryString(stub, queryString)
	if err != nil {
		return shim.Error(err.Error())
	}
	return shim.Success(queryResults)
}

// =========================================================================================
// getQueryResultForQueryString executes the passed in query string.
// Result set is built and returned as a byte array containing the JSON results.
// =========================================================================================
func getQueryResultForQueryString(stub shim.ChaincodeStubInterface, queryString string) ([]byte, error) {

	fmt.Printf("- getQueryResultForQueryString queryString:\n%s\n", queryString)

	resultsIterator, err := stub.GetQueryResult(queryString)
	if err != nil {
		return nil, err
	}
	defer resultsIterator.Close()

	buffer, err := constructQueryResponseFromIterator(resultsIterator)
	if err != nil {
		return nil, err
	}

	fmt.Printf("- getQueryResultForQueryString queryResult:\n%s\n", buffer.String())

	return buffer.Bytes(), nil
}

/** get weather details based on location, startDate & endDate for a contract
jsonString : {"location":"Bangalore","startDate":"xxxxxxx","endDate":"xxxxxx"}
*/
func (t *CustomChaincode) getWeatherDetailsByLocation(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	//   0
	// "queryString"
	if len(args) < 1 {
		return shim.Error("Incorrect number of arguments. Expecting 1")
	}

	jsonString := []byte(args[0])

	var weatherRequestObject map[string]interface{}
	if err := json.Unmarshal(jsonString, &weatherRequestObject); err != nil {
		panic(err)
	}
	fmt.Println(weatherRequestObject)

	location := weatherRequestObject["location"].(string)

	startDate, e1 := time.Parse(RFC3339, weatherRequestObject["startDate"].(string))
	if e1 != nil {
		fmt.Println("Error caught while parsing startDate ", e1)
		return shim.Error("Error caught while parsing startDate " + e1.Error())
	}
	startDateTimestamp := makeTimestampInMS(startDate)
	endDate, e2 := time.Parse(RFC3339, weatherRequestObject["endDate"].(string))
	if e2 != nil {
		fmt.Println("Error caught while parsing endDate ", e2)
		return shim.Error("Error caught while parsing endDate " + e2.Error())
	}
	endDateTimestamp := makeTimestampInMS(endDate)

	queryString := fmt.Sprintf("{\"selector\":{\"docType\":\"weatherObject\",\"location\":\"%s\",\"dateTimeTimestamp\":{\"$lt\": %d, \"$gt\": %d}}}", location, endDateTimestamp, startDateTimestamp)
	fmt.Println("queryString in getWeatherDetailsByLocation ", queryString)

	queryResults, err := getQueryResultForQueryString(stub, queryString)
	if err != nil {
		return shim.Error(err.Error())
	}
	return shim.Success(queryResults)
}

func (t *CustomChaincode) schedulerJob(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	fmt.Println("schedulerJob reached")

	msg1, err1 := setContractsAsActive(stub)
	if err1 != nil {
		fmt.Println("Error found in setContractsAsActive func ", err1.Error())
		return shim.Error(err1.Error())
	}
	fmt.Println("msg1 ", msg1)
	msg2, err2 := setContractsAsUnclaimed(stub)
	if err2 != nil {
		fmt.Println("Error found in setContractsAsUnclaimed func ", err2.Error())
		return shim.Error(err2.Error())
	}
	fmt.Println("msg2 ", msg2)

	return shim.Success(nil)
}

/**	convert time to milliseconds
Reference: https://gobyexample.com/epoch
*/
func makeTimestampInMS(time time.Time) int64 {
	return time.UnixNano() / 1000000
}

/**
Below method returns the contract history based on the contractIDs
*/
func (t *CustomChaincode) getHistoryForContract(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	if len(args) < 1 {
		return shim.Error("Incorrect number of arguments. Expecting 1")
	}

	contractID := args[0]

	fmt.Printf("- start getHistoryForContract: %s\n", contractID)

	resultsIterator, err := stub.GetHistoryForKey(contractID)
	if err != nil {
		return shim.Error(err.Error())
	}
	defer resultsIterator.Close()

	// buffer is a JSON array containing historic values for the marble
	var buffer bytes.Buffer
	buffer.WriteString("[")

	bArrayMemberAlreadyWritten := false
	for resultsIterator.HasNext() {
		response, err := resultsIterator.Next()
		if err != nil {
			return shim.Error(err.Error())
		}
		// Add a comma before array members, suppress it for the first array member
		if bArrayMemberAlreadyWritten == true {
			buffer.WriteString(",")
		}
		buffer.WriteString("{\"TxId\":")
		buffer.WriteString("\"")
		buffer.WriteString(response.TxId)
		buffer.WriteString("\"")

		buffer.WriteString(", \"Value\":")
		// if it was a delete operation on given key, then we need to set the
		//corresponding value null. Else, we will write the response.Value
		//as-is (as the Value itself a JSON object)
		if response.IsDelete {
			buffer.WriteString("null")
		} else {
			buffer.WriteString(string(response.Value))
		}

		buffer.WriteString(", \"Timestamp\":")
		buffer.WriteString("\"")
		buffer.WriteString(time.Unix(response.Timestamp.Seconds, int64(response.Timestamp.Nanos)).String())
		buffer.WriteString("\"")

		buffer.WriteString("}")
		bArrayMemberAlreadyWritten = true
	}
	buffer.WriteString("]")

	fmt.Printf("- getHistoryForContract returning:\n%s\n", buffer.String())

	return shim.Success(buffer.Bytes())
}
