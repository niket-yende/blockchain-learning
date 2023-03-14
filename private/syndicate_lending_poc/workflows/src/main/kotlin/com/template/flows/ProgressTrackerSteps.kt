package com.template.flows

import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.utilities.ProgressTracker

object GENERATING_TRANSACTION : ProgressTracker.Step("Generating Transaction")
object SIGNING_TRANSACTION : ProgressTracker.Step("Signing Transaction")
object GATHERING_SIGNS : ProgressTracker.Step("Gathering Signs"){
    override fun childProgressTracker() = CollectSignaturesFlow.tracker()
}

object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining Notary signature and recording transaction") {
    override fun childProgressTracker() = FinalityFlow.tracker()
}