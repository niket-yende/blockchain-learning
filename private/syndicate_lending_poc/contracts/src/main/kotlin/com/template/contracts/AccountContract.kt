package com.template.contracts

import com.template.states.AccountState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class AccountContract: Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.template.contracts.AccountContract"
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<AccountContract.Commands>()

        when (command.value) {
            is AccountContract.Commands.Create -> {
                requireThat {
                    "There should be no input state included in the transaction!" using (tx.inputs.size == 0)
                    "There should be atleast one output state included in the transaction!" using (tx.outputs.size == 1)

                    val outputState = tx.getOutput(0) as AccountState

                    "AccountID is invalid!" using (outputState.accountId.isNotEmpty())
                    "Account Owner is invalid!" using (outputState.accountOwner.name.organisation.isNotEmpty())
                    "Account balance invalid!" using (outputState.balance >= 0)
                }
            }
            is AccountContract.Commands.Update -> {
                requireThat {
                    "There should be one input state included in the transaction!" using (tx.inputs.size == 1)
                    "There should be one output state included in the transaction!" using (tx.outputs.size == 1)
//
//                    val outputState = tx.getOutput(0) as AccountState
//
//                    "AccountID is invalid!" using (outputState.accountId.isNotEmpty())
//                    "Account Owner is invalid!" using (outputState.accountOwner.name.organisation.isNotEmpty())
//                    "Account balance invalid!" using (outputState.balance >= 0)
                }
            }
            //pending: Add validations for Account Updates
            else -> throw IllegalArgumentException("Unrecognised Command")
        }
    }

   interface Commands:CommandData {
       class Create: Commands
       class Update: Commands
   }
}


