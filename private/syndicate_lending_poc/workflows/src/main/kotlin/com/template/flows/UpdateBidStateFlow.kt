package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.BidContract
import com.template.schema.BidSchema1
import com.template.states.BidState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger

@InitiatingFlow
@StartableByRPC
class UpdateBidStateFlow(val bidState: BidState) : FlowLogic<SignedTransaction>() {

    /* Progress Tracker : A progress tracker helps surface information about the progress of an operation to a user interface or API of some kind.
         * It lets you define a set of steps that represent an operation. A step is represented by an object (typically a singleton)
         * ProgressTracker Steps are mentioned in the ProgressTrackerSteps.kt of the cordapp-common package.
         */
    override val progressTracker: ProgressTracker = ProgressTracker(GENERATING_TRANSACTION,
            SIGNING_TRANSACTION,
            GATHERING_SIGNS,
            FINALISING_TRANSACTION)

    companion object {
        private val log: Logger = loggerFor<UpdateBidStateFlow>()
    }

    @Suspendable
    override fun call(): SignedTransaction {
        log.info("-------------------Flow name: Update_Static_Data_Flow, My name:${ourIdentity.name}---------------------")
        // Getting Notary from the Network Map
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // Create the state from the input data
        val outputApproveBidState = bidState

        val bidCriteria = builder { BidSchema1.PersistentBid:: loan_ref.equal(outputApproveBidState.loanRef) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(bidCriteria)
        val approveBidStates = serviceHub.vaultService.queryBy(BidState::class.java, criteria).states
        requireThat {
            "No Bid exists with the given id" using (approveBidStates.size > 0)
        }

        val inputApproveBidState = approveBidStates.get(0)

        // Create Command : It is used by contract to check the constraints for creating the OrderRequest
        val approveCommand = Command(BidContract.Commands.Approved(), listOf(ourIdentity.owningKey));

        // Building the transcation by specifying the output state, command and Contract ID to validate the transaction
        progressTracker.currentStep = GENERATING_TRANSACTION
        val transactionBuilder = TransactionBuilder(notary)
                .addInputState(inputApproveBidState)
                .addOutputState(outputApproveBidState, BidContract.ID)
                .addCommand(approveCommand)

        // verifying the transaction by checking the contract conditions are met or not
        transactionBuilder.verify(serviceHub)

        // Transaction signed by the initiator party
        progressTracker.currentStep = SIGNING_TRANSACTION
        val stx = serviceHub.signInitialTransaction(transactionBuilder)

        val leadArrangerSession = initiateFlow(inputApproveBidState.state.data.leadArranger)
        progressTracker.currentStep = FINALISING_TRANSACTION
        val transaction = subFlow(FinalityFlow(stx, listOf(leadArrangerSession), FINALISING_TRANSACTION.childProgressTracker()))

        return transaction
    }
}

@InitiatedBy(UpdateBidStateFlow::class)
class UpdateBidStateFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
//        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
//            override fun checkTransaction(stx: SignedTransaction) = requireThat {
//                val output = stx.tx.outputs.single().data
//                "This must be an Approve Bid transaction." using (output is BidState)
////                val termSheetState = output as BidState
////                "I won't accept IOUs with a value over 100." using (termSheetState.value <= 100)
//            }
//        }
//        val txId = subFlow(signTransactionFlow).id
        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}