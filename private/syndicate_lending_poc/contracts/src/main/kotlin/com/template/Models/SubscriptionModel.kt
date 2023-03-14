package com.template.Models

import com.template.states.SubscriptionState
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.time.Instant

@CordaSerializable
data class SubscriptionModel(val subscriptionId: String,
                             val loanRef: String,
                             val subscriptionName: String,
                             val startDate: String,
                             val endDate: String,
                             val loanAmount: Double,
                             val tenure: Double,
                             val termSheet: String,
                             val lender: String,
                             val leadArranger: String,
                             val subscriptionAmount: Double,
                             val subscriptionStatus: String){
    constructor():this(
            "","","","","",0.0,0.0,"","","",0.0,""
    )
}