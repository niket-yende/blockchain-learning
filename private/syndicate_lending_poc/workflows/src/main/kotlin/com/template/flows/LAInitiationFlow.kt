package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.TemplateContract
import com.template.states.InitiationState
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger

@InitiatingFlow
@StartableByRPC
class LAInitiationFlow(val initiationState: InitiationState) : FlowLogic<SignedTransaction>() {
    /* Progress Tracker : A progress tracker helps surface information about the progress of an operation to a user interface or API of some kind.
     * It lets you define a set of steps that represent an operation. A step is represented by an object (typically a singleton)
     * ProgressTracker Steps are mentioned in the ProgressTrackerSteps.kt of the cordapp-common package.
     */
    override val progressTracker: ProgressTracker = ProgressTracker(GENERATING_TRANSACTION,
            SIGNING_TRANSACTION,
            GATHERING_SIGNS,
            FINALISING_TRANSACTION)

    companion object {
        private val log: Logger = loggerFor<LAInitiationFlow>()
    }

    @Suspendable
    override fun call(): SignedTransaction {
        log.info("-------------------Flow name: LAInitiationFlow, My name:${ourIdentity.name}---------------------")
        // Getting Notary from the Network Map
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // Create the state from the input data
        val initiationOutputState = initiationState

        // Building the transcation by specifying the output state, command and Contract ID to validate the transaction
        progressTracker.currentStep = GENERATING_TRANSACTION
        val transactionBuilder = TransactionBuilder(notary)
                .addOutputState(initiationOutputState, TemplateContract.ID)
                .addCommand(TemplateContract.Commands.Action(), listOf(initiationState.leadArranger.owningKey))

        // verifying the transaction by checking the contract conditions are met or not
        transactionBuilder.verify(serviceHub)

        // Transaction signed by the initiator party
        progressTracker.currentStep = SIGNING_TRANSACTION
        val stx = serviceHub.signInitialTransaction(transactionBuilder)

        progressTracker.currentStep = FINALISING_TRANSACTION
        val transaction = subFlow(FinalityFlow(stx, listOf(), FINALISING_TRANSACTION.childProgressTracker()))

        return transaction
    }
}