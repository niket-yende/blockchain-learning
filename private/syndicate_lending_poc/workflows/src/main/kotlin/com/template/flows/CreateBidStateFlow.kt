package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.BidContract
import com.template.contracts.StaticDataContract
import com.template.schema.StaticDataSchema1
import com.template.schema.SubscriptionSchema1
import com.template.states.BidState
import com.template.states.StaticDataState
import com.template.states.SubscriptionState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.node.services.vault.Builder.equal
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger

@InitiatingFlow
@StartableByRPC
class CreateBidStateFlow(val bidState: BidState) : FlowLogic<List<SignedTransaction>>() {

    override val progressTracker: ProgressTracker = ProgressTracker(GENERATING_TRANSACTION,
            SIGNING_TRANSACTION,
            GATHERING_SIGNS,
            FINALISING_TRANSACTION)

    companion object {
        private val log: Logger = loggerFor<CreateStaticDataFlow>()
    }

    @Suspendable
    override fun call(): List<SignedTransaction> {
        log.info("-------------------Flow name: CreateBidStateFlow, My name:${ourIdentity.name}---------------------")
        // Getting Notary from the Network Map
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        var transactions = mutableListOf<SignedTransaction>()

        //Query subscription state data to get loanAmount
        val subscriptionCriteria = builder { SubscriptionSchema1.PersistentSubsciption::loan_ref.equal(bidState.loanRef) }
        val criteria1 = QueryCriteria.VaultCustomQueryCriteria(subscriptionCriteria)
        val subscriptionStates = serviceHub.vaultService.queryBy(SubscriptionState::class.java, criteria1).states
        requireThat {
            "No Subscription Data exists with the given id" using (subscriptionStates.size > 0)
        }
        val subscriptionState = subscriptionStates.get(0).state.data

        //Query Static data to get the Lenders Account Ids
        val stateIDCriteria = builder { StaticDataSchema1.PersistentStaticData::loan_ref.equal(bidState.loanRef) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(stateIDCriteria)
        val staticDataStates = serviceHub.vaultService.queryBy(StaticDataState::class.java, criteria).states
        requireThat {
            "No Static Data exists with the given id" using (staticDataStates.size > 0)
        }

        val leadArrangerContribution = subscriptionState.loanAmount - (bidState.lenderASubsAmount + bidState.lenderBSubsAmount)
        println("leadArrangerContribution : " + leadArrangerContribution)
        val inputStaticDataState = staticDataStates[0].state.data
        val outputStaticDataState = inputStaticDataState.copy(borrowerLoanAmount = subscriptionState.loanAmount, lenderAContribution = bidState.lenderASubsAmount, lenderBContribution = bidState.lenderBSubsAmount, leadArrangerContribution = leadArrangerContribution)

        val staticDataCommand = Command(StaticDataContract.Commands.Update(), listOf(ourIdentity.owningKey));

        // Create Command : It is used by contract to check the constraints for creating the OrderRequest
        val createCommand = Command(BidContract.Commands.AskConsent(), listOf(ourIdentity.owningKey));

        // Building the transcation by specifying the output state, command and Contract ID to validate the transaction
        progressTracker.currentStep = GENERATING_TRANSACTION
        val transactionBuilder1 = TransactionBuilder(notary)
                .addOutputState(bidState, BidContract.ID)
                .addCommand(createCommand)

        print("Create BID STATe::::::::" + bidState.toString())
        // verifying the transaction by checking the contract conditions are met or not
        transactionBuilder1.verify(serviceHub)

        val transactionBuilder2 = TransactionBuilder(notary)
                .addInputState(staticDataStates.get(0))
                .addOutputState(outputStaticDataState, StaticDataContract.ID)
                .addCommand(staticDataCommand)

        transactionBuilder2.verify(serviceHub)

        // Transaction signed by the initiator party
        progressTracker.currentStep = SIGNING_TRANSACTION
        val stx1 = serviceHub.signInitialTransaction(transactionBuilder1)
        val stx2 = serviceHub.signInitialTransaction(transactionBuilder2)

        val borrowerSession = initiateFlow(bidState.borrower)
        progressTracker.currentStep = FINALISING_TRANSACTION
        val transaction1 = subFlow(FinalityFlow(stx1, listOf(borrowerSession), FINALISING_TRANSACTION.childProgressTracker()))
        val transaction2 = subFlow(FinalityFlow(stx2, listOf(borrowerSession), FINALISING_TRANSACTION.childProgressTracker()))

        transactions.add(transaction1)
        transactions.add(transaction2)
        return transactions
    }
}

@InitiatedBy(CreateBidStateFlow::class)
class CreateBidStateFlowResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call(): Unit {
        subFlow(ReceiveFinalityFlow(counterpartySession))
        subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}