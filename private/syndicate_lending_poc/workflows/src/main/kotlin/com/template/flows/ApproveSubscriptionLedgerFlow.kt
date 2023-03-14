package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.SubscriptionContract
import com.template.schema.SubscriptionSchema1
import com.template.states.SubscriptionState
import net.corda.core.contracts.Command
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
class ApproveSubscriptionLedgerFlow(val loanRef: String, val subscriptionAmount: Double) : FlowLogic<SignedTransaction>() {
    override val progressTracker: ProgressTracker = ProgressTracker(GENERATING_TRANSACTION,
            SIGNING_TRANSACTION,
            FINALISING_TRANSACTION)

    companion object {
        private val log: Logger = loggerFor<ApproveSubscriptionLedgerFlow>()
    }

    @Suspendable
    override fun call(): SignedTransaction {
        log.info("-------------------Flow name: Approve_Subscription_Ledger_Flow, My name:${ourIdentity.name}-------------------")
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

//        val subscriptionIdCriteria = builder { SubscriptionSchema1.PersistentSubsciption::subscription_id.equal(subscriptionId) }
//        val criteria = QueryCriteria.VaultCustomQueryCriteria(subscriptionIdCriteria)
//        var subscriptionStates = serviceHub.vaultService.queryBy(SubscriptionState::class.java, criteria).states
//        if (subscriptionStates.size == 0) {
//            throw FlowException(IllegalArgumentException("No Subscription State found with the given subscription id"))
//        }
        val laonRefCriteria1 = builder { SubscriptionSchema1.PersistentSubsciption:: loan_ref.equal(loanRef) }
        val criteria2 = QueryCriteria.VaultCustomQueryCriteria(laonRefCriteria1)
        val subscriptionStates = serviceHub.vaultService.queryBy(SubscriptionState::class.java, criteria2).states
        if (subscriptionStates.size == 0){
            throw IllegalArgumentException("No Subscription State found with the given loan ref")
        }

        var subscriptionState = subscriptionStates.get(0).state.data.copy(subscriptionStatus = SubscriptionState.SubscriptionStatus.APPROVED, subscriptionAmount = subscriptionAmount)
        val approveCommand = Command(SubscriptionContract.Commands.ApproveSubscription(), listOf(ourIdentity.owningKey))

        progressTracker.currentStep = GENERATING_TRANSACTION
        val transactionBuilder = TransactionBuilder(notary)
                .addInputState(subscriptionStates.get(0))
                .addOutputState(subscriptionState, SubscriptionContract.ID)
                .addCommand(approveCommand)

        transactionBuilder.verify(serviceHub)

        progressTracker.currentStep = SIGNING_TRANSACTION
        val stx = serviceHub.signInitialTransaction(transactionBuilder)

        val leadArrangerSession = initiateFlow(subscriptionState.leadArranger)

        progressTracker.currentStep = FINALISING_TRANSACTION

        return subFlow(FinalityFlow(stx, listOf(leadArrangerSession), FINALISING_TRANSACTION.childProgressTracker()))
    }
}

@InitiatedBy(ApproveSubscriptionLedgerFlow::class)
class ApproveSubscriptionLedgerFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}