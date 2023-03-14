package com.template.contracts

import com.template.states.StaticDataState
import com.template.states.TermSheetState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction


class StaticDataContract : Contract {
    // This is used to identify our contracts when building a transaction
    companion object {
        val ID = "com.template.contracts.StaticDataContract"
    }

    // A transaction is considered valid if the verify() function of the contracts of each of the transaction's input
    // and output states does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
//        val groups = tx.groupStates(BankState::withoutOwner)
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value){
            is Commands.Create -> {
                requireThat {
                    val outputState = tx.getOutput(0) as StaticDataState
//                    "There should be no input state included in the transaction" using (tx.inputs.size==0)
                    "There should be one output state included in the transaction" using (tx.outputs.size==1)
//                    "The borrower Account Number should not be empty" using (outputState.borrowerAccountNumber!="")
//                    "The borrower Loan Amount should not be zero" using (outputState.borrowerLoanAmount>0)
//                    "The lenderA Account Number should not be empty" using (outputState.lenderAAccountNumber!="")
//                    "The lenderA Contribution will not be greater than zero before subscription" using (outputState.lenderAContribution==0.0)
//                    "The lenderB Account Number should not be empty" using (outputState.lenderBAccountNumber!="")
//                    "The lenderB Contribution will not be greater than zero before subscription" using (outputState.lenderBContribution==0.0)
//                    "The lead Arranger Account Number should not be empty" using (outputState.leadArrangerAccountNumber!="")
//                    "The lead Arranger Contribution will not be greater than zero before subscription" using (outputState.leadArrangerContribution==0.0)
//                    "The rate Of Interest should not be zero" using (outputState.rateOfInterest>0.0)
//                    "The payingBank should not be empty" using (outputState.payingBank!="")
                }
            }
            is Commands.Update -> {
                requireThat {
                    val outputState = tx.getOutput(0) as StaticDataState
                    val inputState = tx.getInput(0) as StaticDataState
                    "There should be one input state included in the transaction" using (tx.inputs.size==1)
                    "There should be one output states included in the transaction" using (tx.outputs.size==1)
//                    "The borrower Account Number should not be empty" using (outputState.borrowerAccountNumber==inputState.borrowerAccountNumber)
//                    "The borrower Loan Amount should not be zero" using (outputState.borrowerLoanAmount==inputState.borrowerLoanAmount)
//                    "The lenderA Account Number should not be empty" using (outputState.lenderAAccountNumber==inputState.lenderAAccountNumber)
//                    "The lenderB Account Number should not be empty" using (outputState.lenderBAccountNumber==inputState.lenderBAccountNumber)
//                    "The lead Arranger Account Number should not be empty" using (outputState.leadArrangerAccountNumber==inputState.leadArrangerAccountNumber)
//                    "The lead Arranger and lenders Contribution is not matching with borrower loan amount" using (outputState.leadArrangerContribution+outputState.lenderAContribution+outputState.lenderBContribution==outputState.borrowerLoanAmount)
//                    "The rate Of Interest should not be zero" using (outputState.rateOfInterest==inputState.rateOfInterest)
//                    "The payingBank should not be empty" using (outputState.payingBank==inputState.payingBank)
                }
            }

            else -> throw IllegalArgumentException("Unrecognised Command")
        }

    }
    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Create: Commands
        class Update: Commands
    }

}