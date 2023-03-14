package com.template.Models

import com.template.states.BidState
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.time.Instant

@CordaSerializable
data class BidModel(val loanRef: String,
                    val subscriptionId: String,
                    val subscriptionName: String,
                    val startDate: String,
                    val endDate: String,
                    val loanAmount: Double,
                    val tenure: Double,
                    val lenderAName: String,
                    val lenderASubsAmount: Double,
                    val lenderBName: String,
                    val lenderBSubsAmount: Double,
                    val bidStatus: String,
                    val borrower: String,
                    val leadArranger: String){
    constructor():this(
            "","","","","",0.0,0.0,"",0.0,
            "",0.0,"","",""
    )
}