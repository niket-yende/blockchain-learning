package com.template.webserver

import com.template.flows.CreateLoanLedgerFlow
import com.template.flows.UpdateStaticDataFlow
import com.template.schema.*
import com.template.states.*
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.transactions.SignedTransaction
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.ws.rs.core.Response
import kotlin.collections.LinkedHashMap

class LoanLedgerModel (
        var loanRef: String,
        var loanName: String,
        var loanType: String,
        var principal: Double,
        var tenure: Int,
        var interestRateType: String,
        var interestRate: Double,
        var frequency: String,
        var paymentDate: Int,
        var collateral: String,
        var arranger_account: String
){
    constructor():this(
            "","", "",0.0,0,"", 0.0, "",0,"",""
    )
}

@RestController
@RequestMapping("/api/loan") // The paths for HTTP requests are relative to this base path.
class LoanIssuanceController (val rpc: NodeRPCConnection){

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

//    private val proxy = rpc.proxy

//    @GetMapping(value = "/templateendpoint", produces = arrayOf("text/plain"))
//    private fun templateendpoint(): String {
//        return "Define an endpoint here."
//    }

    //Get all the metadata for a given loanRef
    /*@GetMapping(value = "getStateMetadata/{loanRef}", produces = arrayOf("application/json"))
    fun getStateMetadata(@PathVariable("loanRef") loanRef:String): List<StateAndRef<StaticDataState>>{
        println("loan ref Id "+loanRef)
        val stateIDCriteria = builder { StaticDataSchema1.PersistentStaticData::loan_ref.equal(loanRef) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(stateIDCriteria, Vault.StateStatus.ALL)
        val accountStates = proxy.vaultQueryByCriteria(criteria, StaticDataState::class.java).states
        return accountStates.map { it }
    } */

    @PostMapping(value = "/createLoanLedger")
    fun createLoanLedger(@RequestBody loanLedger: LoanLedgerModel,@RequestHeader(value = "PartyName") callingParty: String): LinkedHashMap<String,Any> {
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
//        try {
        println("~~~Input State" + loanLedger.toString())
        val response: JSONObject = JSONObject()

        if (loanLedger == null){
//            response.put("status",Response.Status.BAD_REQUEST)
//            response.put("message","The loanLedger input must not be null")
            return linkedMapOf(Pair("status",Response.Status.BAD_REQUEST), Pair("message","The loanLedger input must not be null"))
        }

          //  return Response.status(Response.Status.BAD_REQUEST).entity("The loanLedger input must not be null").build();

//        val loadIdCriteria = builder { TermSheetSchema1.PersistentTermSheet::loan_ref.equal(member.loanRef) }
//        val criteria = QueryCriteria.VaultCustomQueryCriteria(loadIdCriteria)
//        val termSheetStates = proxy.vaultQueryByCriteria(criteria, TermSheetState::class.java).states
//        if (termSheetStates.size == 0)
//            return Response.status(Response.Status.BAD_REQUEST).entity("The Loan Reference specified is not present").build()

        //Check if the loanRef is present in the staticDataState
        val loanIdCriteria = builder { StaticDataSchema1.PersistentStaticData::loan_ref.equal(loanLedger.loanRef) }
        val static_criteria = QueryCriteria.VaultCustomQueryCriteria(loanIdCriteria)
        val staticDataStates = proxy.vaultQueryByCriteria(static_criteria, StaticDataState::class.java).states
        println("staticDataStates.size : "+staticDataStates.size)
        if (staticDataStates.size == 0) {
            return linkedMapOf(Pair("status",Response.Status.BAD_REQUEST), Pair("message","The Loan Reference specified is not present for static data"))
        }

        var staticDataState = staticDataStates.get(0).state.data

        //Create LoanLedgerState
        val loanLedgerState = LoanLedgerState(staticDataState.borrower, staticDataState.lenderA, staticDataState.lenderB, staticDataState.leadArranger, staticDataState.loanRef, staticDataState.borrowerLoanAmount, staticDataState.borrowerAccountNumber,  staticDataState.leadArrangerAccountNumber, "", "", "", 0.0,0.0,0.0,LoanLedgerState.LedgerStatus.CREATED)

        //Create BorrowerAccount
        val borrowerAccountState = AccountState(staticDataState.loanRef, staticDataState.borrowerAccountNumber, staticDataState.borrower, staticDataState.borrowerLoanAmount, Arrays.asList(""))


        val signedTx1: List<SignedTransaction> = proxy
                .startTrackedFlowDynamic(CreateLoanLedgerFlow::class.java, loanLedgerState, borrowerAccountState).returnValue.get()
        println("signedtransaction: " + signedTx1)

        if (signedTx1.size < 2) {

            return linkedMapOf(Pair("status",Response.Status.BAD_REQUEST), Pair("message","Failed to create loan ledger state or update static data"))
            // return Response.status(Response.Status.BAD_REQUEST).entity(msg).build()
        }

        //Get contribution amount for LA, LenderA & LenderB
        //Perform a vault query on SubscriptionState to get contribution amount
        var loanRefCriteria = builder { SubscriptionSchema1.PersistentSubsciption:: loan_ref.equal(loanLedger.loanRef) }
        //Subscription criteria is eliminated since one loan_ref will have a unique subscription Id
        //var subscriptionIdCriteria = builder { SubscriptionSchema1.PersistentSubsciption:: subscription_id.equal("XXAS") }
//        var statusCriteria = QueryCriteria.VaultCustomQueryCriteria(builder { SubscriptionSchema1.PersistentSubsciption:: subscription_status.equal(SubscriptionState.SubscriptionStatus.APPROVED) })
//        var criteria = QueryCriteria.VaultCustomQueryCriteria(loanRefCriteria).and(QueryCriteria.VaultCustomQueryCriteria(lenderCriteria))

        println("Frequency value : "+loanLedger.frequency+ "payment date : "+loanLedger.paymentDate)

        //Update staticDataState with frequency
        val staticDataState1 = staticDataState.copy(frequency = loanLedger.frequency, paymentDate = loanLedger.paymentDate)  //payment date added to the static data
        val signedTx2: SignedTransaction = proxy
                .startTrackedFlowDynamic(UpdateStaticDataFlow::class.java, staticDataState1).returnValue.get()
        println("staticDataState signedtransaction: " + signedTx1)

        val msg:String = "Loan ledger & borrower account is successfully created"
       // return Response.status(Response.Status.CREATED).entity(msg).build()

        return linkedMapOf(Pair("status",Response.Status.CREATED), Pair("message","Loan ledger & borrower account is successfully created"))
    }



    //This api will return details of an account given a loanRef
    @GetMapping(value = "/checkAccountBalance/{loanRef}", produces = arrayOf("application/json"))
    fun checkAccountBalance(@PathVariable("loanRef") loanRef:String,@RequestHeader(value = "PartyName") callingParty: String): LinkedHashMap<String,Any>{
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
//        var returnRecords: List<BankState> = ArrayList<BankState>()
        //get the calling party details
        val requestingParty: Party = proxy.nodeInfo().legalIdentities.get(0)

        println("Requesting party name "+requestingParty.name)

        //Check if the loanRef is present in the staticDataState
        val loanIdCriteria = builder { AccountSchema1.PersistentAccount::loan_ref.equal(loanRef) }
        val nameCriteria = builder { AccountSchema1.PersistentAccount:: account_owner.equal(requestingParty.name.toString()) }

//        var criteria = QueryCriteria.VaultCustomQueryCriteria(loanIdCriteria).and(QueryCriteria.VaultCustomQueryCriteria(nameCriteria))
        var criteria = QueryCriteria.VaultCustomQueryCriteria(loanIdCriteria)

        val accountStates = proxy.vaultQueryByCriteria(criteria, AccountState::class.java).states
        if (accountStates.size == 0) {
            return linkedMapOf(Pair("status",Response.Status.BAD_REQUEST), Pair("message","No account for the specified criteria"))
        }

        val accountState: AccountState = accountStates.last().state.data

        println("AccountState object : "+accountState.toString())

        /*val loanIdCriteria = builder { StaticDataSchema1.PersistentStaticData::loan_ref.equal(loanRef) }

        var criteria = QueryCriteria.VaultCustomQueryCriteria(loanIdCriteria)

        val staticDataStates = proxy.vaultQueryByCriteria(criteria, StaticDataState::class.java).states
        if (staticDataStates.size == 0) {
            return linkedMapOf(Pair("status",Response.Status.BAD_REQUEST), Pair("message","No account for the specified criteria"))
        }

        val staticDataState: StaticDataState = staticDataStates.get(0).state.data

        val accountMap = LinkedHashMap<String,Any>()
        val requestingParty1:String = requestingParty.name.toString()
        println("requestingParty1 : "+requestingParty1)
        if (requestingParty1.equals("O=Borrower, L=London, C=GB")){
               accountMap["accountId"] = staticDataState.borrowerAccountNumber
        } else if (requestingParty1.equals("O=LeadArranger, L=New York, C=US")){
            accountMap["accountId"] = staticDataState.leadArrangerAccountNumber
        } else if (requestingParty1.equals("O=LenderA, L=New York, C=US")){
            accountMap["accountId"] = staticDataState.lenderAAccountNumber
        } else if (requestingParty1.equals("O=LenderB, L=New York, C=US")){
            accountMap["accountId"] = staticDataState.lenderBAccountNumber
        } else {
            accountMap["accountId"] = "Wrong requester"
        } */

        val accountMap = LinkedHashMap<String,Any>()
        accountMap["accountId"] = accountState.accountId
        accountMap["balance"] = accountState.balance

        return accountMap
    }


    @GetMapping(value = "/getLoanIssuanceDetails/{loanRef}", produces = arrayOf("application/json"))
    fun getLoanIssuanceDetails(@PathVariable("loanRef") loanRef:String,@RequestHeader(value = "PartyName") callingParty: String): LinkedHashMap<String,Any>{
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps

        //Check if the loanRef is present in the InitiationState
        /*val loanIdCriteria1 = builder { InitiationSchema1.PersistentInitiation::loan_ref.equal(loanRef) }

        var criteria1 = QueryCriteria.VaultCustomQueryCriteria(loanIdCriteria1)

        val initiationStates = proxy.vaultQueryByCriteria(criteria1, InitiationState::class.java).states
        if (initiationStates.size == 0) {
            return linkedMapOf(Pair("status",Response.Status.BAD_REQUEST), Pair("message","No initiation state for the specified loanRef"))
        }
        val initiationState: InitiationState = initiationStates.get(0).state.data */


        val loanIdCriteria2 = builder { StaticDataSchema1.PersistentStaticData::loan_ref.equal(loanRef) }

        var criteria2 = QueryCriteria.VaultCustomQueryCriteria(loanIdCriteria2)

        val staticDataStates = proxy.vaultQueryByCriteria(criteria2, StaticDataState::class.java).states
        if (staticDataStates.size == 0) {
            return linkedMapOf(Pair("status",Response.Status.BAD_REQUEST), Pair("message","No staticData state for the specified loanRef"))
        }
        val staticDataState: StaticDataState = staticDataStates.get(0).state.data

        val loanLedgerModel = LoanLedgerModel(loanRef,"Borrower - Tranche 1","Senior Loan",staticDataState.borrowerLoanAmount,staticDataState.tenure,"Fixed",staticDataState.rateOfInterest,staticDataState.frequency,25,"Not Applicable",staticDataState.leadArrangerAccountNumber)

        val loanMap = LinkedHashMap<String,Any>()
        loanMap["status"] = Response.Status.OK
        loanMap["loan_detail"] = loanLedgerModel

        return loanMap
    }

    @GetMapping("getAllLoanLedgers",produces = arrayOf("application/json"))
    fun getAllLoanLedgers(@RequestHeader(value = "PartyName") callingParty: String): List<LoanLedgerModel>{
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
        var allRecords: List<StateAndRef<LoanLedgerState>> = proxy.vaultQuery(LoanLedgerState::class.java).states
        var records : MutableList<LoanLedgerModel> = mutableListOf()
        for (i in allRecords){
            var record = i.state.data

            val loadIdCriteria = builder { StaticDataSchema1.PersistentStaticData::loan_ref.equal(record.loanRef) }
            val criteria = QueryCriteria.VaultCustomQueryCriteria(loadIdCriteria)
            val staticDataStates = proxy.vaultQueryByCriteria(criteria, StaticDataState::class.java).states
            println("staticDataStates.size : "+staticDataStates.size)
            if (staticDataStates.size == 0) {
                println("staticDataStates found empty")
                return records
            }
            val staticDataState = staticDataStates.get(0).state.data
            records.add(LoanLedgerModel(record.loanRef,"Borrower - Tranche 1","Senior Loan",staticDataState.borrowerLoanAmount,staticDataState.tenure,"Fixed",staticDataState.rateOfInterest,staticDataState.frequency,staticDataState.paymentDate,"Not Applicable",staticDataState.leadArrangerAccountNumber))
        }
        return records
    }


    @GetMapping("/getLoanIds",produces = arrayOf("application/json"))
    private fun getLoanIds(@RequestHeader(value = "PartyName") callingParty: String): List<String>{
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
        //val borrowerParty = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse("O=LeadArranger,L=New York,C=US")) as Party
        //Hardcoding the borrower name as cordaX500Name

        //        val borrowerName = "O=Borrower,L=London,C=GB"
        val loadIdCriteria1 = builder { LoanLedgerSchema1.PersistentLoanLedger::status.equal(LoanLedgerState.LedgerStatus.UPDATED) }
        val loadIdCriteria2 = builder { LoanLedgerSchema1.PersistentLoanLedger::status.equal(LoanLedgerState.LedgerStatus.OUTSTANDING_AMT_UPDATED) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(loadIdCriteria1).or(QueryCriteria.VaultCustomQueryCriteria(loadIdCriteria2))
        val loanLedgerStates = proxy.vaultQueryByCriteria(criteria, LoanLedgerState::class.java).states
        println("loanLedgerStates.size : "+loanLedgerStates.size)
        var loanRefs : MutableList<String> = mutableListOf()
        if (loanLedgerStates.size > 0) {
            for (i in loanLedgerStates){
                var record = i.state.data
                loanRefs.add(record.loanRef)
            }
        }
        return loanRefs
    }
}