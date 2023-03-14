package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.TermSheetContract
import com.template.schema.InitiationSchema1
import com.template.schema.TermSheetSchema1
import com.template.states.InitiationState
import com.template.states.TermSheetState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.SecureHash
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

@InitiatingFlow
@StartableByRPC
class CreateTermSheetFlow(val loanRef: String, val fileHash: String, val leadArranger: Party) : FlowLogic<SignedTransaction>() {

    override val progressTracker: ProgressTracker = ProgressTracker(GENERATING_TRANSACTION,
            SIGNING_TRANSACTION,
            GATHERING_SIGNS,
            FINALISING_TRANSACTION)

    companion object {
        private val log: Logger = loggerFor<CreateTermSheetFlow>()
    }

    @Suspendable
    override fun call(): SignedTransaction {
        log.info("-------------------Flow name: CreateTermSheetFlow, My name:${ourIdentity.name}---------------------")
        // Getting Notary from the Network Map
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val borrowerName = "O=Borrower, L=London, C=GB"

        //Get the loan details from initiation state
        val loanRefCriteria = builder { InitiationSchema1.PersistentInitiation::loan_ref.equal(loanRef) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(loanRefCriteria)
        val initiationStates = serviceHub.vaultService.queryBy(InitiationState::class.java, criteria).states
        requireThat {
            "No Initiation State exists with the given id" using (initiationStates.size > 0)
        }

        val loanRefCriteria1 = builder { TermSheetSchema1.PersistentTermSheet::loan_ref.equal(loanRef) }
        val criteria1 = QueryCriteria.VaultCustomQueryCriteria(loanRefCriteria1)
        val initiationStates1 = serviceHub.vaultService.queryBy(TermSheetState::class.java, criteria1).states
        if (initiationStates1.size == 1) {
            throw IllegalArgumentException("The file hash has already been uploaded for this loanref")
        }

        val initiationState = initiationStates.get(0).state.data

        val finalHash = SecureHash.parse(fileHash)

        val termSheetState = TermSheetState(loanRef,
                serviceHub.networkMapCache.getPeerByLegalName(CordaX500Name.parse(borrowerName)) as Party,
                initiationState.loanType,
                leadArranger,
                TermSheetState.TermSheetStatus.CREATED,
                finalHash.toString()
        )

        // Create Command : It is used by contract to check the constraints for creating the OrderRequest
        val createCommand = Command(TermSheetContract.Commands.Create(), listOf(ourIdentity.owningKey, termSheetState.borrower.owningKey))

        // Building the transcation by specifying the output state, command and Contract ID to validate the transaction
        progressTracker.currentStep = GENERATING_TRANSACTION
        val transactionBuilder = TransactionBuilder(notary)
                .addOutputState(termSheetState, TermSheetContract.ID)
                .addCommand(createCommand)
                .addAttachment(finalHash)

        // verifying the transaction by checking the contract conditions are met or not
        transactionBuilder.verify(serviceHub)

        // Transaction signed by the initiator party
        progressTracker.currentStep = SIGNING_TRANSACTION
        val partSignedTx = serviceHub.signInitialTransaction(transactionBuilder)

        val otherPartySession = initiateFlow(termSheetState.borrower)

        val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartySession), GATHERING_SIGNS.childProgressTracker()))

        progressTracker.currentStep = FINALISING_TRANSACTION

        val transaction = subFlow(FinalityFlow(fullySignedTx, listOf(otherPartySession), FINALISING_TRANSACTION.childProgressTracker()))

        return transaction
    }
}

@InitiatedBy(CreateTermSheetFlow::class)
class CreateTermSheetFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an IOU transaction." using (output is TermSheetState)
                val termSheetState = output as TermSheetState
//                "I won't accept IOUs with a value over 100." using (termSheetState.value <= 100)
            }
        }
        val txId = subFlow(signTransactionFlow).id
        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}