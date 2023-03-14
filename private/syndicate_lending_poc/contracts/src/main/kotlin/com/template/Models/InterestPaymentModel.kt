package com.template.Models

import com.template.states.InterestPaymentState
import net.corda.core.identity.Party
import java.time.LocalDate

data class InterestPaymentModel(val loanRef: String,
                                val paymentDate: String,
                                val paymentAccount: String,
                                val borrowerBank: String,
                                val leadArrangerAccount: String,
                                val leadArrangerBank: String,
                                val interestObligation: Double,
                                val paymentStatus: String,
                                val borrower: String,
                                val leadArranger: String,
                                val lenderA: String,
                                val lenderB: String,
                                var txIdsList: MutableList<String>
){
    constructor():this("","","","","","",0.0,"","","","",
            "", mutableListOf<String>())
}