package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.InterestPaymentContract
import com.template.schema.LoanLedgerSchema1
import com.template.schema.StaticDataSchema1
import com.template.states.InterestPaymentState
import com.template.states.LoanLedgerState
import com.template.states.StaticDataState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
import java.time.Instant
import java.time.LocalDate
import java.util.*
import kotlin.math.pow

@InitiatingFlow
@StartableByRPC
class CreateInterestPaymentFlow(val loanRef: String, val leadArrangerName: String) : FlowLogic<SignedTransaction>() {

    override val progressTracker: ProgressTracker = ProgressTracker(GENERATING_TRANSACTION,
            SIGNING_TRANSACTION,
            FINALISING_TRANSACTION)

    companion object {
        private val log: Logger = loggerFor<CreateInterestPaymentFlow>()
    }

    @Suspendable
    override fun call(): SignedTransaction {
        log.info("-------------------Flow name: CreateInterestPaymentFlow, My name:${ourIdentity.name}---------------------")
        // Getting Notary from the Network Map
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

//        require(CordaX500Name.parse(leadArrangerName) == ourIdentity.name) {
//        }

        if (!leadArrangerName.equals(ourIdentity.name.toString())) {
            throw FlowException("Only Lead Arranger can call this flow  " + ourIdentity.name.toString())
        }
        //If there is an outstanding balance, only then create a InterestPaymentState

        val ledgerIDCriteria = builder { LoanLedgerSchema1.PersistentLoanLedger::loan_ref.equal(loanRef) }
        val ledgerCriteria = QueryCriteria.VaultCustomQueryCriteria(ledgerIDCriteria)
        val loanLedgerStates = serviceHub.vaultService.queryBy(LoanLedgerState::class.java, ledgerCriteria).states
        requireThat {
            "No LoanLedger exists with the given id" using (loanLedgerStates.size > 0)
            "No Outstanding loan" using (loanLedgerStates[0].state.data.outstandingLoan > 0.0)
        }

        //compute the obligation amount based on Data from static Data

        //The interestPaymentState is created by LA by performing vault query to get static Data
        val stateIDCriteria = builder { StaticDataSchema1.PersistentStaticData::loan_ref.equal(loanRef) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(stateIDCriteria)
        val staticDataStates = serviceHub.vaultService.queryBy(StaticDataState::class.java, criteria).states
        requireThat {
            "No Static Data exists with the given id" using (staticDataStates.size > 0)
        }

        val inputStaticDataState = staticDataStates.get(0).state.data

        //Calculate the loan Obligation

        val principal = inputStaticDataState.borrowerLoanAmount
        val annualInterestRate = inputStaticDataState.rateOfInterest
        val duration = inputStaticDataState.tenure
        val frequency = inputStaticDataState.frequency.toLowerCase()

        var numberOfInstalments = 0
        var periodicalInterest = 0.0

        if (frequency.equals("monthly")) {
            numberOfInstalments = duration * 12
            periodicalInterest = annualInterestRate / 1200

        } else if (frequency.equals("quarterly")) {
            numberOfInstalments = duration * 4
            periodicalInterest = annualInterestRate / 400

        } else if (frequency.equals("yearly")) {
            numberOfInstalments = duration
            periodicalInterest = annualInterestRate / 100
        }

        var paymentObligation  = (principal+(principal*periodicalInterest))/numberOfInstalments
        println("Obligation: " + paymentObligation)
        paymentObligation = Math.round(paymentObligation * 100.0) / 100.0

        //val paymentObligation = (principal * (periodicalInterest * (1 + periodicalInterest).pow(numberOfInstalments))) / ((1 + periodicalInterest).pow(numberOfInstalments - 1))

        println("Obligation upto 2 decimal places : " + paymentObligation)
        val interestPaymentState = InterestPaymentState(loanRef = loanRef,
                paymentDate = LocalDate.now(),
                paymentAccount = inputStaticDataState.borrowerAccountNumber,
                borrowerBank = inputStaticDataState.borrowerBank,
                leadArrangerAccount = inputStaticDataState.leadArrangerAccountNumber,
                leadArrangerBank = inputStaticDataState.payingBank,
                interestObligation = paymentObligation,
                paymentStatus = InterestPaymentState.PaymentStatus.CREATED,
                borrower = inputStaticDataState.borrower,
                leadArranger = inputStaticDataState.leadArranger,
                lenderA = inputStaticDataState.lenderA,
                lenderB = inputStaticDataState.lenderB,
                txIdsList = mutableListOf())

        // Create Command : It is used by contract to check the constraints for creating the OrderRequest
        val createCommand = Command(InterestPaymentContract.Commands.Create(), listOf(ourIdentity.owningKey));

        // Building the transcation by specifying the output state, command and Contract ID to validate the transaction
        progressTracker.currentStep = GENERATING_TRANSACTION
        val transactionBuilder = TransactionBuilder(notary)
                .addOutputState(interestPaymentState, InterestPaymentContract.ID)
                .addCommand(createCommand)

        // verifying the transaction by checking the contract conditions are met or not
        transactionBuilder.verify(serviceHub)

        // Transaction signed by the initiator party
        progressTracker.currentStep = SIGNING_TRANSACTION
        val stx = serviceHub.signInitialTransaction(transactionBuilder)

        val borrowerSession = initiateFlow(inputStaticDataState.borrower)
        val lenderASession = initiateFlow(inputStaticDataState.lenderA)
        val lenderBSession = initiateFlow(inputStaticDataState.lenderB)

        progressTracker.currentStep = FINALISING_TRANSACTION
        val transaction = subFlow(FinalityFlow(stx, listOf(borrowerSession, lenderASession, lenderBSession), FINALISING_TRANSACTION.childProgressTracker()))

        return transaction
    }
}

@InitiatedBy(CreateInterestPaymentFlow::class)
class CreateInterestPaymentFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
//        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
//            override fun checkTransaction(stx: SignedTransaction) = requireThat {
//                val output = stx.tx.outputs.single().data
//                "This must be an Interest Payment transaction." using (output is InterestPaymentState)
//                val termSheetState = output as InterestPaymentState
////                "I won't accept IOUs with a value over 100." using (termSheetState.value <= 100)
//            }
//        }
//        val txId = subFlow(signTransactionFlow).id
        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}