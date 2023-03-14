package com.template.webserver

import com.template.Models.InterestPaymentModel
import com.template.flows.CreateInterestPaymentFlow
import com.template.flows.PayInterestFlow
import com.template.schema.BidSchema1
import com.template.schema.InterestPaymentSchema1
import com.template.schema.LoanLedgerSchema1
import com.template.schema.StaticDataSchema1
import com.template.states.*
import net.corda.core.contracts.StateAndRef
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
@RequestMapping("/api/interestPayment") // The paths for HTTP requests are relative to this base path.
class InterestPaymentController (val rpc: NodeRPCConnection){

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

//    private val proxy = rpc.proxy

    @PostMapping(value = "/create",produces = arrayOf("application/json"))
    fun createInterestAmount(@RequestHeader(value = "PartyName") callingParty: String): LinkedHashMap<String,Any> {
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
        var map = LinkedHashMap<String,Any>()
        println("Calling Party in create: "+callingParty)
        var totalLoanCount: Int = 0
        var processedLoans: Int = 0



        val dateCriteria = builder { StaticDataSchema1.PersistentStaticData::payment_date.equal(LocalDate.now().dayOfMonth) }
//        val endDateCriteria = builder { StaticDataSchema1.PersistentStaticData::end_date.greaterThanOrEqual(LocalDate.now()) }
        var criteria = QueryCriteria.VaultCustomQueryCriteria(dateCriteria)

        val staticDataStates = proxy.vaultQueryByCriteria(criteria, StaticDataState::class.java).states

        if (staticDataStates.size == 0){
            return linkedMapOf(Pair("status",Response.Status.BAD_REQUEST), Pair("message","There's no static data present for this date"), Pair("data",{}))
        }
        println("static data size: "+staticDataStates.size)

        for (staticDataRef in staticDataStates){
            var staticData = staticDataRef.state.data
            var paymentDate = staticData.paymentDate
            var frequency = staticData.frequency
            var loanRef = staticData.loanRef
            println("static data :"+staticData.toString())
            var currentDate = LocalDate.now().dayOfMonth
            println("day of month :"+currentDate)

            if (staticData.paymentDate == currentDate) {
                val loanLedgerCriteria1 = builder { LoanLedgerSchema1.PersistentLoanLedger::status.equal(LoanLedgerState.LedgerStatus.UPDATED) }
                val loanLedgerCriteria2 = builder { LoanLedgerSchema1.PersistentLoanLedger::status.equal(LoanLedgerState.LedgerStatus.OUTSTANDING_AMT_UPDATED) }
                val loanLedgerCriteria3 = QueryCriteria.VaultCustomQueryCriteria(loanLedgerCriteria1).or(QueryCriteria.VaultCustomQueryCriteria(loanLedgerCriteria2))
                val loanCriteria = builder { LoanLedgerSchema1.PersistentLoanLedger::loan_ref.equal(loanRef) }
                var criteria = loanLedgerCriteria3.and(QueryCriteria.VaultCustomQueryCriteria(loanCriteria))

                val loanLedgerStates = proxy.vaultQueryByCriteria(criteria, LoanLedgerState::class.java).states

                if (loanLedgerStates.size == 0){
                    return linkedMapOf(Pair("status",Response.Status.BAD_REQUEST), Pair("message","There's no loan ledger data present for this loanRef"), Pair("data",{}))
                }

                totalLoanCount += 1

                val loanLedgerState = loanLedgerStates.get(0).state.data

                //Below value is a CordaX500Name
                var leadArrangerName:String = "O=LeadArranger, L=New York, C=US"

                val signedTx: SignedTransaction = proxy
                        .startTrackedFlowDynamic(CreateInterestPaymentFlow::class.java, loanLedgerState.loanRef, leadArrangerName).returnValue.get()
                println("signedtransaction: " + signedTx)

                if (signedTx != null){
                    processedLoans += 1
                }
            }
        }

        map["processed_count"] = processedLoans
        map["total_count"] = totalLoanCount

        println("totalLoanCount : "+totalLoanCount+" processedLoans : "+processedLoans)
        if (processedLoans == totalLoanCount){
            return linkedMapOf(Pair("status",Response.Status.CREATED), Pair("message","Interest payment states have been created for all these loans"), Pair("data",map))
        }

        return linkedMapOf(Pair("status",Response.Status.BAD_REQUEST), Pair("message","Some of the loans could not be processed for interest payment state"), Pair("data",map))
    }

    @PostMapping(value = "/pay")
    fun payInterestAmount(@RequestBody loanRef: LoanRef, @RequestHeader(value = "PartyName") callingParty: String): LinkedHashMap<String,Any> {
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
//        try {
        println("~~~Input State" + loanRef.loanRef)
//        if (loanRef == null)
//            return linkedMapOf(Pair("status",Response.Status.BAD_REQUEST), Pair("message","loanRef input cannot be null"), Pair("data",{}))

//            return Response.status(Response.Status.BAD_REQUEST).entity("The loanRef input must not be null").build();

        //Check if the loanRef is present in the InterestPaymentState
        val loanIdCriteria = builder { InterestPaymentSchema1.PersistentInterestPayment::loan_reference.equal(loanRef.loanRef) }
        val interestPaymentCriteria = QueryCriteria.VaultCustomQueryCriteria(loanIdCriteria)
        val interestPaymentStates = proxy.vaultQueryByCriteria(interestPaymentCriteria, InterestPaymentState::class.java).states
        if (interestPaymentStates.size == 0)
            return linkedMapOf(Pair("status",Response.Status.BAD_REQUEST), Pair("message","There's no Interest Payment present for the given Loan Reference Id"), Pair("data",{}))

//            return Response.status(Response.Status.BAD_REQUEST).entity("The Loan Reference specified is not present").build()

        var lastIndex : Int = interestPaymentStates.lastIndex
        println("latsIndex value "+ lastIndex)
        var lastInterestPaymentState : InterestPaymentState = interestPaymentStates.get(lastIndex).state.data.copy(paymentStatus = InterestPaymentState.PaymentStatus.BORROWER_PAID)
        println("Interest Payment state: "+lastInterestPaymentState.toString())
        val signedTx: List<SignedTransaction> = proxy
                .startTrackedFlowDynamic(PayInterestFlow::class.java, lastInterestPaymentState).returnValue.get()
        println("signedtransaction: " + signedTx)

        val msg:String = "Interest payment state updated to PAYMENT_INITIATED"
        var map = HashMap<String,Any>()
        map["loanRef"] = loanRef.loanRef

        return linkedMapOf(Pair("status",Response.Status.CREATED), Pair("message","Interest payment state updated to PAYMENT_INITIATED"), Pair("data",map))
//        return Response.status(Response.Status.OK).entity(msg).build()
    }

    @GetMapping("/getAllInterestPayments",produces = arrayOf("application/json"))
    private fun getAllInterestPayments(@RequestHeader(value = "PartyName") callingParty: String): List<InterestPaymentState>{
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
        var allRecords: List<StateAndRef<InterestPaymentState>> = proxy.vaultQuery(InterestPaymentState::class.java).states
        return allRecords.map { it.state.data }
    }

    @GetMapping("/getAllAccounts",produces = arrayOf("application/json"))
    private fun getAllAccounts(@RequestHeader(value = "PartyName") callingParty: String): List<AccountState>{
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
        var allRecords: List<StateAndRef<AccountState>> = proxy.vaultQuery(AccountState::class.java).states
        return allRecords.map { it.state.data }
    }

    @GetMapping("/getInterestPaymentsById/{id}",produces = arrayOf("application/json"))
    private fun getInterestPaymentsById(@PathVariable("id") id:String,@RequestHeader(value = "PartyName") callingParty: String): InterestPaymentModel {
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
        val loadIdCriteria = builder { InterestPaymentSchema1.PersistentInterestPayment::loan_reference.equal(id) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(loadIdCriteria)
        val termSheetStates = proxy.vaultQueryByCriteria(criteria, InterestPaymentState::class.java).states
        val record = termSheetStates.last().state.data
        return InterestPaymentModel(record.loanRef,record.paymentDate.toString(),record.paymentAccount,record.borrowerBank,record.leadArrangerAccount,record.leadArrangerBank,record.interestObligation,
                record.paymentStatus.toString(),record.borrower.name.organisation.toString(),record.leadArranger.name.organisation.toString(),record.lenderA.name.organisation.toString(),
                record.lenderB.name.organisation.toString(),record.txIdsList)
    }

    @GetMapping("/getPaidInterestPaymentsById/{id}",produces = arrayOf("application/json"))
    private fun getPaidInterestPaymentsById(@PathVariable("id") id:String,@RequestHeader(value = "PartyName") callingParty: String): MutableList<InterestPaymentModel> {
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
        val loadIdCriteria1 = builder { InterestPaymentSchema1.PersistentInterestPayment::loan_reference.equal(id) }
        val loadIdCriteria2 = builder { InterestPaymentSchema1.PersistentInterestPayment::payment_status.equal(InterestPaymentState.PaymentStatus.LENDERB_PAYMENT_COMPLETE) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(loadIdCriteria1).and(QueryCriteria.VaultCustomQueryCriteria(loadIdCriteria2))
        val termSheetStates = proxy.vaultQueryByCriteria(criteria, InterestPaymentState::class.java).states
        var returnPayments : MutableList<InterestPaymentModel> = mutableListOf()
        for (i in termSheetStates){
            val record = i.state.data
            returnPayments.add(InterestPaymentModel(record.loanRef,record.paymentDate.toString(),record.paymentAccount,record.borrowerBank,record.leadArrangerAccount,record.leadArrangerBank,record.interestObligation,
                    record.paymentStatus.toString(),record.borrower.name.organisation.toString(),record.leadArranger.name.organisation.toString(),record.lenderA.name.organisation.toString(),
                    record.lenderB.name.organisation.toString(),record.txIdsList))
        }
        return returnPayments
//        val record = termSheetStates.last().state.data
//        return InterestPaymentModel(record.loanRef,record.paymentDate.toString(),record.paymentAccount,record.borrowerBank,record.leadArrangerAccount,record.leadArrangerBank,record.interestObligation,
//                record.paymentStatus.toString(),record.borrower.name.organisation.toString(),record.leadArranger.name.organisation.toString(),record.lenderA.name.organisation.toString(),
//                record.lenderB.name.organisation.toString(),record.txIdsList)
    }

    @GetMapping("/getPaidInterestPayments",produces = arrayOf("application/json"))
    private fun getPaidInterestPayments(@RequestHeader(value = "PartyName") callingParty: String): MutableList<InterestPaymentModel> {
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
//        val loadIdCriteria1 = builder { InterestPaymentSchema1.PersistentInterestPayment::loan_reference.equal(id) }
        val loadIdCriteria2 = builder { InterestPaymentSchema1.PersistentInterestPayment::payment_status.equal(InterestPaymentState.PaymentStatus.LENDERB_PAYMENT_COMPLETE) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(loadIdCriteria2)
        val termSheetStates = proxy.vaultQueryByCriteria(criteria, InterestPaymentState::class.java).states
        var returnPayments : MutableList<InterestPaymentModel> = mutableListOf()
        for (i in termSheetStates){
            val record = i.state.data
            returnPayments.add(InterestPaymentModel(record.loanRef,record.paymentDate.toString(),record.paymentAccount,record.borrowerBank,record.leadArrangerAccount,record.leadArrangerBank,record.interestObligation,
                    record.paymentStatus.toString(),record.borrower.name.organisation.toString(),record.leadArranger.name.organisation.toString(),record.lenderA.name.organisation.toString(),
                    record.lenderB.name.organisation.toString(),record.txIdsList))
        }
        return returnPayments
//        val record = termSheetStates.last().state.data
//        return InterestPaymentModel(record.loanRef,record.paymentDate.toString(),record.paymentAccount,record.borrowerBank,record.leadArrangerAccount,record.leadArrangerBank,record.interestObligation,
//                record.paymentStatus.toString(),record.borrower.name.organisation.toString(),record.leadArranger.name.organisation.toString(),record.lenderA.name.organisation.toString(),
//                record.lenderB.name.organisation.toString(),record.txIdsList)
    }

    @GetMapping("/getInterestPaymentsHistoryById/{id}",produces = arrayOf("application/json"))
    private fun getInterestPaymentsHistoryById(@PathVariable("id") id:String,@RequestHeader(value = "PartyName") callingParty: String): List<StateAndRef<InterestPaymentState>>{
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
        val loadIdCriteria = builder { InterestPaymentSchema1.PersistentInterestPayment::loan_reference.equal(id) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(loadIdCriteria, Vault.StateStatus.ALL)
        val termSheetStates = proxy.vaultQueryByCriteria(criteria, InterestPaymentState::class.java).states
        return termSheetStates.map { it }
    }

    @GetMapping("/getLoanIds",produces = arrayOf("application/json"))
    private fun getInterestLoanIds(@RequestHeader(value = "PartyName") callingParty: String): List<String>{
        val proxy = rpc.myRPCMap.get(callingParty)?.proxy as CordaRPCOps
        //val borrowerParty = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse("O=LeadArranger,L=New York,C=US")) as Party
        //Hardcoding the borrower name as cordaX500Name

        //        val borrowerName = "O=Borrower,L=London,C=GB"
        val loadIdCriteria1 = builder { InterestPaymentSchema1.PersistentInterestPayment::payment_status.equal(InterestPaymentState.PaymentStatus.CREATED) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(loadIdCriteria1)
        val loanLedgerStates = proxy.vaultQueryByCriteria(criteria, InterestPaymentState::class.java).states
        println("interestPaymentStates.size : "+loanLedgerStates.size)
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