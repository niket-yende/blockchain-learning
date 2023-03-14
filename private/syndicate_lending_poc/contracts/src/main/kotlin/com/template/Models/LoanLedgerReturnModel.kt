package com.template.Models

import com.template.states.LoanLedgerState
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class LoanLedgerReturnModel(val borrower: String,
                                 val lenderA: String,
                                 val lenderB: String,
                                 val leadArranger: String,
                                 val loanRef: String,
                                 val outstandingLoan: Double,
                                 val borrowerAccountId: String,
                                 val leadArrangerAccountId: String,
                                 val lenderAAccountId: String,
                                 val lenderBAccountId: String,
                                 val interestPayoutTxId: String,
                                 val lenderAPercentage: Double,
                                 val lenderBPercentage: Double,
                                 val leadArrangerPercentage: Double,
                                 val status: String){
    constructor():this(
            "","","","","",0.0,"","","","","",0.0,
            0.0,0.0,"")
}