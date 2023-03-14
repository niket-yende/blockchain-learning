package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.AccountContract
import com.template.contracts.InterestPaymentContract
import com.template.schema.AccountSchema1
import com.template.schema.InterestPaymentSchema1
import com.template.schema.TransactionSchema1
import com.template.states.AccountState
import com.template.states.InterestPaymentState
import com.template.states.TransactionState
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
import java.lang.IllegalArgumentException
import java.util.*

@InitiatingFlow
@StartableByRPC
@SchedulableFlow
class UpdateAccountBalance(val args: Array<Any>) : FlowLogic<List<SignedTransaction>>() {
    override val progressTracker: ProgressTracker = ProgressTracker(GENERATING_TRANSACTION,
            SIGNING_TRANSACTION,
            FINALISING_TRANSACTION)

    companion object {
        private val log: Logger = loggerFor<UpdateAccountBalance>()
    }

    @Suspendable
    override fun call(): List<SignedTransaction> {


        log.info("-------------------Flow name: UpdateAccountBalance, My name:${ourIdentity.name}---------------------")



        var transactions = mutableListOf<SignedTransaction>()
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val partyName = args[0] as String
        val interestPaymentStateId = UUID.fromString(args[1] as String)
        val txId = UUID.fromString(args[2] as String)
        val paymentStatus = args[3] as InterestPaymentState.PaymentStatus
        val loanRef = args[4] as String


        if (!partyName.equals(ourIdentity.name.toString())) {
            throw FlowException("Only Account owner can update his balance + " + ourIdentity.name.toString())
        }

        //Get the Party account
        val accountIDCriteria = builder { AccountSchema1.PersistentAccount::loan_ref.equal(loanRef) }
        val accountCriteria = QueryCriteria.VaultCustomQueryCriteria(accountIDCriteria)
        val accountStates = serviceHub.vaultService.queryBy(AccountState::class.java, accountCriteria).states
        requireThat {
            "No Account exists for the given party : " + partyName using (accountStates.size > 0)
        }
        val accountState :AccountState = accountStates.get(0).state.data

        println("Account state size: "+accountStates.size+":Party: "+partyName+"our identity: "+ourIdentity.name)
        //Get the Transaction from TxId
        val txIDCriteria = builder { TransactionSchema1.PersistentTransaction::linearId.equal(txId) }
        val txCriteria = QueryCriteria.VaultCustomQueryCriteria(txIDCriteria)
        val txStates = serviceHub.vaultService.queryBy(TransactionState::class.java, txCriteria).states
        requireThat {
            "No Transaction exists for the given party : " + partyName using (txStates.size > 0)
        }
        val txState:TransactionState = txStates.get(0).state.data


        var updatedBalance:Double = accountState.balance + txState.amount
        updatedBalance = Math.round(updatedBalance * 100.0) / 100.0

        var txIdsList: MutableList<String> = mutableListOf()
        txIdsList.addAll(accountState.transactionIds)
        txIdsList.add(txId.toString())


        val outputAccountState:AccountState = accountState.copy(balance = updatedBalance, transactionIds = txIdsList)

        val interestPaymentIDCriteria = builder { InterestPaymentSchema1.PersistentInterestPayment::linearId.equal(interestPaymentStateId) }
        val interestCriteria = QueryCriteria.VaultCustomQueryCriteria(interestPaymentIDCriteria)
        val interestPaymentStates = serviceHub.vaultService.queryBy(InterestPaymentState::class.java, interestCriteria).states
        requireThat {
            "No Interest Payment State exists with the given id" using (interestPaymentStates.size > 0)
        }
        val inputInterestPaymentState:InterestPaymentState = interestPaymentStates[0].state.data

        val outputInterestPaymentState:InterestPaymentState = inputInterestPaymentState.copy(paymentStatus = paymentStatus)

        //Create transaction for updating Account State
        val accountCommand = Command(AccountContract.Commands.Update(), listOf(ourIdentity.owningKey));
        val updateCommand = Command(InterestPaymentContract.Commands.Update(), listOf(ourIdentity.owningKey));

        progressTracker.currentStep = GENERATING_TRANSACTION

        val transactionBuilder1 = TransactionBuilder(notary)
                .addInputState(accountStates.get(0))
                .addOutputState(outputAccountState, AccountContract.ID)
                .addCommand(accountCommand)
        transactionBuilder1.verify(serviceHub)

        val transactionBuilder2 = TransactionBuilder(notary)
                .addInputState(interestPaymentStates.get(0))
                .addOutputState(outputInterestPaymentState, InterestPaymentContract.ID)
                .addCommand(updateCommand)

        progressTracker.currentStep = SIGNING_TRANSACTION
        val stx1 = serviceHub.signInitialTransaction(transactionBuilder1)
        val stx2 = serviceHub.signInitialTransaction(transactionBuilder2)

        val leadArrangerSession = initiateFlow(inputInterestPaymentState.leadArranger)
        val borrowerSession = initiateFlow(inputInterestPaymentState.borrower)
        val lenderASession = initiateFlow(inputInterestPaymentState.lenderA)
        val lenderBSession = initiateFlow(inputInterestPaymentState.lenderB)

        var tx2SessionsList = listOf<FlowSession>()

        if (ourIdentity.name.equals(inputInterestPaymentState.leadArranger.name)) {
            println("entreed LA")
            tx2SessionsList = listOf(borrowerSession, lenderASession, lenderBSession)
        } else if (ourIdentity.name.equals(inputInterestPaymentState.lenderA.name)) {
            println("entreed L1")
            tx2SessionsList = listOf(borrowerSession, leadArrangerSession, lenderBSession)
        } else if (ourIdentity.name.equals(inputInterestPaymentState.lenderB.name)) {
            println("entreed L2")
            tx2SessionsList = listOf(borrowerSession, leadArrangerSession, lenderASession)
        }else{
            throw FlowException(IllegalArgumentException("Not Authorized----------"))
        }
        progressTracker.currentStep = FINALISING_TRANSACTION

        val transaction1 = subFlow(FinalityFlow(stx1, listOf(), FINALISING_TRANSACTION.childProgressTracker()))
        val transaction2 = subFlow(FinalityFlow(stx2, tx2SessionsList, FINALISING_TRANSACTION.childProgressTracker()))
        transactions.add(transaction1)
        transactions.add(transaction2)

        return transactions
    }
}

@InitiatedBy(UpdateAccountBalance::class)
class UpdateAccountBalanceFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
//        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
//            override fun checkTransaction(stx: SignedTransaction) = requireThat {
//                val output = stx.tx.outputs.single().data
//                "This must be an IOU transaction." using (output is TermSheetState)
//                val termSheetState = output as TermSheetState
////                "I won't accept IOUs with a value over 100." using (termSheetState.value <= 100)
//            }
//        }
//        val txId = subFlow(signTransactionFlow).id
        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}