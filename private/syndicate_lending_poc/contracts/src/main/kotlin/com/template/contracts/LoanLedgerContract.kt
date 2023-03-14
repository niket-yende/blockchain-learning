package com.template.contracts

import com.template.states.LoanLedgerState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class LoanLedgerContract : Contract {
    companion object {
        const val ID = "com.template.contracts.LoanLedgerContract"
    }

    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.
        val command = tx.commands.requireSingleCommand<LoanLedgerContract.Commands>()

        when (command.value) {
            is LoanLedgerContract.Commands.Create -> {
                requireThat {
                    "There should be no input state included in the transaction!" using (tx.inputs.size == 0)
                    "There should be one output state included in the transaction!" using (tx.outputs.size == 1)

                    val outputState = tx.getOutput(0) as LoanLedgerState
                    "Tx. amount is invalid!" using (outputState.outstandingLoan > 0.0)
                    "Invalid Borrower Identity!" using (outputState.borrower.name.organisation.equals("Borrower"))
                    "Invalid status!" using (outputState.status == LoanLedgerState.LedgerStatus.CREATED)
                    //  "Invalid LeadArranger Identity!" using (outputState.leadArranger.name.organisation.equals("LeadArranger"))
                    //  "Invalid Lender A Identity!" using (outputState.lenderA.name.organisation.equals("LenderA"))
                    //  "Invalid Lender B Identity!" using (outputState.lenderB.name.organisation.equals("LenderB"))
                }
            }
            is LoanLedgerContract.Commands.Update -> {
                requireThat {
                    "There should be no input state included in the transaction" using (tx.inputs.size == 1)
                    "There should be one output states included in the transaction" using (tx.outputs.size == 1)
                }
            }
            else -> throw IllegalArgumentException("Unrecognised Command")
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Create : Commands
        class Update : Commands
    }
}