package com.template.contracts

import com.template.states.InterestPaymentState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction



class TransactionContract : Contract {
    // This is used to identify our contracts when building a transaction
    companion object {
        val ID = "com.template.contracts.TransactionContract"
    }

    // A transaction is considered valid if the verify() function of the contracts of each of the transaction's input
    // and output states does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
//        val groups = tx.groupStates(BankState::withoutOwner)
        val command = tx.commands.requireSingleCommand<Commands>()

        when (command.value){
            is Commands.Create -> {
                requireThat {



//                    val outputState = tx.getOutput(0) as InterestPaymentState
//                    "EMI value is invalid!" using (outputState.interestObligation>0.0)
////                    "Tx. amount is invalid!" using (outputState.loanAmount > 0.0)
//                    "Invalid Borrower Identity!" using (outputState.borrower.name.organisation.equals("Borrower"))
//                    "Invalid LeadArranger Identity!" using (outputState.leadArranger.name.organisation.equals("LeadArranger"))
////                    "Invalid Loan Type!" using (outputState.typeOfLoan.equals("Senior_Loan_Unsecured"))
                }
            }
            is Commands.Completed -> {
                requireThat {
//                    "There should be one input state included in the transaction" using (tx.inputs.size==1)
//                    "There should be two output states included in the transaction" using (tx.outputs.size==1)
                }
            }

            else -> throw IllegalArgumentException("Unrecognised Command")
        }

    }
    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Create: Commands
        class Completed: Commands
    }

}