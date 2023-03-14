package com.template.webserver

import com.template.Models.BidModel
import com.template.Models.TermSheetModel
import com.template.flows.CreateBidStateFlow
import com.template.flows.UpdateBidStateFlow
import com.template.schema.BidSchema1
import com.template.schema.SubscriptionSchema1
import com.template.schema.TermSheetSchema1
import com.template.states.BidState
import com.template.states.SubscriptionState
import com.template.states.TermSheetState
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.Builder.equal
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.transactions.SignedTransaction
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.LocalDate
import java.util.HashMap
import javax.ws.rs.core.Response

@RestController
@RequestMapping("/api/bid") // The paths for HTTP requests are relative to this base path.
class BidController (val rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

//    private val proxy = rpc.proxy

    @PostMapping(value = "/confirmBorrowerConsent")
    fun confirmBorrowerConsent(@RequestBody loanRef: LoanRef,@RequestHeader(value = "PartyName") callingParty: String): LinkedHashMap<String,Any> {
        println("approve borrower consent status for "+loanRef.loanRef)
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
        //perform a vault query to get subscription state based on the subscriptionRef

        val loanIdCriteria = builder { BidSchema1.PersistentBid::loan_ref.equal(loanRef.loanRef) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(loanIdCriteria)
        val subscriptionStates = proxy.vaultQueryByCriteria(criteria, BidState::class.java).states
        if (subscriptionStates.size == 0)
            return linkedMapOf(Pair("status",Response.Status.BAD_REQUEST), Pair("message","The Loan Reference specified is not present"), Pair("data",{}))

//            return Response.status(Response.Status.BAD_REQUEST).entity("The Loan Reference specified is not present").build()

        var approveBidState = subscriptionStates.get(0).state.data.copy(bidStatus = BidState.BidStatus.APPROVED)

        val signedTx: SignedTransaction = proxy.startTrackedFlowDynamic(UpdateBidStateFlow::class.java,approveBidState).returnValue.get()
        println("signedtransaction: " + signedTx)

        val msg: String = "Bid status is provided consent by the borrower"
        var map = HashMap<String,Any>()
        map["loanRef"] = loanRef.loanRef

        return linkedMapOf(Pair("status",Response.Status.CREATED), Pair("message","Consent provided by the borrower"), Pair("data",map))
//        return Response.status(Response.Status.OK).entity(msg).build()
    }

    @PostMapping(value = "/confirmBid")
    fun confirmBid(@RequestBody loanRef: LoanRef,@RequestHeader(value = "PartyName") callingParty: String): LinkedHashMap<String,Any> {
        println("loanRef "+loanRef)
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
        //perform a vault query to get bid state based on the subscriptionRef

        println("~~~Input State" + loanRef)
//        if (loanRef.length > 0)
//            return linkedMapOf(Pair("status",Response.Status.BAD_REQUEST), Pair("message","The loanRef input must not be null"), Pair("data",{}))

//            return Response.status(Response.Status.BAD_REQUEST).entity("The loanRef input must not be null").build();

        //lender names are hardcoded
        val lenderAName: String = "O=LenderA,L=New York,C=US"
        val lenderBName: String = "O=LenderB,L=New York,C=US"
        val borrowerName: String = "O=Borrower,L=London,C=GB"

//        val loanRefCriteria = builder { SubscriptionSchema1.PersistentSubsciption::loan_ref.equal(loanRef) }
//        val lenderCriteria = builder { SubscriptionSchema1.PersistentSubsciption::lender_name.equal(lenderAName) }
//        val statusCriteria = builder { SubscriptionSchema1.PersistentSubsciption:: subscription_status.equal(SubscriptionState.SubscriptionStatus.APPROVED) }
//        val criteria1 = QueryCriteria.VaultCustomQueryCriteria(loanRefCriteria).and(QueryCriteria.VaultCustomQueryCriteria(lenderCriteria))
//                //.and(QueryCriteria.VaultCustomQueryCriteria(statusCriteria))
//
//        val subscriptionStates1 = proxy.vaultQueryByCriteria(criteria1, SubscriptionState::class.java).states
//        if (subscriptionStates1.size == 0)
//            return linkedMapOf(Pair("status",Response.Status.BAD_REQUEST), Pair("message","The Loan Reference specified is not present"), Pair("data",{}))
////            return Response.status(Response.Status.BAD_REQUEST).entity("The Loan Reference specified is not present").build()
//
//        var subscriptionStateA=subscriptionStates1.get(0).state.data
//
//        val loanRefCriteria1 = builder { SubscriptionSchema1.PersistentSubsciption::loan_ref.equal(loanRef) }
//        val lenderCriteria1 = builder { SubscriptionSchema1.PersistentSubsciption::lender_name.equal(lenderBName) }
//        val statusCriteria1 = builder { SubscriptionSchema1.PersistentSubsciption:: subscription_status.equal(SubscriptionState.SubscriptionStatus.APPROVED) }
//        val criteria2 = QueryCriteria.VaultCustomQueryCriteria(loanRefCriteria1).and(QueryCriteria.VaultCustomQueryCriteria(lenderCriteria1)).and(QueryCriteria.VaultCustomQueryCriteria(statusCriteria1))
//
//        val subscriptionStates2 = proxy.vaultQueryByCriteria(criteria2, SubscriptionState::class.java).states
//        if (subscriptionStates2.size == 0)
//            return linkedMapOf(Pair("status",Response.Status.BAD_REQUEST), Pair("message","The Loan Reference specified is not present"), Pair("data",{}))
//
//
////            return Response.status(Response.Status.BAD_REQUEST).entity("The Loan Reference specified is not present").build()
//
//        var subscriptionStateB=subscriptionStates2.get(0).state.data

        val loanRefCriteria = builder { SubscriptionSchema1.PersistentSubsciption::loan_ref.equal(loanRef.loanRef) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(loanRefCriteria)
        val subscriptionStates = proxy.vaultQueryByCriteria(criteria,SubscriptionState::class.java).states
        var count:Int = 0
        var subscriptionStateA : SubscriptionState = SubscriptionState("","","", LocalDate.now(), LocalDate.now(),0.0,0.0,"",
                proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(lenderAName)) as Party,proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(lenderAName)) as Party,
                1.0,SubscriptionState.SubscriptionStatus.CREATED)
        var subscriptionStateB : SubscriptionState= SubscriptionState("","","", LocalDate.now(), LocalDate.now(),0.0,0.0,"",
        proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(lenderAName)) as Party,proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(lenderAName)) as Party,
        1.0,SubscriptionState.SubscriptionStatus.CREATED)

        for (i in subscriptionStates){
            if (i.state.data.subscriptionStatus.equals(SubscriptionState.SubscriptionStatus.APPROVED))
                count = count + 1
            if (i.state.data.lender.equals(proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(lenderAName))))
                subscriptionStateA=i.state.data
            if (i.state.data.lender.equals(proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(lenderBName))))
                subscriptionStateB=i.state.data
        }
        println("Approval count : "+count)
        if (count < 2)
            return linkedMapOf(Pair("status",Response.Status.BAD_REQUEST), Pair("message","One or more lenders didnot approve their Subscription ledger."), Pair("data",{}))

        val borrower = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(borrowerName)) as Party

        var bidState: BidState = BidState(loanRef.loanRef,subscriptionStateA.subscriptionId,subscriptionStateA.subscriptionName,subscriptionStateA.startDate,
                subscriptionStateA.endDate,subscriptionStateA.loanAmount,subscriptionStateA.tenure,subscriptionStateA.lender.name.organisation.toString(),
                subscriptionStateA.subscriptionAmount,subscriptionStateB.lender.name.organisation.toString(),subscriptionStateB.subscriptionAmount,
                BidState.BidStatus.CREATED,borrower,
                subscriptionStateA.leadArranger)

        val signedTx: List<SignedTransaction> = proxy.startTrackedFlowDynamic(CreateBidStateFlow::class.java,bidState).returnValue.get()
                        println("signedtransaction: " + signedTx)

        val msg: String = "Bid status is provided consent by the borrower"

        var map = HashMap<String,Any>()
        map["loanRef"] = loanRef.loanRef

        return linkedMapOf(Pair("status",Response.Status.CREATED), Pair("message","Bid send to the borrower for consent"), Pair("data",map))
//        return Response.status(Response.Status.OK).entity(msg).build()
    }

    @GetMapping("/getAllBidState",produces = arrayOf("application/json"))
    private fun getAllBidState(@RequestHeader(value = "PartyName") callingParty: String): List<BidModel>{
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
        var allRecords: List<StateAndRef<BidState>> = proxy.vaultQuery(BidState::class.java).states
        var records : MutableList<BidModel> = mutableListOf()
        for (i in allRecords){
            var record = i.state.data
            records.add(BidModel(record.loanRef,record.subscriptionId,record.subscriptionName,record.startDate.toString(),record.endDate.toString(),record.loanAmount,record.tenure,
                    record.lenderAName,record.lenderASubsAmount,record.lenderBName,record.lenderBSubsAmount,record.bidStatus.toString(),record.borrower.name.organisation.toString(),
                    record.leadArranger.name.organisation.toString()))
        }
        return records
    }

    @GetMapping("/getBidStateById/{id}",produces = arrayOf("application/json"))
    private fun getBidStateById(@PathVariable("id") id:String,@RequestHeader(value = "PartyName") callingParty: String): BidModel {
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
        val loadIdCriteria = builder { BidSchema1.PersistentBid::loan_ref.equal(id) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(loadIdCriteria)
        val termSheetStates = proxy.vaultQueryByCriteria(criteria, BidState::class.java).states
        val record = termSheetStates.get(0).state.data
        return BidModel(record.loanRef,record.subscriptionId,record.subscriptionName,record.startDate.toString(),record.endDate.toString(),record.loanAmount,record.tenure,
                record.lenderAName,record.lenderASubsAmount,record.lenderBName,record.lenderBSubsAmount,record.bidStatus.toString(),record.borrower.name.organisation.toString(),
                record.leadArranger.name.organisation.toString())
    }

    @GetMapping("/getBidStateHistoryById/{id}",produces = arrayOf("application/json"))
    private fun getBidStateHistoryById(@PathVariable("id") id:String,@RequestHeader(value = "PartyName") callingParty: String): List<BidModel>{
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
        val loadIdCriteria = builder { BidSchema1.PersistentBid::loan_ref.equal(id) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(loadIdCriteria, Vault.StateStatus.ALL)
        val termSheetStates = proxy.vaultQueryByCriteria(criteria, BidState::class.java).states
        var records : MutableList<BidModel> = mutableListOf()
        for (i in termSheetStates){
            var record = i.state.data
            records.add(BidModel(record.loanRef,record.subscriptionId,record.subscriptionName,record.startDate.toString(),record.endDate.toString(),record.loanAmount,record.tenure,
                    record.lenderAName,record.lenderASubsAmount,record.lenderBName,record.lenderBSubsAmount,record.bidStatus.toString(),record.borrower.name.organisation.toString(),
                    record.leadArranger.name.organisation.toString()))
        }
        return records
    }

}