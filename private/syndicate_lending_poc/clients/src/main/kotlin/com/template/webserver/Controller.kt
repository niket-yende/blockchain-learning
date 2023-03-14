package com.template.webserver

import com.template.flows.*
import com.template.schema.InitiationSchema1
import com.template.schema.SubscriptionSchema1
import com.template.schema.TermSheetSchema1
import com.template.states.*
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.transactions.SignedTransaction
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import javax.json.JsonObject
import javax.ws.rs.core.Response
import java.util.HashMap
import javax.ws.rs.PathParam


class SubscriptionInput(
//        val subscriptionId: String,
        val loanRef: String,
        val subscriptionName: String,
        val loanAmount: Double,
        val tenure: Double,
        val termSheet: String,
        val lender: String,
        val leadArranger: String,
        val subscriptionAmount: Double,
        val lenderA:String,
        val lenderB:String


)

data class LoanRef(
        val loanRef: String
){
    constructor():this(
            ""
    )
}

data class SubscriptionId(
        val loanRef: String,
        val subscriptionAmount: Double
){
    constructor():this(
            "",0.0
    )
}

data class InitiationModel(
        val loanRef: String,
        val issuerName:String,
        val entityType:String,
        val syndicationType:String,
        val loanType:String,
        val tenure:Int
){
    constructor():this(
            "","", "","","",0
    )
}

/**
 * Define your API endpoints here.
 */



@RestController
@RequestMapping("/api") // The paths for HTTP requests are relative to this base path.
class Controller(val rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

//    private val proxy = rpc.proxy

    @GetMapping(value = "/templateendpoint", produces = arrayOf("text/plain"))
    private fun templateendpoint(): String {
        return "Define an endpoint here."
    }

    @GetMapping("/me",produces = arrayOf("application/json"))
    fun whoAmI(@RequestHeader(value = "PartyName") callingParty: String): CordaX500Name{
        return rpc.myRPCMap.get(callingParty)?.proxy?.nodeInfo()?.legalIdentities?.get(0)?.name as CordaX500Name
//        return proxy.nodeInfo().legalIdentities.get(0).name
    }

    @PostMapping("/createInitiationState", produces = arrayOf("application/json"))
    fun createInitiationState(@RequestHeader(value = "PartyName") callingParty: String, @RequestBody initiationObject: InitiationModel): LinkedHashMap<String,Any>{
        val partyProxyObj = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps

        if (initiationObject == null){

            return linkedMapOf(Pair("status",Response.Status.BAD_REQUEST), Pair("message","The initiationObject input must not be null"), Pair("data",{}))
        }


//        val randomValue = (Math.random() * 1000 + 1).toInt()
        var value: Int = 0
//        var randomValue: String = "0000"

        var allRecords: List<StateAndRef<InitiationState>> = partyProxyObj.vaultQuery(InitiationState::class.java).states
        value = (allRecords.size)+1

        val loanRef = "Loan" + value
        println("loanRef "+loanRef)

        val leadArranger = partyProxyObj.wellKnownPartyFromX500Name(CordaX500Name.parse("O=LeadArranger,L=New York,C=US")) as Party

        val initiationState = InitiationState(loanRef,initiationObject.issuerName,initiationObject.entityType,initiationObject.syndicationType,initiationObject.loanType,initiationObject.tenure,leadArranger)

        val signedTx:SignedTransaction = partyProxyObj
                .startTrackedFlowDynamic(LAInitiationFlow::class.java,initiationState).returnValue.get()

        var map = HashMap<String,Any>()
        map["loanRef"] = loanRef

        return linkedMapOf(Pair("status",Response.Status.CREATED), Pair("message","Initiation state succesfully created"), Pair("data",map))
    }

    //This api will return details of an account given a loanRef
    @GetMapping(value = "/getInitiationData/{loanRef}", produces = arrayOf("application/json"))
    fun getInitiationDataByLoanRef(@RequestHeader(value = "PartyName") callingParty: String, @PathVariable("loanRef") loanRef:String): InitiationModel{
//        var returnRecords: List<BankState> = ArrayList<BankState>()
        val partyProxyObj = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps

        println("loanRef "+loanRef)

        //get the calling party details
        val requestingParty: Party = partyProxyObj.nodeInfo().legalIdentities.get(0)

        println("Requesting party name "+requestingParty.name)

        //Check if the loanRef is present in the staticDataState
        val loanIdCriteria = builder { InitiationSchema1.PersistentInitiation::loan_ref.equal(loanRef) }

        var criteria = QueryCriteria.VaultCustomQueryCriteria(loanIdCriteria)

        val intiationStates = partyProxyObj.vaultQueryByCriteria(criteria, InitiationState::class.java).states
        val intitionState = intiationStates.get(0).state.data
        val initiationModel = InitiationModel(intitionState.loanRef, intitionState.issuerName, intitionState.entityType, intitionState.syndicationType, intitionState.loanType, intitionState.tenure)
        return initiationModel
    }

    @GetMapping(value = "/getAllInitiationData", produces = arrayOf("application/json"))
    fun getAllInitiationData(@RequestHeader(value = "PartyName") callingParty: String): List<InitiationModel>{
//        var returnRecords: List<BankState> = ArrayList<BankState>()
        //get the calling party details

        val partyProxyObj = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps

        val requestingParty: Party = partyProxyObj.nodeInfo().legalIdentities.get(0)

        println("Requesting party name "+requestingParty.name)

        //Check if the loanRef is present in the staticDataState

        var allRecords: List<StateAndRef<InitiationState>> = partyProxyObj.vaultQuery(InitiationState::class.java).states
        var returnInitiation : MutableList<InitiationModel> = mutableListOf()
        for (i in allRecords){
            val single = i.state.data
            returnInitiation.add(InitiationModel(single.loanRef, single.issuerName, single.entityType, single.syndicationType, single.loanType, single.tenure))
        }

        return returnInitiation
    }

//    /**
//     * Uploads the attachment at [attachmentPath] to the node.
//     */
//    @PostMapping("/uploadTermSheet",consumes = arrayOf(org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE))
//    fun uploadTermSheet( @RequestParam file: MultipartFile, @RequestParam loanRef: String):  Response  {
//
//
//        var bytes = file.bytes
//        var path: Path = Paths.get("/Users/venkat.bandaru/Downloads/" + file.originalFilename)
//        Files.write(path, bytes)
//
//        //ZIP the downloaded file
//        var downloadedFile = File("/Users/venkat.bandaru/Downloads/" + file.originalFilename)
//        var zipFileName: String = downloadedFile.name + ".zip"
//        print("file ------------------ " + zipFileName)
//
//        var fos = FileOutputStream("/Users/venkat.bandaru/Downloads/" + zipFileName)
//        var zos = ZipOutputStream(fos)
//
//        zos.putNextEntry(ZipEntry(downloadedFile.name))
//
//        var bytes2 = Files.readAllBytes(Paths.get("/Users/venkat.bandaru/Downloads/" + downloadedFile.name))
//        zos.write(bytes2, 0, bytes2.size)
//        zos.closeEntry()
//        zos.close()
//
//        fos.close()
//
//        val attachmentUploadInputStream = File("/Users/venkat.bandaru/Downloads/" + zipFileName).inputStream()
//        val myHash = proxy.uploadAttachment(attachmentUploadInputStream).toString()
////        return "File Uploaded and Zipped!!! SecureHash = " + myHash
//
//        if (myHash.length == 0) {
//            return Response.status(Response.Status.CREATED).entity("File failed to upload").build()
//        }
//
//        val leadArranger = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse("O=LeadArranger,L=New York,C=US")) as Party
//
//        var uploadState: UploadState = UploadState(loanRef, file.originalFilename, UploadState.UploadStatus.COMPLETED, myHash, leadArranger)
//
//        val signedTx: SignedTransaction = proxy.startTrackedFlowDynamic(UploadFlow::class.java,uploadState).returnValue.get()
//        println("signedtransaction: " + signedTx)
//
//        val msg: String = "Term sheet file uploaded successfully with hash "+ myHash
//        return Response.status(Response.Status.CREATED).entity(msg).build()
//    }
//
//    /**
//     * Downloads the attachment with hash [attachmentHash] from the node.
//     */
//    @GetMapping("/downloadTermSheet", produces = arrayOf(MediaType.MULTIPART_FORM_DATA))
//    fun downloadTermSheet(@QueryParam("fileHash") fileHash: String): ResponseEntity<InputStreamResource> {
//        println("Term sheet file received " + fileHash)
//
////        val termSheetCriteria = builder { TermSheetSchema1.PersistentTermSheet::file_hash.equal(SecureHash.parse(fileHash)) }
////        val static_criteria = QueryCriteria.VaultCustomQueryCriteria(termSheetCriteria)
////        val termSheetStates = proxy.vaultQueryByCriteria(static_criteria, TermSheetState::class.java).states
////        if (termSheetStates.size == 0)
////            throw error("no termsheet with loan ref")
//
////        val termSheet = termSheetStates.get(0).state.data as TermSheetState
//
////        val hash = termSheet.fileHash
//
//        val file = File("/Users/venkat.bandaru/Downloads/OutputFile.pdf")
//        if (proxy.attachmentExists(SecureHash.parse(fileHash))) {
//            print("hash exists ---")
//
//
//            val inputJar = proxy.openAttachment(SecureHash.parse(fileHash))
//
//            var file_name_data: Pair<String, ByteArray>? = null
//            JarInputStream(inputJar).use { jar ->
//                while (true) {
//                    val nje = jar.nextEntry ?: break
//                    if (nje.isDirectory) {
//                        continue
//                    }
//                    file_name_data = Pair(nje.name, jar.readBytes())
//                }
//            }
//
//
//
//            val file = File("/Users/venkat.bandaru/Desktop/"+file_name_data?.first)
//
//            val bytes=file_name_data?.second as ByteArray
//            file.writeBytes(bytes)
//
//        }
//
//
//        val resource: InputStreamResource = InputStreamResource(FileInputStream(file))
//
//
//        var headers = HttpHeaders()
//
//        headers.add("Content-Disposition", String.format("attachment; filename=\"%s\"", file.name))
//        headers.add("Cache-Control", "no-cache, no-store, must-revalidate")
//        headers.add("Pragma", "no-cache")
//        headers.add("Expires", "0")
//
//        var responseEntity: ResponseEntity<InputStreamResource> = ResponseEntity.ok().headers(headers).contentLength(file.length()).contentType(org.springframework.http.MediaType.parseMediaType(MediaType.MULTIPART_FORM_DATA)).body(resource)
//
//
//        return responseEntity
//    }
//    private fun downloadAttachment(attachmentHash: SecureHash): JarInputStream {
//        val attachmentDownloadInputStream = proxy.openAttachment(attachmentHash)
//        return JarInputStream(attachmentDownloadInputStream)
//    }
//
//    /*@PostMapping("/createTermSheet")
//    private fun createTermSheet(@RequestBody termSheetState: TermSheetModel): Response{
//        if (termSheetState == null)
//            return Response.status(Response.Status.BAD_REQUEST).entity("The termSheetState input must not be null").build();
//        var allRecords: List<StateAndRef<TermSheetState>> = proxy.vaultQuery(TermSheetState::class.java).states
//        for(i in allRecords){
//            var singlerecord : StateAndRef<TermSheetState> = i
//            if (singlerecord.state.data.loanRef.equals(termSheetState.loanRef))
//                return Response.status(Response.Status.BAD_REQUEST).entity("LoanRef already exists in the database").build();
//        }
//        val termSheetInput = TermSheetState(termSheetState.loanRef,
//                proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(termSheetState.borrower)) as Party,
//                termSheetState.typeOfLoan,
//                proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(termSheetState.leadArranger)) as Party,
//                termSheetState.loanAmount,
//                TermSheetState.TermSheetStatus.CREATED,
//                SecureHash.parse(termSheetState.fileHash)
//                )
//        val signedTx:SignedTransaction = proxy
//                .startTrackedFlowDynamic(CreateTermSheetFlow::class.java,termSheetInput).returnValue.get()
//        val msg:String = "TermSheet State succesfullly created"
//        return Response.status(Response.Status.CREATED).entity(msg).build()
//    } */
//
//    @GetMapping("/getAllTermSheet",produces = arrayOf("application/json"))
//    private fun getAllTermSheet(): List<TermSheetState>{
//        var allRecords: List<StateAndRef<TermSheetState>> = proxy.vaultQuery(TermSheetState::class.java).states
//        return allRecords.map { it.state.data }
//    }
//
//    @GetMapping("/getTermSheetById/{id}",produces = arrayOf("application/json"))
//    private fun getAllTermSheetById(@PathVariable("id") id:String): TermSheetState{
//        val loadIdCriteria = builder { TermSheetSchema1.PersistentTermSheet::loan_ref.equal(id) }
//        val criteria = QueryCriteria.VaultCustomQueryCriteria(loadIdCriteria)
//        val termSheetStates = proxy.vaultQueryByCriteria(criteria, TermSheetState::class.java).states
//        return termSheetStates.get(0).state.data
//    }
//
//    @GetMapping("/getTermSheetHistoryById/{id}",produces = arrayOf("application/json"))
//    private fun getTermSheetHistoryById(@PathVariable("id") id:String): List<StateAndRef<TermSheetState>>{
//        val loadIdCriteria = builder { TermSheetSchema1.PersistentTermSheet::loan_ref.equal(id) }
//        val criteria = QueryCriteria.VaultCustomQueryCriteria(loadIdCriteria,Vault.StateStatus.ALL)
//        val termSheetStates = proxy.vaultQueryByCriteria(criteria, TermSheetState::class.java).states
//        return termSheetStates.map { it }
//    }
//
//    @PutMapping("/approveTermSheetByBorrower")
//    private fun approveTermSheet(@RequestBody name: LoanRef): Response{
////        if (name == null)
////            return Response.status(Response.Status.BAD_REQUEST).entity("The Loan Ref cannot be null").build()
//        val signedTx:SignedTransaction = proxy
//                .startTrackedFlowDynamic(ApproveTermSheetFlow::class.java,name.loanRef).returnValue.get()
//        return Response.status(Response.Status.ACCEPTED).entity("Term sheet Approved").build()
//    }

//    @PostMapping("addKeyData")
//    private fun addKeyData(@RequestBody staticData:StaticDataState): Response{
//        if (staticData == null)
//            return Response.status(Response.Status.BAD_REQUEST).entity("The Key Data must not be null").build()
//        val loadIdCriteria = builder { TermSheetSchema1.PersistentTermSheet::loan_ref.equal(staticData.loanRef) }
//        val criteria = QueryCriteria.VaultCustomQueryCriteria(loadIdCriteria)
//        val termSheetStates = proxy.vaultQueryByCriteria(criteria, TermSheetState::class.java).states
//        if (termSheetStates.size == 0)
//            return Response.status(Response.Status.BAD_REQUEST).entity("The Loan Reference specified is not present").build()
//        val signedTx = proxy.startTrackedFlowDynamic(CreateStaticDataFlow::class.java,staticData)
//        val msg:String = "The Static Data is successfully created"
//        return Response.status(Response.Status.CREATED).entity(msg).build()
//    }

    /**
     * Below code moved to SubscriptionController
     */
    /*@PostMapping("addSubscriptionLender")
    private fun createSubscriptionLedger(@RequestBody subscriptionInput:SubscriptionInput): Response{
        if (subscriptionInput == null)
            return Response.status(Response.Status.BAD_REQUEST).entity("The subscription data must not be null").build()
        val loadIdCriteria = builder { TermSheetSchema1.PersistentTermSheet::loan_ref.equal(subscriptionInput.loanRef) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(loadIdCriteria)
        val termSheetStates = proxy.vaultQueryByCriteria(criteria, TermSheetState::class.java).states
        if (termSheetStates.size == 0)
            return Response.status(Response.Status.BAD_REQUEST).entity("The Loan Reference specified is not present").build()
        val subscriptionIdCriteria = builder { SubscriptionSchema1.PersistentSubsciption::subscription_id.equal(subscriptionInput.subscriptionId) }
        val criteria2 = QueryCriteria.VaultCustomQueryCriteria(subscriptionIdCriteria)
        val subscriptionStates = proxy.vaultQueryByCriteria(criteria2, SubscriptionState::class.java).states
        if (termSheetStates.size == 1)
            return Response.status(Response.Status.BAD_REQUEST).entity("The Subscription state Id specified is already present").build()
        val subscriptionState = SubscriptionState(
        subscriptionInput.subscriptionId,
        subscriptionInput.loanRef,
        subscriptionInput.subscriptionName,
        Instant.parse(subscriptionInput.startDate),
        Instant.parse(subscriptionInput.endDate),
        subscriptionInput.loanAmount,
        subscriptionInput.tenure,
        subscriptionInput.termSheet,
                proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(subscriptionInput.leadArranger)) as Party, //lender cannot be null
        proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(subscriptionInput.leadArranger)) as Party,
                subscriptionInput.subscriptionAmount,
                SubscriptionState.SubscriptionStatus.CREATED
        )
        val signedTx:List<SignedTransaction> = proxy
                .startTrackedFlowDynamic(CreateSubscriptionLedgerFlow::class.java,subscriptionState,subscriptionInput.lenderA,subscriptionInput.lenderB).returnValue.get()
        val msg:String = "The Subscription Ledgers for two lenders is successfully created"
        return Response.status(Response.Status.CREATED).entity(msg).build()
    }

    @PutMapping("/approveLenderSubscriptionByJson")
    private fun approveSubscriptionLedgerJson(@RequestBody name: JsonObject): Response{
        if (name.get("subscriptionId")== null)
            return Response.status(Response.Status.BAD_REQUEST).entity("The SubscriptionId cannot be null").build()
        val signedTx:SignedTransaction = proxy
                .startTrackedFlowDynamic(ApproveSubscriptionLedgerFlow::class.java,name.get("subscriptionId")).returnValue.get()
        return Response.status(Response.Status.ACCEPTED).entity("Term sheet Approved").build()
    }

    @PutMapping("/approveLenderSubscriptionById")
    private fun approveSubscriptionLedgerId(@RequestBody name: SubscriptionId): Response{
//        if (name == null)
//            return Response.status(Response.Status.BAD_REQUEST).entity("The Loan Ref cannot be null").build()
        val signedTx:SignedTransaction = proxy
                .startTrackedFlowDynamic(ApproveSubscriptionLedgerFlow::class.java,name.subscriptionId).returnValue.get()
        return Response.status(Response.Status.ACCEPTED).entity("Term sheet Approved").build()
    }

    @GetMapping("/getAllSubscriptions",produces = arrayOf("application/json"))
    private fun getAllSubscriptions(): List<SubscriptionState>{
        var allRecords: List<StateAndRef<SubscriptionState>> = proxy.vaultQuery(SubscriptionState::class.java).states
        return allRecords.map { it.state.data }
    }

    @GetMapping("/getSubscriptionsById/{id}",produces = arrayOf("application/json"))
    private fun getSubscriptionsById(@PathVariable("id") id:String): SubscriptionState{
        val loadIdCriteria = builder { SubscriptionSchema1.PersistentSubsciption::subscription_id.equal(id) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(loadIdCriteria)
        val subscriptionStates = proxy.vaultQueryByCriteria(criteria, SubscriptionState::class.java).states
        return subscriptionStates.get(0).state.data
    }

    @GetMapping("/getSubscriptionsHistoryById/{id}",produces = arrayOf("application/json"))
    private fun getSubscriptionsHistoryById(@PathVariable("id") id:String): List<StateAndRef<SubscriptionState>>{
        val loadIdCriteria = builder { SubscriptionSchema1.PersistentSubsciption::subscription_id.equal(id) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(loadIdCriteria,Vault.StateStatus.ALL)
        val subscriptionStates = proxy.vaultQueryByCriteria(criteria, SubscriptionState::class.java).states
        return subscriptionStates.map { it }
    } */

}