package com.template.contracts

import com.template.states.SubscriptionState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class SubscriptionContract: Contract {
    companion object {
        val ID = "com.template.contracts.SubscriptionContract"
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()

        when (command.value) {
            is Commands.Create -> {
                requireThat {
                    "There should be no input state included in the transaction!" using (tx.inputs.size == 0)
                    "There should be one output state included in the transaction!" using (tx.outputs.size == 1)

                    val outputState = tx.getOutput(0) as SubscriptionState
                    "End Date should be greater than Start Date!" using (outputState.startDate.compareTo(outputState.endDate) < 0)
                    "LoanRef should not be empty" using  (outputState.loanRef!="")
                    "Tx. Loan Amount is invalid!" using (outputState.loanAmount > 0.0)
                    "Tx. Tenure is invalid!" using (outputState.tenure > 0.0)
                    "Tx. Subscription Amount is invalid! Expecting zero." using (outputState.subscriptionAmount == 0.0)
                    "Invalid Lender Identity!" using (outputState.lender?.name?.organisation in listOf<String>("LenderA", "LenderB"))
                    "Invalid LeadArranger Identity!" using (outputState.leadArranger.name.organisation.equals("LeadArranger"))
                    "Invalid status! Expecting Created" using (outputState.subscriptionStatus.equals(SubscriptionState.SubscriptionStatus.CREATED))
                }
            }

            is Commands.ApproveSubscription -> {
                requireThat {
                    "There should be one input state included in the transaction!" using (tx.inputs.size == 1)
                    "There should be one output state included in the transaction!" using (tx.outputs.size == 1)

                    val outputState = tx.getOutput(0) as SubscriptionState
                    val inputState = tx.getInput(0) as SubscriptionState

                    "End Date should be greater than Start Date!" using (outputState.startDate.compareTo(outputState.endDate) < 0)

                    "Tx. Loan Amount is invalid!" using (outputState.loanAmount > 0.0)
                    "Tx. Tenure is invalid!" using (outputState.tenure > 0.0)
                    "Tx. Subscription Amount is invalid!" using (outputState.subscriptionAmount > 0.0)
                    "Invalid Lender Identity!" using (outputState.lender?.name?.organisation in listOf<String>("LenderA", "LenderB"))
                    "Invalid LeadArranger Identity!" using (outputState.leadArranger.name.organisation.equals("LeadArranger"))
                    "Invalid status! Expecting Approved" using (outputState.subscriptionStatus.equals(SubscriptionState.SubscriptionStatus.APPROVED))
//                    "Input & Output do not match!" using (inputState.equals(outputState))
                }
            }
        }

    }

    interface Commands : CommandData {
        class Create: Commands
        class ApproveSubscription: Commands
    }
}