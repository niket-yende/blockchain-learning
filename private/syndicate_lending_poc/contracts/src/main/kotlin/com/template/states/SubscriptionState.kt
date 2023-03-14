package com.template.states

import com.template.contracts.SubscriptionContract
import com.template.schema.SubscriptionSchema1
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable
import java.time.Instant
import java.time.LocalDate


@BelongsToContract(SubscriptionContract::class)
data class SubscriptionState(val subscriptionId: String,
                             val loanRef: String,
                             val subscriptionName: String,
                             val startDate: LocalDate,
                             val endDate: LocalDate,
                             val loanAmount: Double,
                             val tenure: Double,
                             val termSheet: String,
                             val lender: Party,
                             val leadArranger: Party,
                             val subscriptionAmount: Double,
                             val subscriptionStatus:SubscriptionStatus,
                           override val linearId: UniqueIdentifier = UniqueIdentifier()): LinearState, QueryableState {
    override val participants: List<AbstractParty> = listOf(leadArranger,lender as Party)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is SubscriptionSchema1 -> SubscriptionSchema1.PersistentSubsciption(
                    this.subscriptionId,
                    this.loanRef,
                    this.subscriptionName,
                    this.startDate,
                    this.endDate,
                    this.loanAmount,
                    this.tenure,
                    this.termSheet,
                    this.lender.name.toString(),
                    this.leadArranger.name.toString(),
                    this.subscriptionAmount,
                    this.subscriptionStatus,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    @CordaSerializable
    enum class SubscriptionStatus{
        CREATED,
        APPROVED
    }

    override fun equals(other: Any?): Boolean {
        if(other is SubscriptionState) {
            return this.subscriptionId == other.subscriptionId &&
                    this.subscriptionName == other.subscriptionName &&
                    this.startDate == other.startDate &&
                    this.endDate == other.endDate &&
                    this.loanAmount == other.loanAmount &&
                    this.tenure == other.tenure &&
                    this.termSheet == other.termSheet &&
                    this.lender == other.lender &&
                    this.leadArranger == other.leadArranger &&
                    this.subscriptionAmount == other.subscriptionAmount &&
                    this.linearId == other.linearId

        } else
            return false
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(SubscriptionSchema1)

}
