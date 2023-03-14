package com.template.states

import com.template.contracts.BidContract
import com.template.schema.BidSchema1
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

@BelongsToContract(BidContract::class)
data class BidState(val loanRef: String,
                    val subscriptionId: String,
                    val subscriptionName: String,
                    val startDate: LocalDate,
                    val endDate: LocalDate,
                    val loanAmount: Double,
                    val tenure: Double,
                    val lenderAName: String,
                    val lenderASubsAmount: Double,
                    val lenderBName: String,
                    val lenderBSubsAmount: Double,
                    val bidStatus: BidStatus,
                    val borrower: Party,
                    val leadArranger: Party,
                    override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, QueryableState {

    override val participants: List<AbstractParty> = listOf(leadArranger, borrower)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is BidSchema1 -> BidSchema1.PersistentBid(
                    this.loanRef,
                    this.subscriptionId,
                    this.subscriptionName,
                    this.startDate,
                    this.endDate,
                    this.loanAmount,
                    this.tenure,
                    this.lenderAName,
                    this.lenderASubsAmount,
                    this.lenderBName,
                    this.lenderBSubsAmount,
                    this.bidStatus,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is BidState) {
            return this.subscriptionId == other.subscriptionId &&
                    this.subscriptionName == other.subscriptionName &&
                    this.startDate == other.startDate &&
                    this.endDate == other.endDate &&
                    this.loanAmount == other.loanAmount &&
                    this.tenure == other.tenure &&
                    this.lenderAName == other.lenderAName &&
                    this.lenderASubsAmount == other.lenderASubsAmount &&
                    this.lenderBName == other.lenderBName &&
                    this.lenderBSubsAmount == other.lenderBSubsAmount &&
                    this.linearId == other.linearId
        } else
            return false
    }

    @CordaSerializable
    enum class BidStatus {
        CREATED,
        APPROVED
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(BidSchema1)
}