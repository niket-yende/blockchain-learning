package com.template.Models

import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.time.Instant

@CordaSerializable
data class StaticDataModel(val loanRef: String,
                           val borrower: String,
                           val borrowerAccountNumber: String,
                           val borrowerBank: String,
                           val borrowerLoanAmount: Double,
                           val lenderA: String,
                           val lenderAAccountNumber: String,
                           val lenderABank: String,
                           val lenderAContribution: Double?,
                           val lenderB: String,
                           val lenderBAccountNumber: String,
                           val lenderBBank: String,
                           val lenderBContribution: Double?,
                           val leadArranger: String,
                           val leadArrangerAccountNumber: String,
                           val leadArrangerContribution: Double?,
                           val rateOfInterest: Double,
                           val frequency: String,
                           val payingBank: String,
                           val tenure: Int,
                           val startDate: String,
                           val endDate: String
){
    constructor() : this(
            "","","","",0.0,"","","",0.0,"","",
            "",0.00,"","",0.0,0.0,"","",2, "",""
    )
}