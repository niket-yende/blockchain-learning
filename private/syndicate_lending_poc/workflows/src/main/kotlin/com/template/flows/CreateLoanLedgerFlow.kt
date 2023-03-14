package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.AccountContract
import com.template.contracts.LoanLedgerContract
import com.template.schema.StaticDataSchema1
import com.template.states.AccountState
import com.template.states.LoanLedgerState
import com.template.states.StaticDataState
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

@StartableByRPC
@InitiatingFlow
class CreateLoanLedgerFlow(val loanLedgerState: LoanLedgerState, val borrowerAccount: AccountState) : FlowLogic<List<SignedTransaction>>() {

    override val progressTracker: ProgressTracker = ProgressTracker(GENERATING_TRANSACTION,
            SIGNING_TRANSACTION,
            FINALISING_TRANSACTION)

    companion object {
        private val log: Logger = loggerFor<CreateLoanLedgerFlow>()
    }

    @Suspendable
    override fun call(): List<SignedTransaction> {
        log.info("-------------------Flow name: Create_Loan_Ledger_Flow, My name:${ourIdentity.name}---------------------")

        var transactions = mutableListOf<SignedTransaction>()

//        require(loanLedgerState.borrower.name == ourIdentity.name) {
//            "Only Borrower can create a Loan ledger"
//        }

        if (!loanLedgerState.borrower.name.toString().equals(ourIdentity.name.toString())) {
            throw FlowException("Only Lead Arranger can call this flow, Current Identity is + " + ourIdentity.name.toString())
        }

        // Getting Notary from the Network Map
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // Create Command : It is used by contract to check the constraints for creating the OrderRequest
        val createLoanLedgerCommand = Command(LoanLedgerContract.Commands.Create(), listOf(ourIdentity.owningKey))
        val createAccountCommand = Command(AccountContract.Commands.Create(), listOf(ourIdentity.owningKey))

        // Building the transcation by specifying the output state, command and Contract ID to validate the transaction
        progressTracker.currentStep = GENERATING_TRANSACTION

        val transactionBuilder1 = TransactionBuilder(notary)
                .addOutputState(loanLedgerState)
                .addCommand(createLoanLedgerCommand)

        val transactionBuilder2 = TransactionBuilder(notary)
                .addOutputState(borrowerAccount)
                .addCommand(createAccountCommand)

        // verifying the transaction by checking the contract conditions are met or not
        transactionBuilder1.verify(serviceHub)
        transactionBuilder2.verify(serviceHub)

        // Transaction signed by the initiator party
        progressTracker.currentStep = SIGNING_TRANSACTION
        val stx1 = serviceHub.signInitialTransaction(transactionBuilder1)
        val stx2 = serviceHub.signInitialTransaction(transactionBuilder2)

        val leadArrangerSession = initiateFlow(loanLedgerState.leadArranger)
        val lenderASession = initiateFlow(loanLedgerState.lenderA)
        val lenderBSession = initiateFlow(loanLedgerState.lenderB)

        val fullySignedSTX1 = subFlow(CollectSignaturesFlow(stx1, listOf(leadArrangerSession), CollectSignaturesFlow.tracker()))

        progressTracker.currentStep = FINALISING_TRANSACTION
        val transaction1 = subFlow(FinalityFlow(fullySignedSTX1, listOf(leadArrangerSession, lenderASession, lenderBSession), FINALISING_TRANSACTION.childProgressTracker()))
        val transaction2 = subFlow(FinalityFlow(stx2, listOf(), FINALISING_TRANSACTION.childProgressTracker()))

        transactions.add(transaction1)
        transactions.add(transaction2)

        return transactions
    }
}

@InitiatedBy(CreateLoanLedgerFlow::class)
class CreateLoanLedgerFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

//        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
//            override fun checkTransaction(stx: SignedTransaction) = requireThat {
//                val output = stx.tx.outputs.single().data
//
//                //LA check
//                if (output is LoanLedgerState && (ourIdentity.name == output.leadArranger.name)) {
//
//                    val loanLedgerState = output
//
//                    //Query Static data to get the Lenders Account Ids
//                    val stateIDCriteria = builder { StaticDataSchema1.PersistentStaticData::loan_ref.equal(loanLedgerState.loanRef) }
//                    val criteria = QueryCriteria.VaultCustomQueryCriteria(stateIDCriteria)
//                    val staticDataStates = serviceHub.vaultService.queryBy(StaticDataState::class.java, criteria).states
//                    requireThat {
//                        "No Static Data exists with the given id" using (staticDataStates.size > 0)
//                    }
//                    requireThat {
//                        "Outstanding value incorrect" using (loanLedgerState.outstandingLoan == staticDataStates.get(0).state.data.borrowerLoanAmount)
//                    }
//                }
//            }
//        }
//        val txId = subFlow(signTransactionFlow).id
        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}