package com.template.states

import com.template.contracts.TermSheetContract
import com.template.schema.TermSheetSchema1
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.SchedulableState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable

@BelongsToContract(TermSheetContract::class)
data class TermSheetState(val loanRef: String,
                          val borrower: Party,
                          val typeOfLoan: String,
                          val leadArranger: Party,
                          val status: TermSheetStatus,
                          val fileHash: String,
                          override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, QueryableState {
    override val participants: List<AbstractParty> = listOf(borrower, leadArranger)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is TermSheetSchema1 -> TermSheetSchema1.PersistentTermSheet(
                    this.loanRef,
                    this.borrower.name.toString(),
                    this.typeOfLoan,
                    this.leadArranger.name.toString(),
                    this.status.toString(),
                    this.fileHash,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    @CordaSerializable
    enum class TermSheetStatus {
        CREATED,
        APPROVED
    }

    override fun equals(other: Any?): Boolean {
        if (other is TermSheetState) {
            return this.borrower == other.borrower &&
                    this.typeOfLoan == other.typeOfLoan &&
                    this.leadArranger == other.leadArranger &&
                    this.fileHash == other.fileHash &&
                    this.linearId == other.linearId
        } else
            return false
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(TermSheetSchema1)
}