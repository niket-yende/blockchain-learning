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
class CreateAccountsOnLoanLedgerFlow(val dataArray: Array<Any>) : FlowLogic<List<SignedTransaction>>() {

    val loanRef = dataArray[0] as String
    val leadArrangerName = dataArray[1] as CordaX500Name

    override val progressTracker: ProgressTracker = ProgressTracker(GENERATING_TRANSACTION,
            SIGNING_TRANSACTION,
            FINALISING_TRANSACTION)

    companion object {
        private val log: Logger = loggerFor<CreateAccountsOnLoanLedgerFlow>()
    }

    @Suspendable
    override fun call(): List<SignedTransaction> {
        CreateAccountsOnLoanLedgerFlow.log.info("-------------------Flow name: CreateAccountsOnLoanLedgerFlow, My name:${ourIdentity.name}---------------------")
        var transactions = mutableListOf<SignedTransaction>()

//        require(leadArrangerName == ourIdentity.name) {
//            "Only Lead Arranger can call this flow"
//        }

        // Getting Notary from the Network Map
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        //Query Static data to get the Lenders Account Ids
        val stateIDCriteria = builder { StaticDataSchema1.PersistentStaticData::loan_ref.equal(loanRef) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(stateIDCriteria)
        val staticDataStates = serviceHub.vaultService.queryBy(StaticDataState::class.java, criteria).states
//        requireThat {
//            "No Static Data exists with the given id" using (staticDataStates.size > 0)
//        }
        if (!leadArrangerName.toString().equals(ourIdentity.name.toString())) {
            throw FlowException("Only Lead Arranger can call this flow, Current Identity is + " + ourIdentity.name.toString())
        }

        val inputStaticDataState = staticDataStates[0].state.data


        //Create accounts for LA,LenderA,LenderB
        val lAAccountState = AccountState(inputStaticDataState.loanRef, inputStaticDataState.leadArrangerAccountNumber, inputStaticDataState.leadArranger, 0.0, mutableListOf())
        val lenderAAccountState = AccountState(inputStaticDataState.loanRef, inputStaticDataState.lenderAAccountNumber, inputStaticDataState.lenderA, 0.0, mutableListOf())
        val lenderBAccountState = AccountState(inputStaticDataState.loanRef, inputStaticDataState.lenderBAccountNumber, inputStaticDataState.lenderB, 0.0, mutableListOf())

        val accountCommand = Command(AccountContract.Commands.Create(), listOf(ourIdentity.owningKey));

        // Building the transcation by specifying the output state, command and Contract ID to validate the transaction
        progressTracker.currentStep = GENERATING_TRANSACTION


        val transactionBuilder1 = TransactionBuilder(notary)
                .addOutputState(lAAccountState)
                .addCommand(accountCommand)

        val transactionBuilder2 = TransactionBuilder(notary)
                .addOutputState(lenderAAccountState, AccountContract.ID)
                .addCommand(accountCommand)

        val transactionBuilder3 = TransactionBuilder(notary)
                .addOutputState(lenderBAccountState, AccountContract.ID)
                .addCommand(accountCommand)


        // verifying the transaction by checking the contract conditions are met or not
        transactionBuilder1.verify(serviceHub)
        transactionBuilder2.verify(serviceHub)
        transactionBuilder3.verify(serviceHub)
//        transactionBuilder4.verify(serviceHub)
        //transactionBuilder5.verify(serviceHub)

        // Transaction signed by all the participants
        progressTracker.currentStep = SIGNING_TRANSACTION

        val stx1 = serviceHub.signInitialTransaction(transactionBuilder1)
        val stx2 = serviceHub.signInitialTransaction(transactionBuilder2)
        val stx3 = serviceHub.signInitialTransaction(transactionBuilder3)
//        val stx4 = serviceHub.signInitialTransaction(transactionBuilder4)

        val borrowerSession = initiateFlow(inputStaticDataState.borrower)
        val lenderASession = initiateFlow(inputStaticDataState.lenderA)
        val lenderBSession = initiateFlow(inputStaticDataState.lenderB)

        progressTracker.currentStep = FINALISING_TRANSACTION

        val transaction1 = subFlow(FinalityFlow(stx1, listOf(), FINALISING_TRANSACTION.childProgressTracker()))
        val transaction2 = subFlow(FinalityFlow(stx2, listOf(lenderASession), FINALISING_TRANSACTION.childProgressTracker()))
        val transaction3 = subFlow(FinalityFlow(stx3, listOf(lenderBSession), FINALISING_TRANSACTION.childProgressTracker()))

        transactions.add(transaction1)
        transactions.add(transaction2)
        transactions.add(transaction3)

        return transactions
    }
}

@InitiatedBy(CreateAccountsOnLoanLedgerFlow::class)
class CreateAccountsOnLoanLedgerFlowResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call(): Unit {
        println("Reached CreateAccountsOnLoanLedgerFlowResponder")
//        subFlow(ReceiveFinalityFlow(counterpartySession))
        subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}