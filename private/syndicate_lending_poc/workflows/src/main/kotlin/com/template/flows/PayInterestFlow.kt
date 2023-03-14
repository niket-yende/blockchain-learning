package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.*
import com.template.schema.*
import com.template.states.*
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
import java.time.Instant

@InitiatingFlow
@StartableByRPC
class PayInterestFlow(val interestPaymentState: InterestPaymentState) : FlowLogic<List<SignedTransaction>>() {
    override val progressTracker: ProgressTracker = ProgressTracker(GENERATING_TRANSACTION,
            SIGNING_TRANSACTION,
            FINALISING_TRANSACTION)

    companion object {
        private val log: Logger = loggerFor<PayInterestFlow>()
    }

    @Suspendable
    override fun call(): List<SignedTransaction> {
        log.info("-------------------Flow name: PayInterestFlow, My name:${ourIdentity.name}---------------------")

        var transactions = mutableListOf<SignedTransaction>()

        // Getting Notary from the Network Map
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        require(interestPaymentState.borrower.name == ourIdentity.name) {
            "Only Borrower can initiate Payment flow"
        }

        //Get the BorrowerAccount
        val accountIDCriteria = builder { AccountSchema1.PersistentAccount::account_id.equal(interestPaymentState.paymentAccount) }
        val accountCriteria = QueryCriteria.VaultCustomQueryCriteria(accountIDCriteria)
        val borrowerAccountStates = serviceHub.vaultService.queryBy(AccountState::class.java, accountCriteria).states
        requireThat {
            "No Account State exists with the given id" using (borrowerAccountStates.size > 0)
        }
        val borrowerAccountState = borrowerAccountStates.get(0).state.data

        //Initiate a transaction and save to Borrower and LeadArranger
        val transactionState = TransactionState(interestPaymentState.leadArranger,
                interestPaymentState.borrower,
                interestPaymentState.paymentAccount,
                interestPaymentState.leadArrangerAccount,
                interestPaymentState.interestObligation,
                Instant.now())
        //Add the transaction to the BorrowerAccountState

        println("Tx Linear Id: " + transactionState.linearId.id.toString())

        var txIdList: MutableList<String> = mutableListOf()
        txIdList.addAll(borrowerAccountState.transactionIds)
        txIdList.add(transactionState.linearId.id.toString())

        val updatedBorrowerBalance = borrowerAccountState.balance - interestPaymentState.interestObligation
        val outputBorrowerAccountState = borrowerAccountState.copy(balance = updatedBorrowerBalance, transactionIds = txIdList)

        //Update IP status and append the TxList
        println("Interest Id: " + interestPaymentState.linearId.id)
        val interestPaymentIDCriteria = builder { InterestPaymentSchema1.PersistentInterestPayment::loan_reference.equal(interestPaymentState.loanRef) }
        val interestCriteria = QueryCriteria.VaultCustomQueryCriteria(interestPaymentIDCriteria)
        val interestPaymentStates = serviceHub.vaultService.queryBy(InterestPaymentState::class.java, interestCriteria).states
        requireThat {
            "No Interest Payment State exists with the given id" using (interestPaymentStates.size > 0)
        }

        requireThat {
            "Not sufficient balance to Pay" using (borrowerAccountState.balance >= interestPaymentState.interestObligation)
        }
        //Append the borrowerTxId
        var txList: MutableList<String> = mutableListOf()
        txList.addAll(interestPaymentState.txIdsList)
        txList.add(transactionState.linearId.toString())
        val outputInterestPaymentState = interestPaymentState.copy(txIdsList = txList)
        println("txList: "+txList.toString())

        //Update the outstanding amount on LoanLedger
        val loanLedgerIDCriteria = builder { LoanLedgerSchema1.PersistentLoanLedger::loan_ref.equal(interestPaymentState.loanRef) }
        val loanLedgerCriteria = QueryCriteria.VaultCustomQueryCriteria(loanLedgerIDCriteria)
        val loanLedgerStates = serviceHub.vaultService.queryBy(LoanLedgerState::class.java, loanLedgerCriteria).states
        requireThat {
            "No Loan Ledger state exists with the given id" using (loanLedgerStates.size > 0)
        }
        val inputLoanLedgerState:LoanLedgerState = loanLedgerStates.get(0).state.data
        val outstandingAmount:Double = inputLoanLedgerState.outstandingLoan - interestPaymentState.interestObligation

        var status: LoanLedgerState.LedgerStatus = LoanLedgerState.LedgerStatus.UPDATED

        if (outstandingAmount <= 0) {
            status = LoanLedgerState.LedgerStatus.LOAN_RECOVERED
        }
        else{
            status = LoanLedgerState.LedgerStatus.OUTSTANDING_AMT_UPDATED
        }

        val outputLoanLedgerState = inputLoanLedgerState.copy(outstandingLoan = outstandingAmount, status = status)
        println("before command")
        val createCommand = Command(TransactionContract.Commands.Create(), listOf(ourIdentity.owningKey))
        val updateCommand = Command(AccountContract.Commands.Update(), listOf(ourIdentity.owningKey))
        val updateInterestCommand = Command(InterestPaymentContract.Commands.Update(), listOf(ourIdentity.owningKey))
        val updateLoanLedgerCommand = Command(LoanLedgerContract.Commands.Update(), listOf(ourIdentity.owningKey))

        // Building the transcation by specifying the output state, command and Contract ID to validate the transaction
        progressTracker.currentStep = GENERATING_TRANSACTION
        println("before builders")
        val transactionBuilder1 = TransactionBuilder(notary)
                .addOutputState(transactionState, TransactionContract.ID)
                .addCommand(createCommand)
        transactionBuilder1.verify(serviceHub)

        val transactionBuilder2 = TransactionBuilder(notary)
                .addInputState(borrowerAccountStates.get(0))
                .addOutputState(outputBorrowerAccountState, AccountContract.ID)
                .addCommand(updateCommand)
        transactionBuilder2.verify(serviceHub)

        val transactionBuilder3 = TransactionBuilder(notary)
                .addInputState(interestPaymentStates.last())
                .addOutputState(outputInterestPaymentState, InterestPaymentContract.ID)
                .addCommand(updateInterestCommand)
        transactionBuilder3.verify(serviceHub)

        val transactionBuilder4 = TransactionBuilder(notary)
                .addInputState(loanLedgerStates.get(0))
                .addOutputState(outputLoanLedgerState, LoanLedgerContract.ID)
                .addCommand(updateLoanLedgerCommand)
        transactionBuilder4.verify(serviceHub)

        progressTracker.currentStep = SIGNING_TRANSACTION
        val stx1 = serviceHub.signInitialTransaction(transactionBuilder1)
        val stx2 = serviceHub.signInitialTransaction(transactionBuilder2)
        val stx3 = serviceHub.signInitialTransaction(transactionBuilder3)
        val stx4 = serviceHub.signInitialTransaction(transactionBuilder4)

        val leadArrangerSession = initiateFlow(interestPaymentState.leadArranger)
        val lenderASession = initiateFlow(interestPaymentState.lenderA)
        val lenderBSession = initiateFlow(interestPaymentState.lenderB)


        progressTracker.currentStep = FINALISING_TRANSACTION
        val transaction1 = subFlow(FinalityFlow(stx1, listOf(leadArrangerSession), FINALISING_TRANSACTION.childProgressTracker()))
        val transaction2 = subFlow(FinalityFlow(stx2, listOf(), FINALISING_TRANSACTION.childProgressTracker()))
        val transaction3 = subFlow(FinalityFlow(stx3, listOf(leadArrangerSession, lenderASession, lenderBSession), FINALISING_TRANSACTION.childProgressTracker()))
        val transaction4 = subFlow(FinalityFlow(stx4, listOf(leadArrangerSession, lenderASession, lenderBSession), FINALISING_TRANSACTION.childProgressTracker()))


        transactions.add(transaction1)
        transactions.add(transaction2)
        transactions.add(transaction3)
        transactions.add(transaction4)

        return transactions
    }
}


@InitiatedBy(PayInterestFlow::class)
class PayInterestResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call(): Unit {
        if (ourIdentity.name.organisation.equals("LeadArranger")) {
            subFlow(ReceiveFinalityFlow(counterpartySession))
            subFlow(ReceiveFinalityFlow(counterpartySession))
            subFlow(ReceiveFinalityFlow(counterpartySession))
        }
        else if (ourIdentity.name.organisation.equals("LenderA") || ourIdentity.name.organisation.equals("LenderB")) {
            subFlow(ReceiveFinalityFlow(counterpartySession))
            subFlow(ReceiveFinalityFlow(counterpartySession))
        }
        else {
            subFlow(ReceiveFinalityFlow(counterpartySession))
        }
//        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
//            override fun checkTransaction(stx: SignedTransaction) =
//                    requireThat {
//                        val output = stx.tx.outputs.single().data
//
//                        if (output is InterestPaymentState) {
//
//                            val interestPaymentState = output
//                            //vault Query
//                            val interestPaymentIDCriteria = builder { InterestPaymentSchema1.PersistentInterestPayment::linearId.equal(interestPaymentState.linearId) }
//                            val interestCriteria = QueryCriteria.VaultCustomQueryCriteria(interestPaymentIDCriteria)
//                            val interestPaymentStates = serviceHub.vaultService.queryBy(InterestPaymentState::class.java, interestCriteria).states
//                            val inputInterestPaymentState = interestPaymentStates[0].state.data
//
//                            requireThat {
//                                "No Interest Payment State exists with the given id" using (interestPaymentStates.size > 0)
//                                "Interest Payment Status Invalid" using (inputInterestPaymentState.paymentStatus == InterestPaymentState.PaymentStatus.PAYMENT_INITIATED)
//                                "Interest Obligation value is changed by Borrower.Cannot proceed" using (inputInterestPaymentState.interestObligation == interestPaymentState.interestObligation)
//                            }
//                        }
//                        //If LA, check if the amount transferred equal to Obligation amount from transactions object tied to it.
//
//                        if (output is AccountState) {
//
//                            val account = output as AccountState
//
//                            val interestPaymentIDCriteria = builder { InterestPaymentSchema1.PersistentInterestPayment::loan_reference.equal(account.loanRef) }
//                            val interestCriteria = QueryCriteria.VaultCustomQueryCriteria(interestPaymentIDCriteria)
//                            val interestPaymentStates = serviceHub.vaultService.queryBy(InterestPaymentState::class.java, interestCriteria).states
//                            requireThat {
//                                "No Interest Payment State exists with the given id" using (interestPaymentStates.size > 0)
//                            }
//                            val inputInterestPaymentState = interestPaymentStates.last().state.data
//
//                            //IF tx object is available in the Account
//                            val txIDCriteria = builder { TransactionSchema1.PersistentTransaction::linearId.equal(account.transactionIds.last()) }
//                            val txCriteria = QueryCriteria.VaultCustomQueryCriteria(txIDCriteria)
//                            val txStates = serviceHub.vaultService.queryBy(TransactionState::class.java, txCriteria).states
//
//                            val txState = txStates.get(0).state.data
//                            requireThat {
//                                "No tranaction committed" using (txStates.size > 0)
//                                "No transaction Initiated by Borrower" using (txState.paymentState == TransactionState.Status.INITIATED)
//                                "Partial interest Payments are not supported" using (txState.amount == inputInterestPaymentState.interestObligation)
//                                "Invaid Payment Account" using (txState.fromAccount == inputInterestPaymentState.paymentAccount)
//                                "Invaid LA Account" using (txState.toAccount == inputInterestPaymentState.leadArrangerAccount)
//                            }
//
//                        }
//
//                    }
//        }
//        val txId = subFlow(signTransactionFlow).id
    }
}