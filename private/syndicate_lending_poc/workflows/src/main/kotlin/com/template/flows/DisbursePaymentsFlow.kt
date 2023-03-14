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
import java.util.*

@InitiatingFlow
@StartableByRPC
@SchedulableFlow
class DisbursePaymentsFlow(val args: Array<Any>) : FlowLogic<List<SignedTransaction>>() {

    override val progressTracker: ProgressTracker = ProgressTracker(GENERATING_TRANSACTION,
            SIGNING_TRANSACTION,
            FINALISING_TRANSACTION)

    companion object {
        private val log: Logger = loggerFor<DisbursePaymentsFlow>()
    }

    @Suspendable
    override fun call(): List<SignedTransaction> {
        log.info("-------------------Flow name: DisbursePaymentsFlow, My name:${ourIdentity.name}---------------------")

        var transactions :MutableList<SignedTransaction> = mutableListOf<SignedTransaction>()

        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val leadArrangerName = args[0] as String
        val interestPaymentStateId = UUID.fromString(args[1] as String)

        if (!leadArrangerName.equals(ourIdentity.name.toString())) {
            throw FlowException("Only Lead Arranger can call this flow, Current Identity is + " + ourIdentity.name.toString())
        }


        //Get the Interest payment details
        val interestPaymentIDCriteria = builder { InterestPaymentSchema1.PersistentInterestPayment::linearId.equal(interestPaymentStateId) }
        val interestCriteria = QueryCriteria.VaultCustomQueryCriteria(interestPaymentIDCriteria)
        val interestPaymentStates = serviceHub.vaultService.queryBy(InterestPaymentState::class.java, interestCriteria).states
        requireThat {
            "No Interest Payment State exists with the given id" using (interestPaymentStates.size > 0)
        }
        val inputInterestPaymentState:InterestPaymentState = interestPaymentStates[0].state.data


        //Query Static data to get the Lenders Account Ids
        val loanLedgerStateIDCriteria = builder { LoanLedgerSchema1.PersistentLoanLedger::loan_ref.equal(inputInterestPaymentState.loanRef) }
        val criteria = QueryCriteria.VaultCustomQueryCriteria(loanLedgerStateIDCriteria)
        val loanLedgerStates = serviceHub.vaultService.queryBy(LoanLedgerState::class.java, criteria).states
        requireThat {
            "No Static Data State exists with the given id" using (loanLedgerStates.size > 0)
        }
        val loanLedgerState : LoanLedgerState = loanLedgerStates[0].state.data

        //Get the lead ArrangerAccount

        print("LAAcc ID"+ loanLedgerState.leadArrangerAccountId)
        val lAAccountIDCriteria = builder { AccountSchema1.PersistentAccount::account_id.equal(loanLedgerState.leadArrangerAccountId) }
        val lAAccountCriteria = QueryCriteria.VaultCustomQueryCriteria(lAAccountIDCriteria)
        val lAAccountStates = serviceHub.vaultService.queryBy(AccountState::class.java, lAAccountCriteria).states

        val lAAccountState:AccountState = lAAccountStates[0].state.data
        print("LAAcc "+ lAAccountState)


        //Get the payment obligation and update the LA and L1 and L2 accounts
        val paymentObligationForLenderA:Double = inputInterestPaymentState.interestObligation * loanLedgerState.lenderAPercentage / 100
        val paymentObligationForLenderB:Double = inputInterestPaymentState.interestObligation * loanLedgerState.lenderBPercentage / 100
        var updatedLABalance:Double = lAAccountState.balance - (paymentObligationForLenderA + paymentObligationForLenderB)
        updatedLABalance = Math.round(updatedLABalance * 100.0) / 100.0
        //Add the transactions to their accounts

        //Initiate a transfer to LA transaction
        val lenderATransactionState:TransactionState = TransactionState(inputInterestPaymentState.leadArranger, loanLedgerState.lenderA, loanLedgerState.leadArrangerAccountId, loanLedgerState.lenderAAccountId, paymentObligationForLenderA, Instant.now())
        val lenderBTransactionState:TransactionState = TransactionState(inputInterestPaymentState.leadArranger, loanLedgerState.lenderB, loanLedgerState.leadArrangerAccountId, loanLedgerState.lenderBAccountId, paymentObligationForLenderB, Instant.now())

        var lATxIdsList: MutableList<String> = mutableListOf()
        lATxIdsList.addAll(lAAccountState.transactionIds)
        lATxIdsList.add(lenderATransactionState.linearId.toString())
        lATxIdsList.add(lenderBTransactionState.linearId.toString())

        val outputLAAccountState:AccountState = lAAccountState.copy(balance = updatedLABalance,transactionIds = lATxIdsList)

        println("opaccstate"+outputLAAccountState)
        var txIdsList: MutableList<String> = mutableListOf()
        txIdsList.addAll(inputInterestPaymentState.txIdsList)
        txIdsList.add(lenderATransactionState.linearId.id.toString())
        txIdsList.add(lenderBTransactionState.linearId.id.toString())

        val outputInterestPaymentState:InterestPaymentState = inputInterestPaymentState.copy(paymentStatus = InterestPaymentState.PaymentStatus.PAY_LENDERS_INITIATED, txIdsList = txIdsList)

        println("OpIPstate"+outputInterestPaymentState)
        // Create Command : It is used by contract to check the constraints for creating the OrderRequest
        val createCommand = Command(TransactionContract.Commands.Create(), listOf(ourIdentity.owningKey));
        val updateCommand = Command(AccountContract.Commands.Update(), listOf(ourIdentity.owningKey));
        val updateInterestCommand = Command(InterestPaymentContract.Commands.Update(), listOf(ourIdentity.owningKey));

        // Building the transcation by specifying the output state, command and Contract ID to validate the transaction
        progressTracker.currentStep = GENERATING_TRANSACTION

        val transactionBuilder1 = TransactionBuilder(notary)
                .addOutputState(lenderATransactionState, TransactionContract.ID)
                .addCommand(createCommand)
        transactionBuilder1.verify(serviceHub)

        val transactionBuilder2 = TransactionBuilder(notary)
                .addOutputState(lenderBTransactionState, TransactionContract.ID)
                .addCommand(createCommand)
        transactionBuilder2.verify(serviceHub)

        val transactionBuilder3 = TransactionBuilder(notary)
                .addInputState(lAAccountStates.get(0))
                .addOutputState(outputLAAccountState, AccountContract.ID)
                .addCommand(updateCommand)
        transactionBuilder3.verify(serviceHub)
        //Changing the Interest payment state to completed
        val transactionBuilder4 = TransactionBuilder(notary)
                .addInputState(interestPaymentStates.get(0))
                .addOutputState(outputInterestPaymentState, InterestPaymentContract.ID)
                .addCommand(updateInterestCommand)
        transactionBuilder4.verify(serviceHub)
        println("tbuidlders done")
        progressTracker.currentStep = SIGNING_TRANSACTION
        val stx1 = serviceHub.signInitialTransaction(transactionBuilder1)
        val stx2 = serviceHub.signInitialTransaction(transactionBuilder2)
        val stx3 = serviceHub.signInitialTransaction(transactionBuilder3)
        val stx4 = serviceHub.signInitialTransaction(transactionBuilder4)

        val borrowerSession = initiateFlow(inputInterestPaymentState.borrower)
        val lenderASession = initiateFlow(inputInterestPaymentState.lenderA)
        val lenderBSession = initiateFlow(inputInterestPaymentState.lenderB)

        println("sessions done")
        progressTracker.currentStep = FINALISING_TRANSACTION
        val transaction1 = subFlow(FinalityFlow(stx1, listOf(lenderASession), FINALISING_TRANSACTION.childProgressTracker()))
        val transaction2 = subFlow(FinalityFlow(stx2, listOf(lenderBSession), FINALISING_TRANSACTION.childProgressTracker()))
        val transaction3 = subFlow(FinalityFlow(stx3, listOf(), FINALISING_TRANSACTION.childProgressTracker()))
        val transaction4 = subFlow(FinalityFlow(stx4, listOf(borrowerSession, lenderASession, lenderBSession), FINALISING_TRANSACTION.childProgressTracker()))
        println("txns complete done")
        transactions.add(transaction1)
        transactions.add(transaction2)
        transactions.add(transaction3)
        transactions.add(transaction4)

        return transactions
    }
}

//Responder flow to verify the amounts paid to each lender is correct or not
@InitiatedBy(DisbursePaymentsFlow::class)
class DisbursePaymentsResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call(): Unit {
        if (ourIdentity.name.organisation.equals("LenderA") || ourIdentity.name.organisation.equals("LenderB")) {
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
//                            requireThat {
//                                "No Interest Payment State exists with the given id" using (interestPaymentStates.size > 0)
//
//                            }
//                            val inputInterestPaymentState = interestPaymentStates[0].state.data
//
//                            requireThat {
//                                "Interest Obligation value is changed by Borrower.Cannot proceed" using (interestPaymentState.paymentStatus == InterestPaymentState.PaymentStatus.PAYMENT_INITIATED)
//                                "Interest Obligation value is changed by Borrower.Cannot proceed" using (inputInterestPaymentState.interestObligation == interestPaymentState.interestObligation)
//                            }
//                        }
//
//                        if (output is AccountState) {
//
//                            val account = output as AccountState
//
//                            val interestPaymentIDCriteria = builder { InterestPaymentSchema1.PersistentInterestPayment::loan_reference.equal(account.loanRef) }
//                            val interestCriteria = QueryCriteria.VaultCustomQueryCriteria(interestPaymentIDCriteria)
//                            val interestPaymentStates = serviceHub.vaultService.queryBy(InterestPaymentState::class.java, interestCriteria).states
//                            val inputInterestPaymentState = interestPaymentStates.last().state.data
//
//                            requireThat {
//                                "No Interest Payment State exists with the given id" using (interestPaymentStates.size > 0)
//                                "Interest Payment Status Invalid" using(inputInterestPaymentState.paymentStatus == InterestPaymentState.PaymentStatus.PAY)
//                            }
//
//                            val vaultAccountID = builder { AccountSchema1.PersistentAccount ::account_id.equal(account.accountId) }
//                            val accountIdCriteria = QueryCriteria.VaultCustomQueryCriteria(vaultAccountID)
//                            val accountStates = serviceHub.vaultService.queryBy(AccountState::class.java, accountIdCriteria).states
//                            val inputAccountState = accountStates.get(0).state.data
//
//                            //Only Lenders transactions
//                            if(account.accountOwner.name!= inputInterestPaymentState.leadArranger.name) {
//                                //IF tx object is available in the Account
//                                val txIDCriteria = builder { TransactionSchema1.PersistentTransaction::linearId.equal(account.transactionIds.last()) }
//                                val txCriteria = QueryCriteria.VaultCustomQueryCriteria(txIDCriteria)
//                                val txStates = serviceHub.vaultService.queryBy(TransactionState::class.java, txCriteria).states
//
//                                val txState = txStates.get(0).state.data
//                                requireThat {
//                                    "No tranaction committed" using (txStates.size > 0)
//                                    "No transaction Initiated by Borrower" using (txState.paymentState == TransactionState.Status.INITIATED)
//                                    "Partial interest Payments are not supported" using (txState.amount == inputInterestPaymentState.interestObligation/3)
//                                    "Invaid Payment Account" using (txState.fromAccount == inputInterestPaymentState.leadArrangerAccount)
//                                    "Invalid balance update in lender Account" using(account.balance==inputAccountState.balance+inputInterestPaymentState.interestObligation/3)
//                                }
//                            }else{
//                                requireThat {
//
//                                    "Invalid balance transfer from LA Account" using(account.balance==inputAccountState.balance-(2*inputInterestPaymentState.interestObligation/3))
//                                }
//                            }
//                        }
//
//                    }
//        }
//        val txId = subFlow(signTransactionFlow).id
//        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}
