package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.SubscriptionContract
import com.template.schema.SubscriptionSchema1
import com.template.schema.TermSheetSchema1
import com.template.states.StaticDataState
import com.template.states.SubscriptionState
import com.template.states.TermSheetState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger

@StartableByRPC
@InitiatingFlow
class CreateSubscriptionLedgerFlow(val subscriptionState: SubscriptionState, val lenderAName: String, val lenderBName: String) : FlowLogic<List<SignedTransaction>>() {

    override val progressTracker: ProgressTracker = ProgressTracker(GENERATING_TRANSACTION,
            SIGNING_TRANSACTION,
            FINALISING_TRANSACTION)

    companion object {
        private val log: Logger = loggerFor<CreateSubscriptionLedgerFlow>()
    }

    @Suspendable
    override fun call(): List<SignedTransaction> {
        var transactions = mutableListOf<SignedTransaction>()

        log.info("-------------------Flow name: Create_Subscription_Ledger_Flow, My name:${ourIdentity.name}---------------------")
        // Getting Notary from the Network Map
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val lenderAParty = serviceHub.networkMapCache.getPeerByLegalName(CordaX500Name.parse(lenderAName)) as Party
        val lenderBParty = serviceHub.networkMapCache.getPeerByLegalName(CordaX500Name.parse(lenderBName)) as Party

        // Create the state from the input data
        var subscriptionState = subscriptionState

//        val subscriptionIDCriteria = builder { SubscriptionSchema1.PersistentSubsciption::subscription_id.equal(subscriptionState.subscriptionId) }
//        var criteria = QueryCriteria.VaultCustomQueryCriteria(subscriptionIDCriteria)
//        val subscriptionStates = serviceHub.vaultService.queryBy(StaticDataState::class.java, criteria).states
//        if (subscriptionStates.size == 1)
//            throw  IllegalArgumentException("Subscription State already exists with the given Subscription Id")

        val laonRefCriteria1 = builder { SubscriptionSchema1.PersistentSubsciption:: loan_ref.equal(subscriptionState.loanRef) }
        val criteria2 = QueryCriteria.VaultCustomQueryCriteria(laonRefCriteria1)
        val initiationStates1 = serviceHub.vaultService.queryBy(SubscriptionState::class.java, criteria2).states
        if (initiationStates1.size == 1){
            throw IllegalArgumentException("Subscription State already exists with the given Loan Ref")
        }


        val loanRefCriteria = builder { TermSheetSchema1.PersistentTermSheet::loan_ref.equal(subscriptionState.loanRef) }
        val criteria1 = QueryCriteria.VaultCustomQueryCriteria(loanRefCriteria)
        val termSheetStates = serviceHub.vaultService.queryBy(TermSheetState::class.java, criteria1).states

        if (termSheetStates.size == 0)
            throw  IllegalArgumentException("No static data found")

        var termSheetState: TermSheetState = termSheetStates.get(0).state.data
        requireThat {
//            "No loan with the given loan ref" using (termSheetStates.size > 0)
//            var termSheetState = termSheetStates.get(0).state.data
            "Term Sheet not yet approved by borrower" using (termSheetState.status == TermSheetState.TermSheetStatus.APPROVED)
        }

        var subscriptionStateA = subscriptionState.copy(lender = lenderAParty)
        // Create Command : It is used by contract to check the constraints for creating the OrderRequest
        var createCommand = Command(SubscriptionContract.Commands.Create(), listOf(ourIdentity.owningKey, lenderAParty.owningKey))

        // Building the transcation by specifying the output state, command and Contract ID to validate the transaction
        progressTracker.currentStep = GENERATING_TRANSACTION
        var transactionBuilderForLenderA = TransactionBuilder(notary)
                .addOutputState(subscriptionStateA, SubscriptionContract.ID)
                .addCommand(createCommand)
                .addAttachment(SecureHash.parse(termSheetState.fileHash))

        // verifying the transaction by checking the contract conditions are met or not
        transactionBuilderForLenderA.verify(serviceHub)

        // Transaction signed by the initiator party
        progressTracker.currentStep = SIGNING_TRANSACTION
        val stxA = serviceHub.signInitialTransaction(transactionBuilderForLenderA)

        val lenderASession = initiateFlow(lenderAParty)

        val fullySignedTxA = subFlow(CollectSignaturesFlow(stxA, listOf(lenderASession), CollectSignaturesFlow.tracker()))

        progressTracker.currentStep = FINALISING_TRANSACTION
        val transactionA = subFlow(FinalityFlow(fullySignedTxA, listOf(lenderASession), FINALISING_TRANSACTION.childProgressTracker()))
        transactions.add(transactionA)

        var subscriptionStateB = subscriptionState.copy(lender = lenderBParty)
        // Create Command : It is used by contract to check the constraints for creating the OrderRequest
        createCommand = Command(SubscriptionContract.Commands.Create(), listOf(ourIdentity.owningKey, lenderBParty.owningKey));

        // Building the transcation by specifying the output state, command and Contract ID to validate the transaction
        progressTracker.currentStep = GENERATING_TRANSACTION
        var transactionBuilderForLenderB = TransactionBuilder(notary)
                .addOutputState(subscriptionStateB, SubscriptionContract.ID)
                .addCommand(createCommand)

        // verifying the transaction by checking the contract conditions are met or not
        transactionBuilderForLenderB.verify(serviceHub)

        // Transaction signed by the initiator party
        progressTracker.currentStep = SIGNING_TRANSACTION
        val stxB = serviceHub.signInitialTransaction(transactionBuilderForLenderB)

        val lenderBSession = initiateFlow(lenderBParty)

        val fullySignedTxB = subFlow(CollectSignaturesFlow(stxB, listOf(lenderBSession), CollectSignaturesFlow.tracker()))

        progressTracker.currentStep = FINALISING_TRANSACTION
        val transactionB = subFlow(FinalityFlow(fullySignedTxB, listOf(lenderBSession), FINALISING_TRANSACTION.childProgressTracker()))
        transactions.add(transactionB)

        return transactions
    }
}

@InitiatedBy(CreateSubscriptionLedgerFlow::class)
class CreateSubscriptionLedgerFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be a Subscription transaction." using (output is SubscriptionState)
                val subscriptionState = output as SubscriptionState
            }
        }
        val txId = subFlow(signTransactionFlow).id
        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}