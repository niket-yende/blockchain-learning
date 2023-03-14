package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.StaticDataContract
import com.template.schema.StaticDataSchema1
import com.template.states.StaticDataState
import com.template.states.TermSheetState
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
class CreateStaticDataFlow(val staticDataState: StaticDataState) : FlowLogic<SignedTransaction>() {

    override val progressTracker: ProgressTracker = ProgressTracker(GENERATING_TRANSACTION,
            SIGNING_TRANSACTION,
            FINALISING_TRANSACTION)

    companion object {
        private val log: Logger = loggerFor<CreateStaticDataFlow>()
    }

    @Suspendable
    override fun call(): SignedTransaction {
        log.info("-------------------Flow name: Create_Static_Data_Flow, My name:${ourIdentity.name}---------------------")
        // Getting Notary from the Network Map
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // Create the state from the input data
        val staticDataState = staticDataState

        val stateIDCriteria = builder { StaticDataSchema1.PersistentStaticData::loan_ref.equal(staticDataState.loanRef) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(stateIDCriteria)
        val staticDataStates = serviceHub.vaultService.queryBy(StaticDataState::class.java, criteria).states
        requireThat {
            "Static Data State already exists with the given id" using (staticDataStates.size == 0)
        }

        // Create Command : It is used by contract to check the constraints for creating the OrderRequest
        val createCommand = Command(StaticDataContract.Commands.Create(), listOf(ourIdentity.owningKey));

        // Building the transcation by specifying the output state, command and Contract ID to validate the transaction
        progressTracker.currentStep = GENERATING_TRANSACTION
        val transactionBuilder = TransactionBuilder(notary)
                .addOutputState(staticDataState, StaticDataContract.ID)
                .addCommand(createCommand)

        // verifying the transaction by checking the contract conditions are met or not
        transactionBuilder.verify(serviceHub)


        // Transaction signed by the initiator party
        progressTracker.currentStep = SIGNING_TRANSACTION
        val stx = serviceHub.signInitialTransaction(transactionBuilder)

        val borrowerSession = initiateFlow(staticDataState.borrower)

        progressTracker.currentStep = FINALISING_TRANSACTION
        val transaction = subFlow(FinalityFlow(stx, listOf(borrowerSession), FINALISING_TRANSACTION.childProgressTracker()))

        return transaction
    }
}

@InitiatedBy(CreateStaticDataFlow::class)
class CreateStaticDataFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}