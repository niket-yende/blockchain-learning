package com.template.contracts

import com.template.states.BidState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class BidContract : Contract {
    companion object {
        const val ID = "com.template.contracts.BidContract"
    }

    override fun verify(tx: LedgerTransaction) {

        val command = tx.commands.requireSingleCommand<Commands>()

        when (command.value) {
            is Commands.AskConsent -> {
                requireThat {
                    "There should be no input state included in the transaction!" using (tx.inputs.size == 0)
                    "There should be one output state included in the transaction!" using (tx.outputs.size == 1)

                    val outputState = tx.getOutput(0) as BidState
                    "End Date should be greater than Start Date!" using (outputState.startDate.compareTo(outputState.endDate) < 0)
                    "Tx. Loan Amount is invalid!" using (outputState.loanAmount > 0.0)
                    "Tx. Tenure is invalid!" using (outputState.tenure > 0.0)
                    "Tx. Subscription Amount is invalid!" using (outputState.lenderASubsAmount > 0.0)
                    "Tx. Subscription Amount is invalid!" using (outputState.lenderBSubsAmount > 0.0)

                    "Invalid Borrower Identity!" using (outputState.borrower.name.organisation.equals("Borrower"))
                    "Invalid LeadArranger Identity!" using (outputState.leadArranger.name.organisation.equals("LeadArranger"))
                }
            }
            is Commands.Approved -> {

            }
            else -> throw IllegalArgumentException("Unrecognised Command")
        }
    }

    interface Commands : CommandData {
        class AskConsent : Commands
        class Approved : Commands
    }
}