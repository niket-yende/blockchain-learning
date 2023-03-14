package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.AccountContract
import com.template.contracts.LoanLedgerContract
import com.template.contracts.StaticDataContract
import com.template.schema.BidSchema1
import com.template.schema.LoanLedgerSchema1
import com.template.schema.StaticDataSchema1
import com.template.states.AccountState
import com.template.states.BidState
import com.template.states.LoanLedgerState
import com.template.states.StaticDataState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
import java.util.*

@StartableByRPC
@InitiatingFlow
@SchedulableFlow
class LoanAllocationFlow(val dataArray: Array<Any>) : FlowLogic<SignedTransaction>() {
    val loanRef = dataArray[0] as String
    val leadArrangerName = dataArray[1] as CordaX500Name

    override val progressTracker: ProgressTracker = ProgressTracker(GENERATING_TRANSACTION,
            SIGNING_TRANSACTION,
            FINALISING_TRANSACTION)

    companion object {
        private val log: Logger = loggerFor<LoanAllocationFlow>()
    }

    @Suspendable
    override fun call(): SignedTransaction {
        println("Reached loan allocation")
        LoanAllocationFlow.log.info("-------------------Flow name: LoanAllocationFlow, My name:${ourIdentity.name}---------------------")

//        var transactions = mutableListOf<SignedTransaction>()

//        require(leadArrangerName == ourIdentity.name) {
//            "Only Lead Arranger can call this flow"
//        }
        if (!leadArrangerName.toString().equals(ourIdentity.name.toString())) {
            throw FlowException("Only Lead Arranger can call this flow, Current Identity is + " + ourIdentity.name.toString())
        }

        println("Step 1 complete")

        // Getting Notary from the Network Map
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        //Query Static data to get the Lenders Account Ids
        val stateIDCriteria = builder { StaticDataSchema1.PersistentStaticData::loan_ref.equal(loanRef) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(stateIDCriteria)
        val staticDataStates = serviceHub.vaultService.queryBy(StaticDataState::class.java, criteria).states
        requireThat {
            "No Static Data exists with the given id" using (staticDataStates.size > 0)
        }

        val inputStaticDataState = staticDataStates[0].state.data

        //Query ApproveBids data to get the Lenders Contributions
        val approveIDCriteria = builder { BidSchema1.PersistentBid::loan_ref.equal(loanRef) }
        val approveCriteria = QueryCriteria.VaultCustomQueryCriteria(approveIDCriteria)
        val approveDataStates = serviceHub.vaultService.queryBy(BidState::class.java, approveCriteria).states
        requireThat {
            "No Approved Bid exists with the given id" using (approveDataStates.size > 0)
        }
        val approveDataState = approveDataStates.get(0).state.data


        //Query LoanLedger data to get the loan ledger details
        val loanLedgerCriteria = builder { LoanLedgerSchema1.PersistentLoanLedger::loan_ref.equal(loanRef) }
        val loanCriteria = QueryCriteria.VaultCustomQueryCriteria(loanLedgerCriteria)
        val loanLedgerStates = serviceHub.vaultService.queryBy(LoanLedgerState::class.java, loanCriteria).states
        requireThat {
            "No loanLedger  exists with the given id" using (loanLedgerStates.size > 0)
        }

        println("Step 2 complete")

        val loanLedgerState = loanLedgerStates.get(0).state.data

        println("loanLedgerState Before : "+loanLedgerState.toString())

        val lenderAPercentage = (approveDataState.lenderASubsAmount / inputStaticDataState.borrowerLoanAmount) * 100
        val lenderBPercentage = (approveDataState.lenderBSubsAmount / inputStaticDataState.borrowerLoanAmount) * 100
        val leadArrangerPercentage = ((inputStaticDataState.borrowerLoanAmount - (approveDataState.lenderASubsAmount + approveDataState.lenderBSubsAmount)) / inputStaticDataState.borrowerLoanAmount) * 100


        //Update loanLedger with Lenders and LA Account IDs and their individual contribution percentage
        val outputLoanLedgerState = loanLedgerState.copy(status = LoanLedgerState.LedgerStatus.UPDATED,
                lenderAAccountId = inputStaticDataState.lenderAAccountNumber,
                lenderBAccountId = inputStaticDataState.lenderBAccountNumber,
                lenderAPercentage = lenderAPercentage,
                lenderBPercentage = lenderBPercentage,
                leadArrangerPercentage = leadArrangerPercentage)

        println("outputLoanLedgerState After : "+outputLoanLedgerState.toString())

        println("Step 3 complete")


        val updateLoanLedgerCommand = Command(LoanLedgerContract.Commands.Update(), listOf(ourIdentity.owningKey));
//        val accountCommand = Command(AccountContract.Commands.Create(), listOf(ourIdentity.owningKey));
//        val staticDataCommand = Command(StaticDataContract.Commands.Update(), listOf(ourIdentity.owningKey));

        // Building the transcation by specifying the output state, command and Contract ID to validate the transaction
        progressTracker.currentStep = GENERATING_TRANSACTION

        val transactionBuilder1 = TransactionBuilder(notary)
                .addInputState(loanLedgerStates.get(0))
                .addOutputState(outputLoanLedgerState, LoanLedgerContract.ID)
                .addCommand(updateLoanLedgerCommand)

        // verifying the transaction by checking the contract conditions are met or not
        transactionBuilder1.verify(serviceHub)

        println("Step 4 complete")

        // Transaction signed by all the participants
        progressTracker.currentStep = SIGNING_TRANSACTION

        val stx1 = serviceHub.signInitialTransaction(transactionBuilder1)

        val borrowerSession = initiateFlow(loanLedgerState.borrower)
        val lenderASession = initiateFlow(loanLedgerState.lenderA)
        val lenderBSession = initiateFlow(loanLedgerState.lenderB)

        progressTracker.currentStep = FINALISING_TRANSACTION

        val transaction1 = subFlow(FinalityFlow(stx1, listOf(borrowerSession, lenderASession, lenderBSession), FINALISING_TRANSACTION.childProgressTracker()))

//        transactions.add(transaction1)

        return transaction1
    }
}

@InitiatedBy(LoanAllocationFlow::class)
class LoanAllocationFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        println("Step 5 complete - reached responder flow")
        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}