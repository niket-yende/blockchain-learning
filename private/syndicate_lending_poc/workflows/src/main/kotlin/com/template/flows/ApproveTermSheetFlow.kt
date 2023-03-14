package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.TermSheetContract
import com.template.schema.TermSheetSchema1
import com.template.states.TermSheetState
import net.corda.core.contracts.Command
import net.corda.core.contracts.Requirements.using
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
class ApproveTermSheetFlow(val loanRef: String) : FlowLogic<SignedTransaction>() {
    override val progressTracker: ProgressTracker = ProgressTracker(GENERATING_TRANSACTION,
            SIGNING_TRANSACTION,
            GATHERING_SIGNS,
            FINALISING_TRANSACTION)

    companion object {
        private val log: Logger = loggerFor<ApproveTermSheetFlow>()
    }

    @Suspendable
    override fun call(): SignedTransaction {
        log.info("-------------------Flow name: Approve_Term_Sheet_Flow, My name:${ourIdentity.name}-------------------")
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val loadIdCriteria = builder { TermSheetSchema1.PersistentTermSheet::loan_ref.equal(loanRef) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(loadIdCriteria)
        val termSheetStates = serviceHub.vaultService.queryBy(TermSheetState::class.java, criteria).states
        if (termSheetStates.size == 0) {
            throw FlowException(IllegalArgumentException("No Term sheet found with the given Loan Ref."))
        }

        val termSheetState = termSheetStates.get(0).state.data.copy(status = TermSheetState.TermSheetStatus.APPROVED)
        val approveCommand = Command(TermSheetContract.Commands.Approve(), listOf(ourIdentity.owningKey, termSheetState.leadArranger.owningKey))

        progressTracker.currentStep = GENERATING_TRANSACTION
        val transactionBuilder = TransactionBuilder(notary)
                .addInputState(termSheetStates.get(0))
                .addOutputState(termSheetState, TermSheetContract.ID)
                .addCommand(approveCommand)

        transactionBuilder.verify(serviceHub)

        progressTracker.currentStep = SIGNING_TRANSACTION
        val stx = serviceHub.signInitialTransaction(transactionBuilder)

        progressTracker.currentStep = FINALISING_TRANSACTION
        val otherPartySession = initiateFlow(termSheetState.leadArranger)
        val fullySignedTx = subFlow(CollectSignaturesFlow(stx, setOf(otherPartySession), GATHERING_SIGNS.childProgressTracker()))
        return subFlow(FinalityFlow(fullySignedTx, listOf(otherPartySession)))
    }
}

@InitiatedBy(ApproveTermSheetFlow::class)
class ApproveTermSheetFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an Approve term sheet transaction." using (output is TermSheetState)
                val termSheetState = output as TermSheetState
//                "I won't accept IOUs with a value over 100." using (termSheetState.value <= 100)
            }
        }
        val txId = subFlow(signTransactionFlow).id
        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}