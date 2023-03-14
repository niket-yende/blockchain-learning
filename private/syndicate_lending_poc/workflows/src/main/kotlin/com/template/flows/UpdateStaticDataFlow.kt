package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.StaticDataContract
import com.template.schema.StaticDataSchema1
import com.template.states.StaticDataState
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
class UpdateStaticDataFlow(val staticDataState: StaticDataState) : FlowLogic<SignedTransaction>(){

    override val progressTracker: ProgressTracker = ProgressTracker(GENERATING_TRANSACTION,
                                                                    SIGNING_TRANSACTION,
                                                                    FINALISING_TRANSACTION)
    companion object {
        private val log: Logger = loggerFor<UpdateStaticDataFlow>()
    }

    @Suspendable
    override fun call(): SignedTransaction {
        log.info("-------------------Flow name: Update_Static_Data_Flow, My name:${ourIdentity.name}---------------------")

//        if (!staticDataState.leadArranger.name.toString().equals(ourIdentity.name.toString())) {
//            throw FlowException("Only Lead Arranger can call this UpdateStaticDataFlow, Current Identity is + " + ourIdentity.name.toString())
//        }
        // Getting Notary from the Network Map
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // Create the state from the input data
        val outputStaticDataState = staticDataState

        val stateIDCriteria = builder { StaticDataSchema1.PersistentStaticData:: loan_ref.equal(outputStaticDataState.loanRef) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(stateIDCriteria)
        val statisDataStates = serviceHub.vaultService.queryBy(StaticDataState::class.java, criteria).states
        requireThat {
            "No Static Data State exists with the given id" using (statisDataStates.size > 0)
        }

        val inputStaticDataState = statisDataStates[0]

        // Create Command : It is used by contract to check the constraints for creating the OrderRequest
        val createCommand = Command(StaticDataContract.Commands.Create(), listOf(ourIdentity.owningKey));

        // Building the transcation by specifying the output state, command and Contract ID to validate the transaction
        progressTracker.currentStep = GENERATING_TRANSACTION
        val transactionBuilder = TransactionBuilder(notary)
                .addInputState(inputStaticDataState)
                .addOutputState(outputStaticDataState, StaticDataContract.ID)
                .addCommand(createCommand)

        // verifying the transaction by checking the contract conditions are met or not
        transactionBuilder.verify(serviceHub)

        // Transaction signed by the initiator party
        progressTracker.currentStep = SIGNING_TRANSACTION
        val stx = serviceHub.signInitialTransaction(transactionBuilder)

        val borrowerSession = initiateFlow(staticDataState.borrower)
        val leadArrangerSession = initiateFlow(staticDataState.leadArranger)

        progressTracker.currentStep=FINALISING_TRANSACTION

        println("staticDataState id check "+ourIdentity.name.toString())
        if (staticDataState.borrower.name.toString().equals(ourIdentity.name.toString())) {
            val transaction = subFlow(FinalityFlow(stx, listOf(leadArrangerSession),FINALISING_TRANSACTION.childProgressTracker()))
            return transaction
        } else if(staticDataState.leadArranger.name.toString().equals(ourIdentity.name.toString())){
            val transaction = subFlow(FinalityFlow(stx, listOf(borrowerSession),FINALISING_TRANSACTION.childProgressTracker()))
            return transaction
        } else {
            val transaction = subFlow(FinalityFlow(stx, listOf(),FINALISING_TRANSACTION.childProgressTracker()))
            return transaction
        }
    }
}


@InitiatedBy(UpdateStaticDataFlow::class)
class UpdateStaticDataFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}