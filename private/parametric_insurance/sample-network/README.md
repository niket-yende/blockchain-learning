## Sample Network Config

Note that this basic configuration uses pre-generated certificates and
key material, and also has predefined transactions to initialize a
channel named "mychannel".

Install the prerequisites, Samples, Binaries and Docker Images before running the scripts as mentioned in the hyperledger-Fabric docs and also fabric-samples
<a href="https://hyperledger-fabric.readthedocs.io/en/release-1.2/prereqs.html">Prerequisites</a><br/>
<a  href="https://hyperledger-fabric.readthedocs.io/en/release-1.2/install.html">Install Samples, Binaries and Docker Images</a>

**To generate the material:**
`generate.sh`.

**To stop it, run** 
`stop.sh`

**To completely remove all incriminating evidence of the network on your system, run** 
`teardown.sh`.

**After crypto content is generated succesfully, follow the below steps:**
`export COMPOSE_PROJECT_NAME=network`

**To bring up the network, Run the docker command** 
 Change the private keys
`docker-compose -f docker-compose-e2e.yml up -d`

**Enter into the docker cotainer using command** 
`docker exec -it cli bash`

**Export the CHANNEL_NAME using the command**
`export CHANNEL_NAME=mychannel`

**Create the channel using the command**
`peer channel create -o orderer.example.com:7050 -c $CHANNEL_NAME -f ./channel-artifacts/channel.tx`

**join peer0.org1.example.com to the channel**
`peer channel join -b mychannel.block`

**join peer0.org2.example.com to the channel**
`CORE_PEER_MSPCONFIGPATH=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/org2.example.com/users/Admin@org2.example.com/msp CORE_PEER_ADDRESS=peer0.org2.example.com:7051 CORE_PEER_LOCALMSPID="Org2MSP" peer channel join -b mychannel.block`

**update anchor peer for Org1**
`peer channel update -o orderer.example.com:7050 -c $CHANNEL_NAME -f ./channel-artifacts/Org1MSPanchors.tx`

**update anchor peer for Org2**
`CORE_PEER_MSPCONFIGPATH=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/org2.example.com/users/Admin@org2.example.com/msp CORE_PEER_ADDRESS=peer0.org2.example.com:7051 CORE_PEER_LOCALMSPID="Org2MSP" peer channel update -o orderer.example.com:7050 -c $CHANNEL_NAME -f ./channel-artifacts/Org2MSPanchors.tx`

**Install custom_chaincode**
`peer chaincode install -n customcc -v 1.0 -p github.com/custom_chaincode/`

**Instantiate custom_chaincode**
`peer chaincode instantiate -o orderer.example.com:7050 -C $CHANNEL_NAME -n customcc -v 1.0 -c '{"Args":[""]}' -P "OR ('Org1MSP.peer','Org2MSP.peer')"`


**Advance steps to build and troubleshoot chaincode**

For an already deployed blockchain n/w, you would require to copy the latest chaincode to the cli container using the below command:
`docker cp <chaincode_file.go> <container_id>:<path_to_destination_folder>`

Referece: https://docs.docker.com/engine/reference/commandline/cp/

