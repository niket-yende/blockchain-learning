//package com.template.webserver
package com.template.webserver

import com.template.Models.StaticDataModel
import com.template.Models.SubscriptionModel
import com.template.flows.ApproveSubscriptionLedgerFlow
import com.template.flows.CreateStaticDataFlow
import com.template.flows.CreateSubscriptionLedgerFlow
import com.template.flows.UpdateStaticDataFlow
import com.template.schema.InitiationSchema1
import com.template.schema.TermSheetSchema1
import com.template.schema.StaticDataSchema1
import com.template.schema.SubscriptionSchema1
import com.template.states.InitiationState
import com.template.states.StaticDataState
import com.template.states.SubscriptionState
import com.template.states.TermSheetState
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
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import javax.json.JsonObject
import javax.ws.rs.core.Response

class MemberModel(
        val loanRef: String,
        val entityName: String,
        val entityType: String,
        val accountName: String,
        val acountType: String,
        val paymentAccount: String,
        val paymentBank: String,
        val leadArrangerName: String,
        val leadArrangerAccountNumber: String,
        val rateOfInterest: Double,
        val leadArrangerBank: String
) {
    constructor() : this(
            "", "", "", "", "", "" +
            "", "", "", "", 0.0, ""
    )
}

//open class SubscriptionModel {
//    val subscriptionRef: String = ""
//    val subscriptionName: String = ""
//    val startDate: String = ""
//    val endDate: String = ""
//    val size: Long = 0
//    val tenure: Int = 0
//    val termSheetRef: String = ""
//}
//
//class LenderSubscribeModel : SubscriptionModel() {
//    val lenderName: String = ""
//    val sizeToSubscribe: Int = 0
//}

@RestController
@RequestMapping("/api/subscription") // The paths for HTTP requests are relative to this base path.
class SubscriptionController(val rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

//    private val proxy = rpc.proxy

//    @GetMapping(value = "/templateendpoint", produces = arrayOf("text/plain"))
//    private fun templateendpoint(): String {
//        return "Define an endpoint here."
//    }

    //Get all the metadata for a given loanRef
    @GetMapping(value = "getStateMetadata/{loanRef}", produces = arrayOf("application/json"))
    fun getStateMetadata(@PathVariable("loanRef") loanRef: String,@RequestHeader(value = "PartyName") callingParty: String): List<StaticDataModel> {
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
        println("loan ref Id " + loanRef)
        val stateIDCriteria = builder { StaticDataSchema1.PersistentStaticData::loan_ref.equal(loanRef) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(stateIDCriteria)
        val accountStates = proxy.vaultQueryByCriteria(criteria, StaticDataState::class.java).states
        var records: MutableList<StaticDataModel> = mutableListOf()
        for (i in accountStates) {
            var record = i.state.data
            records.add(StaticDataModel(record.loanRef, record.borrower.name.organisation.toString(), record.borrowerAccountNumber, record.borrowerBank, record.borrowerLoanAmount, record.lenderA.name.organisation.toString(),
                    record.lenderAAccountNumber, record.lenderABank, record.lenderAContribution, record.lenderB.name.organisation.toString(), record.lenderBAccountNumber, record.lenderBBank,
                    record.lenderBContribution, record.leadArranger.name.organisation.toString(), record.leadArrangerAccountNumber, record.leadArrangerContribution, record.rateOfInterest,
                    record.frequency, record.payingBank, record.tenure, record.startDate.toString(), record.endDate.toString()))
        }
        return records

    }

    @PostMapping(value = "/addMemberDetails")
    fun addMemberDetails(@RequestBody member: MemberModel,@RequestHeader(value = "PartyName") callingParty: String): LinkedHashMap<String, Any> {
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
        val response = JSONObject()
//        try {
        println("~~~Input State" + member)
        if (member == null) {
//            return Response.status(Response.Status.BAD_REQUEST).entity("The member input must not be null").build();
//            return response
            return linkedMapOf(Pair("status", Response.Status.BAD_REQUEST), Pair("message", "The member input must not be null"), Pair("data", {}))
        }


        val loadIdCriteria = builder { TermSheetSchema1.PersistentTermSheet::loan_ref.equal(member.loanRef) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(loadIdCriteria)
        val termSheetStates = proxy.vaultQueryByCriteria(criteria, TermSheetState::class.java).states
        if (termSheetStates.size == 0) {
//            return Response.status(Response.Status.BAD_REQUEST).entity("The Loan Reference specified is not present").build()
            return linkedMapOf(Pair("status", Response.Status.BAD_REQUEST), Pair("message", "The Loan Reference specified is not present"), Pair("data", {}))
        }

        val initiationCriteria = builder { InitiationSchema1.PersistentInitiation::loan_ref.equal(member.loanRef) }
        val criteria1 = QueryCriteria.VaultCustomQueryCriteria(initiationCriteria)
        val initiationStates = proxy.vaultQueryByCriteria(criteria1, InitiationState::class.java).states
        if (initiationStates.size == 0) {
//            return Response.status(Response.Status.BAD_REQUEST).entity("The Loan Reference specified is not present").build()
            return linkedMapOf(Pair("status", Response.Status.BAD_REQUEST), Pair("message", "The Initiaiton state required is not present"), Pair("data", {}))
        }


        //Check if the loanRef is present in the staticDataState
        val loanIdCriteria = builder { StaticDataSchema1.PersistentStaticData::loan_ref.equal(member.loanRef) }
        val static_criteria = QueryCriteria.VaultCustomQueryCriteria(loanIdCriteria)
        val staticDataStates = proxy.vaultQueryByCriteria(static_criteria, StaticDataState::class.java).states
        println("staticDataStates size : " + staticDataStates.size)
        if (staticDataStates.size == 0) {
            println("The Loan Reference specified is not present, adding a new one")
            var borrowerName: String = member.entityName
            var borrower = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse("O=Borrower,L=London,C=GB")) as Party

            var borrowerAccount: String = member.paymentAccount
            var borrowerBank: String = member.paymentBank

            var leadArrangerAccountNumber: String = member.leadArrangerAccountNumber
            var rateOfInterest: Double = member.rateOfInterest
            var payingBank: String = member.leadArrangerBank
            //hardcoding party names
            var leadArranger = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse("O=LeadArranger,L=New York,C=US")) as Party
//            var leadArranger = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(member.leadArrangerName)) as Party

            var lenderAName: String = ""
//            var lenderA =  null
            var lenderAAccountNumber: String = ""
            var lenderABank: String = ""
            var lenderBName: String = ""
//            var lenderB: Party = null
            var lenderBAccountNumber: String = ""
            var lenderBBank: String = ""
            var leadArrangerName: String = ""
            var tenure: Int = initiationStates.get(0).state.data.tenure

            //Query initiation state to get borrower amount
//            val loanIdCriteria1 = builder { InitiationSchema1.PersistentInitiation::loan_ref.equal(member.loanRef) }
//
//            var criteria1 = QueryCriteria.VaultCustomQueryCriteria(loanIdCriteria1)
//
//            val intiationStates = proxy.vaultQueryByCriteria(criteria1, InitiationState::class.java).states
//            val intitionState = intiationStates.get(0).state.data

            val staticDataState = StaticDataState(
                    loanRef = member.loanRef,
                    borrower = borrower,
                    borrowerAccountNumber = borrowerAccount,
                    borrowerBank = borrowerBank,
                    borrowerLoanAmount = 0.0,
                    lenderA = leadArranger, //initially there will be no lender
                    lenderAAccountNumber = lenderAAccountNumber,
                    lenderABank = lenderABank,
                    lenderB = leadArranger, //initially there will be no lender
                    lenderBAccountNumber = lenderBAccountNumber,
                    lenderBBank = lenderBBank,
                    leadArranger = leadArranger,
                    leadArrangerAccountNumber = leadArrangerAccountNumber,
                    rateOfInterest = rateOfInterest, //this should be replaced with static value
                    frequency = "",
                    payingBank = payingBank,
                    lenderAContribution = 0.0,
                    lenderBContribution = 0.0,
                    leadArrangerContribution = 0.0,
                    tenure = tenure,
                    startDate = LocalDate.now(), endDate = LocalDate.now(),
                    paymentDate = 0
            )

            println("~~~Converted static data State: " + staticDataState)
            val signedTx: SignedTransaction = proxy
                    .startTrackedFlowDynamic(CreateStaticDataFlow::class.java, staticDataState).returnValue.get()
            println("signedtransaction: " + signedTx)
            val msg: String = "Static data created succesfully"

//            return Response.status(Response.Status.CREATED).entity(msg).build()
            return linkedMapOf(Pair("status", Response.Status.CREATED), Pair("message", "Static data created succesfully"))
        } else {
            //it should update only new details
            println("Reached the else part")
            println("MemberModel data : " + member.toString())
            println("entityName : " + member.entityName + " account : " + member.paymentAccount + " bank : " + member.paymentBank)
            var staticDataState = staticDataStates.get(0).state.data
            var name: String = member.entityName
            var account: String = member.paymentAccount
            var bank: String = member.paymentBank
            println("member type : " + member.entityType)
            val entityType: String = member.entityType.toLowerCase()
            if (entityType.equals("borrower")) {
                var borrower = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse("O=Borrower,L=London,C=GB")) as Party
                staticDataState = staticDataStates.get(0).state.data.copy(borrower = borrower, borrowerAccountNumber = account, borrowerBank = bank)
            } else if (entityType.equals("lender")) {
                if (member.entityName.equals("LenderA")) {
                    var lenderA = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse("O=LenderA,L=New York,C=US")) as Party
                    staticDataState = staticDataStates.get(0).state.data.copy(lenderA = lenderA, lenderAAccountNumber = account, lenderABank = bank)
                } else if (member.entityName.equals("LenderB")) {
                    var lenderB = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse("O=LenderB,L=New York,C=US")) as Party
                    staticDataState = staticDataStates.get(0).state.data.copy(lenderB = lenderB, lenderBAccountNumber = account, lenderBBank = bank)
                }
            }
            println("~~~Updated static data State " + staticDataState)
            val signedTx: SignedTransaction = proxy
                    .startTrackedFlowDynamic(UpdateStaticDataFlow::class.java, staticDataState).returnValue.get()
            println("signedtransaction: " + signedTx)
//            val msg:String = "Static data updated successfully"
            //change the response from created to updated
//            return Response.status(Response.Status.CREATED).entity(msg).build()

            return linkedMapOf(Pair("status", Response.Status.OK), Pair("message", "Static data updated successfully"))
        }
    }


    @PostMapping("addSubscriptionLender")
    fun createSubscriptionLedger(@RequestBody subscriptionInput: SubscriptionInput,@RequestHeader(value = "PartyName") callingParty: String): LinkedHashMap<String, Any> {
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
        if (subscriptionInput == null) {
//                return Response.status(Response.Status.BAD_REQUEST).entity("The subscription data must not be null").build()
            return linkedMapOf(Pair("status", Response.Status.BAD_REQUEST), Pair("message", "The subscription data must not be null"))
        }


        val loadIdCriteria = builder { TermSheetSchema1.PersistentTermSheet::loan_ref.equal(subscriptionInput.loanRef) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(loadIdCriteria)
        val termSheetStates = proxy.vaultQueryByCriteria(criteria, TermSheetState::class.java).states
        if (termSheetStates.size == 0) {
//                return Response.status(Response.Status.BAD_REQUEST).entity("The Loan Reference specified is not present").build()
            return linkedMapOf(Pair("status", Response.Status.BAD_REQUEST), Pair("message", "The Loan Reference specified is not present"))
        }

        var allRecords: List<StateAndRef<SubscriptionState>> = proxy.vaultQuery(SubscriptionState::class.java).states
        val subscriptionId: String = "SUB" + (100 + allRecords.size)

//            val subscriptionIdCriteria = builder { SubscriptionSchema1.PersistentSubsciption::subscription_id.equal(subscriptionId) }
//            val criteria2 = QueryCriteria.VaultCustomQueryCriteria(subscriptionIdCriteria)
//            val subscriptionStates = proxy.vaultQueryByCriteria(criteria2, SubscriptionState::class.java).states
//            if (subscriptionStates.size == 1){
////                return Response.status(Response.Status.BAD_REQUEST).entity("The Subscription state Id specified is already present").build()
//                return linkedMapOf(Pair("status",Response.Status.BAD_REQUEST), Pair("message","The Subscription state Id specified is already present"))
//            }

        val tenureInYears: Long = subscriptionInput.tenure.toLong()

        val subscriptionState = SubscriptionState(
                subscriptionId,
                subscriptionInput.loanRef,
                subscriptionInput.subscriptionName,
                LocalDate.now(),
                LocalDate.now().plusYears(tenureInYears),
//                    Instant.now(),
//                    Instant.now().plusSeconds(100),
                subscriptionInput.loanAmount,
                subscriptionInput.tenure,
                subscriptionInput.termSheet,
                proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(subscriptionInput.leadArranger)) as Party, //lender cannot be null
                proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(subscriptionInput.leadArranger)) as Party,
                subscriptionInput.subscriptionAmount,
                SubscriptionState.SubscriptionStatus.CREATED
        )

        println("subscriptionState object before persisting : " + subscriptionState.toString())

        val signedTx: List<SignedTransaction> = proxy
                .startTrackedFlowDynamic(CreateSubscriptionLedgerFlow::class.java, subscriptionState, subscriptionInput.lenderA, subscriptionInput.lenderB).returnValue.get()
//            val msg:String = "The Subscription Ledgers for two lenders is successfully created"
//            return Response.status(Response.Status.CREATED).entity(msg).build()

        println("signedtransaction: " + signedTx)

        return linkedMapOf(Pair("status", Response.Status.CREATED), Pair("message", "The Subscription Ledgers for two lenders is successfully created"))
    }

    @PutMapping("/approveLenderSubscriptionByJson")
    fun approveSubscriptionLedgerJson(@RequestBody name: JsonObject,@RequestHeader(value = "PartyName") callingParty: String): LinkedHashMap<String, Any> {
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
        if (name.get("subscriptionId") == null) {
//                return Response.status(Response.Status.BAD_REQUEST).entity("The SubscriptionId cannot be null").build()
            return linkedMapOf(Pair("status", Response.Status.BAD_REQUEST), Pair("message", "The SubscriptionId cannot be null"))
        }

        val signedTx: SignedTransaction = proxy
                .startTrackedFlowDynamic(ApproveSubscriptionLedgerFlow::class.java, name.get("subscriptionId")).returnValue.get()

//            return Response.status(Response.Status.ACCEPTED).entity("Term sheet Approved").build()
        return linkedMapOf(Pair("status", Response.Status.ACCEPTED), Pair("message", "Term sheet Approved"))
    }

    @PutMapping("/approveLenderSubscriptionById")
    fun approveSubscriptionLedgerId(@RequestBody subscription: SubscriptionId,@RequestHeader(value = "PartyName") callingParty: String): LinkedHashMap<String, Any> {
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
//        if (name == null)
//            return Response.status(Response.Status.BAD_REQUEST).entity("The Loan Ref cannot be null").build()
        val signedTx: SignedTransaction = proxy
                .startTrackedFlowDynamic(ApproveSubscriptionLedgerFlow::class.java, subscription.loanRef, subscription.subscriptionAmount).returnValue.get()

//            return Response.status(Response.Status.ACCEPTED).entity("Term sheet Approved").build()
        return linkedMapOf(Pair("status", Response.Status.ACCEPTED), Pair("message", "Lender subscription Approved"))
    }

    @GetMapping("/getAllSubscriptions", produces = arrayOf("application/json"))
    fun getAllSubscriptions(@RequestHeader(value = "PartyName") callingParty: String): List<SubscriptionModel> {
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
        var allRecords: List<StateAndRef<SubscriptionState>> = proxy.vaultQuery(SubscriptionState::class.java).states
        var records: MutableList<SubscriptionModel> = mutableListOf()
        for (i in allRecords) {
            var record = i.state.data
            records.add(SubscriptionModel(record.subscriptionId, record.loanRef, record.subscriptionName, record.startDate.toString(), record.endDate.toString(), record.loanAmount, record.tenure,
                    record.termSheet, record.lender.name.organisation.toString(), record.leadArranger.name.organisation.toString(), record.subscriptionAmount, record.subscriptionStatus.toString()))
        }
        return records
    }

    @GetMapping("/getSubscriptionsById/{id}", produces = arrayOf("application/json"))
    fun getSubscriptionsById(@PathVariable("id") id: String,@RequestHeader(value = "PartyName") callingParty: String): List<SubscriptionModel> {
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
        val loadIdCriteria = builder { SubscriptionSchema1.PersistentSubsciption::loan_ref.equal(id) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(loadIdCriteria)
        val allRecords = proxy.vaultQueryByCriteria(criteria, SubscriptionState::class.java).states
        var records: MutableList<SubscriptionModel> = mutableListOf()
        for (i in allRecords) {
            val record = i.state.data
            records.add(SubscriptionModel(record.subscriptionId, record.loanRef, record.subscriptionName, record.startDate.toString(), record.endDate.toString(), record.loanAmount, record.tenure,
                    record.termSheet, record.lender.name.organisation.toString(), record.leadArranger.name.organisation.toString(), record.subscriptionAmount, record.subscriptionStatus.toString()))
        }
        return records
    }

    @GetMapping("/getSubscriptionsHistoryById/{id}", produces = arrayOf("application/json"))
    fun getSubscriptionsHistoryById(@PathVariable("id") id: String,@RequestHeader(value = "PartyName") callingParty: String): List<SubscriptionModel> {
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
        val loadIdCriteria = builder { SubscriptionSchema1.PersistentSubsciption::loan_ref.equal(id) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(loadIdCriteria, Vault.StateStatus.ALL)
        val subscriptionStates = proxy.vaultQueryByCriteria(criteria, SubscriptionState::class.java).states
        var records: MutableList<SubscriptionModel> = mutableListOf()
        for (i in subscriptionStates) {
            var record = i.state.data
            records.add(SubscriptionModel(record.subscriptionId, record.loanRef, record.subscriptionName, record.startDate.toString(), record.endDate.toString(), record.loanAmount, record.tenure,
                    record.termSheet, record.lender.name.organisation.toString(), record.leadArranger.name.organisation.toString(), record.subscriptionAmount, record.subscriptionStatus.toString()))
        }
        return records
    }

    @GetMapping("/getLoanIds", produces = arrayOf("application/json"))
    private fun getLoanIds(@RequestHeader(value = "PartyName") callingParty: String): List<String> {
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
        //val borrowerParty = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse("O=LeadArranger,L=New York,C=US")) as Party
        //Hardcoding the borrower name as cordaX500Name

        //        val borrowerName = "O=Borrower,L=London,C=GB"
        val loadIdCriteria = builder { SubscriptionSchema1.PersistentSubsciption::subscription_status.equal(SubscriptionState.SubscriptionStatus.CREATED) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(loadIdCriteria)
        val subscriptionStates = proxy.vaultQueryByCriteria(criteria, SubscriptionState::class.java).states
        println("subscriptionStates.size : " + subscriptionStates.size)
        var loanRefs: MutableList<String> = mutableListOf()
        if (subscriptionStates.size > 0) {
            for (i in subscriptionStates) {
                var record = i.state.data
                loanRefs.add(record.loanRef)
            }
        }
        return loanRefs
    }

    @GetMapping("/getAllApprovedSubscriptions", produces = arrayOf("application/json"))
    fun getAllApprovedSubscriptions(@RequestHeader(value = "PartyName") callingParty: String): List<SubscriptionModel> {
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
        var allRecords: List<StateAndRef<SubscriptionState>> = proxy.vaultQuery(SubscriptionState::class.java).states
        var records: MutableList<SubscriptionModel> = mutableListOf()
        for (i in allRecords) {
            var record = i.state.data
            if (record.subscriptionStatus.equals(SubscriptionState.SubscriptionStatus.APPROVED)) {
                records.add(SubscriptionModel(record.subscriptionId, record.loanRef, record.subscriptionName, record.startDate.toString(), record.endDate.toString(), record.loanAmount, record.tenure,
                        record.termSheet, record.lender.name.organisation.toString(), record.leadArranger.name.organisation.toString(), record.subscriptionAmount, record.subscriptionStatus.toString()))
            }
        }
        return records
    }

    @GetMapping("/getAllStaticData", produces = arrayOf("application/json"))
    fun getAllStaticData(@RequestHeader(value = "PartyName") callingParty: String): List<StaticDataModel> {
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
        var allRecords: List<StateAndRef<StaticDataState>> = proxy.vaultQuery(StaticDataState::class.java).states
        var records: MutableList<StaticDataModel> = mutableListOf()
        for (i in allRecords) {
            var record = i.state.data
            records.add(StaticDataModel(record.loanRef, record.borrower.name.organisation.toString(), record.borrowerAccountNumber, record.borrowerBank, record.borrowerLoanAmount, record.lenderA.name.organisation.toString(), record.lenderAAccountNumber, record.lenderABank, record.lenderAContribution, record.lenderB.name.organisation.toString(), record.lenderBAccountNumber, record.lenderBBank, record.lenderBContribution, record.leadArranger.name.organisation.toString(), record.leadArrangerAccountNumber, record.leadArrangerContribution, record.rateOfInterest, record.frequency, record.payingBank, record.tenure, record.startDate.toString(), record.endDate.toString()))
        }
        return records
    }

//        var allRecords: List<StateAndRef<BankState>> = proxy.vaultQuery(BankState::class.java).states
//        for(i in allRecords){
//            var singlerecord : StateAndRef<BankState> = i
//            if (singlerecord.state.data.stateId.equals(bankState.stateId))
//                return Response.status(Response.Status.BAD_REQUEST).entity("StateId already exists in the database").build();
//        }
//        val bankInput = BankState(bankState.stateId,
//                proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(bankState.issuer)),
//                bankState.faceValue.toDouble(),
//                proxy.wellKnownPartyFromX500Name((CordaX500Name.parse(bankState.owner)))as Party)


}

//    @GetMapping(value = "/getSubscriptionDetails", produces = arrayOf("application/json"))
//    fun getSubscriptionDetails(): List<SubscriptionState>{
////        var returnRecords: List<BankState> = ArrayList<BankState>()
//        var allRecords: List<StateAndRef<SubscriptionState>> = proxy.vaultQuery(SubscriptionState::class.java).states
//        return allRecords.map { it.state.data }
//    }


//    @PostMapping(value = "/createSubscription")
//    fun addSubscriptionLender(@RequestBody subscription: SubscriptionModel): Response {
//        println("subscription "+subscription)
//        return Response.status()
//    }
//
//import com.template.flows.CreateStaticDataFlow
//import com.template.schema.TermSheetSchema1
//import com.template.states.StaticDataState
//import com.template.states.SubscriptionState
//import com.template.states.TermSheetState
//import net.corda.core.identity.CordaX500Name
//import net.corda.core.identity.Party
//import net.corda.core.node.services.vault.Builder.equal
//import net.corda.core.node.services.vault.QueryCriteria
//import net.corda.core.node.services.vault.builder
//import net.corda.core.transactions.SignedTransaction
//import org.slf4j.LoggerFactory
//import org.springframework.web.bind.annotation.*
//import org.springframework.web.multipart.MultipartFile
//import javax.ws.rs.core.Response
//
//class MemberModel {
//    val loanRef: String = ""
//    val entityName: String = ""
//    val entityType: String = ""
//    val accountName: String = ""
//    val acountType: String = ""
//    val paymentAccount: Long = 0
//    val paymentBank: String = ""
////}
//
//open class SubscriptionModel {
//    val subscriptionRef: String = ""
//    val subscriptionName: String = ""
//    val startDate: String = ""
//    val endDate: String = ""
//    val size: Long = 0
//    val tenure: Int = 0
//    val termSheetRef: String = ""
//}
//
//class LenderSubscribeModel : SubscriptionModel() {
//    val lenderName: String = ""
//    val sizeToSubscribe: Int = 0
//}
//
//@RestController
//@RequestMapping("/api/subscription") // The paths for HTTP requests are relative to this base path.
//class SubscriptionController (rpc: NodeRPCConnection) {
//
//    companion object {
//        private val logger = LoggerFactory.getLogger(RestController::class.java)
//    }
//
//    private val proxy = rpc.proxy
//
////    @GetMapping(value = "/templateendpoint", produces = arrayOf("text/plain"))
////    private fun templateendpoint(): String {
////        return "Define an endpoint here."
////    }
//
////    @GetMapping(value = "/getallbankstates", produces = arrayOf("application/json"))
////    fun getAllBankStates(): List<BankState>{
//////        var returnRecords: List<BankState> = ArrayList<BankState>()
////        var allRecords: List<StateAndRef<BankState>> = proxy.vaultQuery(BankState::class.java).states
////        return allRecords.map { it.state.data }
////    }
//
//    @PostMapping(value = "/addMemberDetails")
//    fun addMemberDetails(@RequestBody member: MemberModel): Response {
////        try {
//        println("~~~Input State"+member)
//        if (member == null)
//            return Response.status(Response.Status.BAD_REQUEST).entity("The member input must not be null").build();
//
//        val loadIdCriteria = builder { TermSheetSchema1.PersistentTermSheet::loan_ref.equal(member.loanRef) }
//        val criteria = QueryCriteria.VaultCustomQueryCriteria(loadIdCriteria)
//        val termSheetStates = proxy.vaultQueryByCriteria(criteria, TermSheetState::class.java).states
//        if (termSheetStates.size == 0)
//            return Response.status(Response.Status.BAD_REQUEST).entity("The Loan Reference specified is not present").build()
//
//        var borrowerName: String = ""
//        var borrowerAccount: Long = 0
//        var borrowerBank: String = ""
//        var lenderAName: String = ""
//        var lenderAAccountNumber: Long = 0
//        var lenderABank: String = ""
//        var lenderBName: String = ""
//        var lenderBAccountNumber: Long = 0
//        var lenderBBank: String = ""
//        var leadArrangerName: String = ""
//        var leadArrangerAccountNumber: Long = 0
//        var rateOfInterest: Double = 0.0
//        var payingBank: String = ""
//
//        if (member.entityType === "borrower") {
//            borrowerName = member.entityName
//            borrowerAccount = member.paymentAccount
//            borrowerBank = member.paymentBank
//        }
//        else if (member.entityType === "lender"){
//            if (member.entityName === "LenderA"){
//                lenderAName = member.entityName
//                lenderAAccountNumber = member.paymentAccount
//                lenderABank = member.paymentBank
//            } else if (member.entityName === "LenderB"){
//                lenderBName = member.entityName
//                lenderBAccountNumber = member.paymentAccount
//                lenderBBank = member.paymentBank
//            }
//        }
//
//
//        val staticDataState = StaticDataState(
//                loanRef = member.loanRef,
//                borrower = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(borrowerName)) as Party,
//                borrowerAccountNumber = borrowerAccount,
//                borrowerBank = borrowerBank,
//                borrowerLoanAmount = 0.0,
//                lenderA = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(lenderAName)) as Party,
//                lenderAAccountNumber = lenderAAccountNumber,
//                lenderABank = lenderABank,
//                lenderB = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(lenderBName)) as Party,
//                lenderBAccountNumber = lenderBAccountNumber,
//                lenderBBank = lenderBBank,
//                leadArranger = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(leadArrangerName)) as Party,
//                leadArrangerAccountNumber = leadArrangerAccountNumber,
//                rateOfInterest = rateOfInterest, //this should be replaced with static value
//                payingBank = payingBank,
//                lenderAContribution = 5.0,
//                lenderBContribution = 10.0,
//                leadArrangerContribution = 5.0
//                )
////        var allRecords: List<StateAndRef<BankState>> = proxy.vaultQuery(BankState::class.java).states
////        for(i in allRecords){
////            var singlerecord : StateAndRef<BankState> = i
////            if (singlerecord.state.data.stateId.equals(bankState.stateId))
////                return Response.status(Response.Status.BAD_REQUEST).entity("StateId already exists in the database").build();
////        }
////        val bankInput = BankState(bankState.stateId,
////                proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(bankState.issuer)),
////                bankState.faceValue.toDouble(),
////                proxy.wellKnownPartyFromX500Name((CordaX500Name.parse(bankState.owner)))as Party)
//        println("~~~Converted static data State: "+staticDataState)
//        val signedTx: SignedTransaction = proxy
//                .startTrackedFlowDynamic(CreateStaticDataFlow::class.java,staticDataState).returnValue.get()
//        println("signedtransaction: "+signedTx)
//        val msg:String = "Member added succesfully"
//        return Response.status(Response.Status.CREATED).entity(msg).build()
//
//    }
//
////    @PostMapping(value = "/createSubscription")
////    fun addSubscriptionLender(@RequestBody subscription: SubscriptionModel): Response {
////        println("subscription "+subscription)
////        return Response.status()
////    }
////
////    @PostMapping(value = "/addLender")
////    fun addSubscriptionLender(@RequestBody lenderSubscription: LenderSubscribeModel): Response {
////        println("Term sheet file received "+termSheetFile)
////        return Response.status()
////    }
////
////    @PostMapping(value = "/approveLenderSubscription")
////    fun approveLenderSubscription(@RequestParam subscriptionRef: String, @RequestParam  status: String): Response {
////        println("approve Lender Subscription status "+status)
////        //perform a vault query to get subscription state based on the subscriptionRef
////
////
////        return Response.status()
////    }
////

//}