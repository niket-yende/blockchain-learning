package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"strconv"
	"time"

	"github.com/hyperledger/fabric/core/chaincode/shim"
	pb "github.com/hyperledger/fabric/protos/peer"
)

// Date format
const RFC3339 string = "2006-01-02T15:04:05Z07:00"

// SupplyChaincode example simple Chaincode implementation
type SupplyChaincode struct {
}

type actor struct {
	ObjectType string `json:"docType"`
	ID         string `json:"id"`
	Type       string `json:"type"`
	Location   string `json:"location"`
}

type shipment struct {
	ObjectType string `json:"docType"` //docType is used to distinguish the various types of objects in state database
	ShipmentID string `json:"shipmentID"`
	SellerID   string `json:"sellerID"`
	BuyerID    string `json:"buyerID"`
	LGPID      string `json:"lgpID"` //Logistics provider id
	TempBreach string `json:"tempBreach"`
	Status     string `json:"status"` //In-Store or In-transit or Delivered or Accepted /Rejected by buyer
}

type timeRasterDetail struct {
	ObjectType          string  `json:"docType"` //docType is used to distinguish the various types of objects in state database
	ShipmentID          string  `json:"shipmentID"`
	Date                string  `json:"date"`
	TimeRaster          string  `json:"timeRaster"`
	TimeRasterTimestamp int64   `json:"timeRasterTimestamp"`
	Temperature         float64 `json:"temperature"`
}

type sellerView struct {
	ShipmentID     string `json:"shipmentID"`
	SellerLocation string `json:"sellerLocation"`
	Status         string `json:"status"`
	BuyerID        string `json:"buyerID"`
	BuyerLocation  string `json:"buyerLocation"`
	TempBreach     string `json:"tempBreach"`
}

type logisticsView struct {
	ShipmentID     string `json:"shipmentID"`
	SellerLocation string `json:"sellerLocation"`
	Status         string `json:"status"`
	LogisticsID    string `json:"logisticsID"`
	BuyerID        string `json:"buyerID"`
	BuyerLocation  string `json:"buyerLocation"`
	TempBreach     string `json:"tempBreach"`
}

type buyerView struct {
	ShipmentID     string `json:"shipmentID"`
	SellerLocation string `json:"sellerLocation"`
	SellerID       string `json:"sellerID"`
	LogisticsID    string `json:"logisticsID"`
	BuyerLocation  string `json:"buyerLocation"`
	TempBreach     string `json:"tempBreach"`
}

// ===================================================================================
// Main
// ===================================================================================
func main() {
	err := shim.Start(new(SupplyChaincode))
	if err != nil {
		fmt.Printf("Error starting Simple chaincode: %s", err)
	}

}

// Init initializes chaincode
// ===========================
func (t *SupplyChaincode) Init(stub shim.ChaincodeStubInterface) pb.Response {

	return shim.Success(nil)
}

// Invoke - Our entry point for Invocations
// ========================================
func (t *SupplyChaincode) Invoke(stub shim.ChaincodeStubInterface) pb.Response {
	function, args := stub.GetFunctionAndParameters()
	fmt.Println("invoke is running " + function)

	if function == "createShipment" { //create a new Shipment entity
		return t.createShipment(stub, args)
	} else if function == "createActor" { //generate TimeRaster Report
		return t.createActor(stub, args)
	} else if function == "updateShipmentStatus" { //updates the ShipmentStatus
		return t.updateShipmentStatus(stub, args)
	} else if function == "addTimeRasterDetail" { //add TimeRasterDetails periodically
		return t.addTimeRasterDetail(stub, args)
	} else if function == "generateSellerView" { //generate seller view
		return t.generateSellerView(stub, args)
	} else if function == "generateLogisticsView" { //generate seller view
		return t.generateLogisticsView(stub, args)
	} else if function == "generateBuyerView" { //generate seller view
		return t.generateBuyerView(stub, args)
	} else if function == "queryState" { //generic method to perform rich query
		return t.queryState(stub, args)
	} else if function == "getHistoryForShipment" { //find history details of a shipment
		return t.getHistoryForShipment(stub, args)
	}

	fmt.Println("invoke did not find func: " + function) //error
	return shim.Error("Received unknown function invocation")
}

// ============================================================
// createShipment - create a new shipment, store into chaincode state
// ============================================================
func (t *SupplyChaincode) createShipment(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	jsonString := []byte(args[0])

	var shipmentInterface map[string]interface{}
	err := json.Unmarshal(jsonString, &shipmentInterface)
	if err != nil {
		panic(err)
	}
	fmt.Println(shipmentInterface)

	docType := "shipment"
	shipmentID := shipmentInterface["shipmentID"].(string)
	sellerID := shipmentInterface["sellerID"].(string)
	buyerID := shipmentInterface["buyerID"].(string)
	lgpID := shipmentInterface["lgpID"].(string)
	tempBreach := "NO"
	status := "In-Store"

	fmt.Println("shipmentID : ", shipmentID)

	shipment := &shipment{docType, shipmentID, sellerID, buyerID, lgpID, tempBreach, status}

	shipmentObjectJSONasBytes, err := json.Marshal(shipment)
	if err != nil {
		return shim.Error(err.Error())
	}
	// === Save shipment to state ===

	err = stub.PutState(shipmentID, shipmentObjectJSONasBytes)
	if err != nil {
		return shim.Error(err.Error())
	}

	shipmentIDAsBytes, _ := json.Marshal(shipmentID)
	fmt.Println("shipmentIDAsBytes ", shipmentIDAsBytes)
	return shim.Success(shipmentIDAsBytes)
}

// ============================================================
// createActor - create a new actor, store into chaincode state
// ============================================================
func (t *SupplyChaincode) createActor(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	jsonString := []byte(args[0])

	var actorInterface map[string]interface{}
	err := json.Unmarshal(jsonString, &actorInterface)
	if err != nil {
		panic(err)
	}
	fmt.Println(actorInterface)

	docType := "actor"
	actorID := actorInterface["id"].(string)
	actortype := actorInterface["type"].(string)
	location := actorInterface["location"].(string)

	actorObject := &actor{docType, actorID, actortype, location}

	actorObjectJSONasBytes, err := json.Marshal(actorObject)
	if err != nil {
		return shim.Error(err.Error())
	}
	// === Save shipment to state ===

	err = stub.PutState(actorID, actorObjectJSONasBytes)
	if err != nil {
		return shim.Error(err.Error())
	}

	actorIDAsBytes, _ := json.Marshal(actorID)
	fmt.Println("actorIDAsBytes ", actorIDAsBytes)
	return shim.Success(actorIDAsBytes)
}

// ============================================================
// update Shipment Status
// ============================================================
func (t *SupplyChaincode) updateShipmentStatus(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	jsonString := []byte(args[0]) //data coming from the IOT device or WeatherReport apis
	var shipmentStatusInterface map[string]interface{}
	if err := json.Unmarshal(jsonString, &shipmentStatusInterface); err != nil {
		panic(err)
	}
	fmt.Println(shipmentStatusInterface)

	shipmentID := shipmentStatusInterface["shipmentID"].(string)
	updatedStatus := shipmentStatusInterface["updatedStatus"].(string)

	queryString := fmt.Sprintf("{\"selector\":{\"docType\":\"shipment\",\"shipmentID\":\"%s\",\"status\":{\"$ne\":\"%s\"}}}", shipmentID, updatedStatus)
	fmt.Println("queryString in updateShipmentStatus ", queryString)

	resultsIterator, err := stub.GetQueryResult(queryString)

	fmt.Println("HasNext ", resultsIterator.HasNext())
	var msg string
	if err != nil {
		msg = "Error performing the query operation"
		fmt.Println(msg)
		return shim.Error(err.Error())
	}
	defer resultsIterator.Close()

	for resultsIterator.HasNext() {
		queryResponse, err := resultsIterator.Next()
		if err != nil {
			msg = "Caught error while getting the queryResponse"
			fmt.Println(msg)
			return shim.Error(err.Error())
		}

		//Key - shipmentID
		fmt.Println("key" + queryResponse.Key)
		//Value - contract shipment doc
		fmt.Println("value" + string(queryResponse.Value))

		updatedShipment := shipment{}
		if err := json.Unmarshal(queryResponse.Value, &updatedShipment); err != nil {
			panic(err)
		}
		fmt.Println(updatedShipment)

		updateType := "statusUpdate"
		err = updateShipmentRecord(stub, updatedShipment, updatedStatus, 0, updateType)

		if err != nil {
			msg = "Could not update the Shipment"
			fmt.Println(msg)
			return shim.Error(err.Error())
		}
		msg = "Successfully updated the Shipment " + updatedShipment.ShipmentID
		msgAsBytes, _ := json.Marshal(msg)
		return shim.Success(msgAsBytes)
	}
	msg = "Current status of the Shipment is already in an updated state"
	msgAsBytes, _ := json.Marshal(msg)
	return shim.Success(msgAsBytes)
}

//this method should check for previous data to mark the tempBreach field in shipment
func (t *SupplyChaincode) addTimeRasterDetail(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	jsonString := []byte(args[0]) //timeraster data coming from the IOT device or WeatherReport apis
	// RFC3339 := "2006-01-02T15:04:05Z07:00"

	fmt.Println("addTimeRasterDetail object ", jsonString)

	var timeRasterInterface map[string]interface{}
	if err := json.Unmarshal(jsonString, &timeRasterInterface); err != nil {
		panic(err)
	}
	fmt.Println("timeRasterInterface ", timeRasterInterface)

	docType := "timeRaster"
	currentDate := time.Now().UTC()
	const dateLayout = "02-Jan-2006"
	formattedCurrentDate := currentDate.Format(dateLayout)

	fmt.Println(formattedCurrentDate)
	currentDateTimeStamp := makeTimestampInMS(currentDate)

	const timeLayout = "15:04:05"
	formattedCurrentTimeRaster := currentDate.Format(timeLayout)

	fmt.Println(formattedCurrentTimeRaster)

	shipmentID := timeRasterInterface["shipmentID"].(string)
	fmt.Println("shipmentID ", shipmentID)

	temperature, err := strconv.ParseFloat(timeRasterInterface["temperature"].(string), 64)
	if err != nil {
		return shim.Error(err.Error())
	}
	fmt.Println("temperature ", temperature)

	timeRasterDetailObject := &timeRasterDetail{docType, shipmentID, formattedCurrentDate, formattedCurrentTimeRaster, currentDateTimeStamp, temperature}
	timeRasterJSONasBytes, err := json.Marshal(timeRasterDetailObject)
	if err != nil {
		return shim.Error(err.Error())
	}

	timeRasterDocId := "TIMERASTER-" + shipmentID + "-" + currentDate.Format("20060102150405")

	// === Save timeRaster to state ===
	err = stub.PutState(timeRasterDocId, timeRasterJSONasBytes)
	if err != nil {
		return shim.Error(err.Error())
	}

	elapsedTime := currentDate.Add(-30 * time.Minute)

	elapsedTimeStamp := makeTimestampInMS(elapsedTime)
	fmt.Println(elapsedTimeStamp)

	//Query the last half an hour details
	queryString := fmt.Sprintf("{\"selector\":{\"docType\":\"timeRaster\",\"shipmentID\":\"%s\",\"timeRasterTimestamp\":{\"$lte\": %d, \"$gte\": %d}}}", shipmentID, currentDateTimeStamp, elapsedTimeStamp)
	fmt.Println("queryString in getWeatherDetailsByLocation ", queryString)

	var sumTemp = 0.0
	var avgTemp = 0.0
	var counter = 0.0
	var samplingPoints = 5.0 //this defines the considered sample points for 30 mins

	resultsIterator, err := stub.GetQueryResult(queryString)

	var msg string
	if err != nil {
		msg = "Error performing the query operation"
		fmt.Println(msg)
		return shim.Error(err.Error())
	}
	defer resultsIterator.Close()
	fmt.Println("Pass 1")

	for resultsIterator.HasNext() {
		queryResponse, err := resultsIterator.Next()
		fmt.Println("Pass 2")
		if err != nil {
			msg = "Caught error while getting the queryResponse"
			fmt.Println(msg)
			return shim.Error(err.Error())
		}

		//Key - timeRaster id
		fmt.Println("key" + queryResponse.Key)
		//Value - timeRasterDetail doc
		fmt.Println("value" + string(queryResponse.Value))

		currentTimeRasterDetail := timeRasterDetail{}
		if err := json.Unmarshal(queryResponse.Value, &currentTimeRasterDetail); err != nil {
			panic(err)
		}
		fmt.Println(currentTimeRasterDetail)

		sumTemp = sumTemp + currentTimeRasterDetail.Temperature
		counter++
		fmt.Println("Pass 3")
	}

	avgTemp = sumTemp / counter
	fmt.Println("avgTemp ", avgTemp)

	fmt.Println("counter ", counter)
	fmt.Println("samplingPoints ", samplingPoints)

	//record temp breach if avgTemp stays above 20.0 for more than 30 mins
	if avgTemp > 20.0 && counter >= samplingPoints {
		//update the shipment record
		queryString := fmt.Sprintf("{\"selector\":{\"docType\":\"shipment\",\"shipmentID\":\"%s\"}}", shipmentID)
		fmt.Println("queryString for setting temp breach ", queryString)

		resultsIterator, err := stub.GetQueryResult(queryString)
		var msg string
		if err != nil {
			msg = "Error performing the query operation"
			fmt.Println(msg)
			return shim.Error(err.Error())
		}
		defer resultsIterator.Close()

		for resultsIterator.HasNext() {
			queryResponse, err := resultsIterator.Next()
			if err != nil {
				msg = "Caught error while getting the queryResponse"
				fmt.Println(msg)
				return shim.Error(err.Error())
			}

			//Key - shipment id
			fmt.Println("key" + queryResponse.Key)
			//Value - shipment doc
			fmt.Println("value" + string(queryResponse.Value))

			shipmentDoc := shipment{}
			if err := json.Unmarshal(queryResponse.Value, &shipmentDoc); err != nil {
				panic(err)
			}
			fmt.Println(shipmentDoc)

			updateType := "tempBreach"
			err = updateShipmentRecord(stub, shipmentDoc, shipmentDoc.Status, avgTemp, updateType)
			var msg string
			if err != nil {
				msg = "Could not update the Shipment"
				fmt.Println(msg)
				return shim.Error(err.Error())
			}
			msg = "Successfully updated the Shipment " + shipmentDoc.ShipmentID
			msgAsBytes, _ := json.Marshal(msg)
			return shim.Success(msgAsBytes)
		}
	}

	msg = "Temp breach did not occur for shipmentID " + shipmentID
	msgAsBytes, _ := json.Marshal(msg)
	return shim.Success(msgAsBytes)
}

func (t *SupplyChaincode) generateSellerView(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	jsonString := []byte(args[0]) //data coming from the IOT device or WeatherReport apis
	var sellerViewInterface map[string]interface{}
	sellerShipments := []sellerView{}
	if err := json.Unmarshal(jsonString, &sellerViewInterface); err != nil {
		panic(err)
	}
	fmt.Println(sellerViewInterface)

	sellerID := sellerViewInterface["sellerID"].(string)

	queryString := fmt.Sprintf("{\"selector\":{\"docType\":\"shipment\",\"sellerID\":\"%s\"}}", sellerID)
	fmt.Println("queryString in generateSellerView ", queryString)

	resultsIterator, err := stub.GetQueryResult(queryString)
	var msg string
	if err != nil {
		msg = "Error performing the query operation"
		fmt.Println(msg)
		return shim.Error(err.Error())
	}
	defer resultsIterator.Close()
	fmt.Println("Pass 1")

	for resultsIterator.HasNext() {
		queryResponse, err := resultsIterator.Next()
		if err != nil {
			msg = "Caught error while getting the queryResponse"
			fmt.Println(msg)
			return shim.Error(err.Error())
		}

		//Key - shipment id
		fmt.Println("key" + queryResponse.Key)
		//Value - shipment doc
		fmt.Println("value" + string(queryResponse.Value))

		retreivedShipment := shipment{}
		if err := json.Unmarshal(queryResponse.Value, &retreivedShipment); err != nil {
			panic(err)
		}
		fmt.Println(retreivedShipment)

		sellerLocation, err := getLocationByActorId(stub, retreivedShipment.SellerID)
		if err != nil {
			msg = "Could not get seller location"
			fmt.Println(msg)
			return shim.Error(err.Error())
		}

		buyerLocation, err := getLocationByActorId(stub, retreivedShipment.BuyerID)
		if err != nil {
			msg = "Could not get buyer location"
			fmt.Println(msg)
			return shim.Error(err.Error())
		}

		sellerView := &sellerView{retreivedShipment.ShipmentID, sellerLocation, retreivedShipment.Status, retreivedShipment.BuyerID, buyerLocation, retreivedShipment.TempBreach}

		sellerShipments = append(sellerShipments, *sellerView)
	}

	sellerViewJSONasBytes, _ := json.Marshal(sellerShipments)
	return shim.Success(sellerViewJSONasBytes)
}

func (t *SupplyChaincode) generateLogisticsView(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	jsonString := []byte(args[0]) //data coming from the IOT device or WeatherReport apis
	var logisticsViewInterface map[string]interface{}
	logisticsShipments := []logisticsView{}
	if err := json.Unmarshal(jsonString, &logisticsViewInterface); err != nil {
		panic(err)
	}
	fmt.Println(logisticsViewInterface)

	lgpID := logisticsViewInterface["lgpID"].(string)

	queryString := fmt.Sprintf("{\"selector\":{\"docType\":\"shipment\",\"lgpID\":\"%s\",\"status\":{\"$or\":[\"In-Transit\",\"Delivered\"]}}}", lgpID)
	fmt.Println("queryString in generateLogisticsView ", queryString)

	resultsIterator, err := stub.GetQueryResult(queryString)
	var msg string
	if err != nil {
		msg = "Error performing the query operation"
		fmt.Println(msg)
		return shim.Error(err.Error())
	}
	defer resultsIterator.Close()
	fmt.Println("Pass 1")

	for resultsIterator.HasNext() {
		queryResponse, err := resultsIterator.Next()
		if err != nil {
			msg = "Caught error while getting the queryResponse"
			fmt.Println(msg)
			return shim.Error(err.Error())
		}

		//Key - shipment id
		fmt.Println("key" + queryResponse.Key)
		//Value - shipment doc
		fmt.Println("value" + string(queryResponse.Value))

		retreivedShipment := shipment{}
		if err := json.Unmarshal(queryResponse.Value, &retreivedShipment); err != nil {
			panic(err)
		}
		fmt.Println(retreivedShipment)

		sellerLocation, err := getLocationByActorId(stub, retreivedShipment.SellerID)
		if err != nil {
			msg = "Could not get seller location"
			fmt.Println(msg)
			return shim.Error(err.Error())
		}

		buyerLocation, err := getLocationByActorId(stub, retreivedShipment.BuyerID)
		if err != nil {
			msg = "Could not get buyer location"
			fmt.Println(msg)
			return shim.Error(err.Error())
		}

		logisticsView := &logisticsView{retreivedShipment.ShipmentID, sellerLocation, retreivedShipment.Status, retreivedShipment.LGPID, retreivedShipment.BuyerID, buyerLocation, retreivedShipment.TempBreach}

		logisticsShipments = append(logisticsShipments, *logisticsView)
	}

	logisticsViewJSONasBytes, _ := json.Marshal(logisticsShipments)
	return shim.Success(logisticsViewJSONasBytes)
}

func (t *SupplyChaincode) generateBuyerView(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	jsonString := []byte(args[0]) //data coming from the IOT device or WeatherReport apis
	var buyerViewInterface map[string]interface{}
	buyerShipments := []buyerView{}
	if err := json.Unmarshal(jsonString, &buyerViewInterface); err != nil {
		panic(err)
	}
	fmt.Println(buyerViewInterface)

	buyerID := buyerViewInterface["buyerID"].(string)

	queryString := fmt.Sprintf("{\"selector\":{\"docType\":\"shipment\",\"buyerID\":\"%s\",\"status\":\"Delivered\"}}", buyerID)
	fmt.Println("queryString in generateBuyerView ", queryString)

	resultsIterator, err := stub.GetQueryResult(queryString)
	var msg string
	if err != nil {
		msg = "Error performing the query operation"
		fmt.Println(msg)
		return shim.Error(err.Error())
	}
	defer resultsIterator.Close()
	fmt.Println("Pass 1")

	for resultsIterator.HasNext() {
		queryResponse, err := resultsIterator.Next()
		if err != nil {
			msg = "Caught error while getting the queryResponse"
			fmt.Println(msg)
			return shim.Error(err.Error())
		}

		//Key - shipment id
		fmt.Println("key" + queryResponse.Key)
		//Value - shipment doc
		fmt.Println("value" + string(queryResponse.Value))

		retreivedShipment := shipment{}
		if err := json.Unmarshal(queryResponse.Value, &retreivedShipment); err != nil {
			panic(err)
		}
		fmt.Println(retreivedShipment)

		sellerLocation, err := getLocationByActorId(stub, retreivedShipment.SellerID)
		if err != nil {
			msg = "Could not get seller location"
			fmt.Println(msg)
			return shim.Error(err.Error())
		}

		buyerLocation, err := getLocationByActorId(stub, retreivedShipment.BuyerID)
		if err != nil {
			msg = "Could not get buyer location"
			fmt.Println(msg)
			return shim.Error(err.Error())
		}

		buyerView := &buyerView{retreivedShipment.ShipmentID, sellerLocation, retreivedShipment.SellerID, retreivedShipment.LGPID, buyerLocation, retreivedShipment.TempBreach}

		buyerShipments = append(buyerShipments, *buyerView)
	}

	buyerViewJSONasBytes, _ := json.Marshal(buyerShipments)
	return shim.Success(buyerViewJSONasBytes)
}

//Update temp breach for Shipment
func updateShipmentRecord(stub shim.ChaincodeStubInterface, shipmentDoc shipment, updatedStatus string, avgTemp float64, updateType string) error {
	var err error
	updatedshipmentDoc := shipmentDoc

	if updateType == "statusUpdate" {
		//update shipment status
		updatedshipmentDoc.Status = updatedStatus
	} else if updateType == "tempBreach" {
		//update temp breach

		breachTxt := "Yes (" + fmt.Sprintf("%f", avgTemp) + " Celsius)"
		updatedshipmentDoc.TempBreach = breachTxt
	}

	updatedShipmentJSONasBytes, _ := json.Marshal(updatedshipmentDoc)
	err = stub.PutState(updatedshipmentDoc.ShipmentID, updatedShipmentJSONasBytes)
	if err != nil {
		return err
	}
	return nil
}

//getLocationByActorId
func getLocationByActorId(stub shim.ChaincodeStubInterface, actorId string) (string, error) {
	var err error

	queryString := fmt.Sprintf("{\"selector\":{\"docType\":\"actor\",\"id\":\"%s\"}}", actorId)
	fmt.Println("queryString in getLocationByActorId ", queryString)

	resultsIterator, err := stub.GetQueryResult(queryString)
	var msg string
	if err != nil {
		msg = "Error performing the query operation"
		return msg, err
	}
	defer resultsIterator.Close()

	var location string

	for resultsIterator.HasNext() {
		queryResponse, err := resultsIterator.Next()
		if err != nil {
			msg = "Caught error while getting the queryResponse"
			return msg, err
		}

		//Key - actor id
		fmt.Println("key" + queryResponse.Key)
		//Value - actor doc
		fmt.Println("value" + string(queryResponse.Value))

		foundActor := actor{}
		if err := json.Unmarshal(queryResponse.Value, &foundActor); err != nil {
			panic(err)
		}
		fmt.Println(foundActor)

		location = foundActor.Location
	}
	if err != nil {
		msg = "Caught error while parsing actor"
		return msg, err
	}
	return location, nil
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
func (t *SupplyChaincode) queryState(stub shim.ChaincodeStubInterface, args []string) pb.Response {

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

/**	convert time to milliseconds
Reference: https://gobyexample.com/epoch
*/
func makeTimestampInMS(time time.Time) int64 {
	return time.UnixNano() / 1000000
}

/**
Below method returns the contract history based on the shipmentID
*/
func (t *SupplyChaincode) getHistoryForShipment(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	if len(args) < 1 {
		return shim.Error("Incorrect number of arguments. Expecting 1")
	}

	shipmentID := args[0]

	fmt.Printf("- start getHistoryForShipment: %s\n", shipmentID)

	resultsIterator, err := stub.GetHistoryForKey(shipmentID)
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

	fmt.Printf("- getHistoryForShipment returning:\n%s\n", buffer.String())

	return shim.Success(buffer.Bytes())
}
