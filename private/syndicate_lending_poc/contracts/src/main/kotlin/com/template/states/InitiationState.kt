package com.template.states

import com.template.contracts.TemplateContract
import com.template.schema.InitiationSchema
import com.template.schema.InitiationSchema1
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

@BelongsToContract(TemplateContract::class)
data class InitiationState(
        val loanRef: String,
        val issuerName: String,
        val entityType: String,
        val syndicationType: String,
        val loanType: String,
        val tenure: Int,
        val leadArranger: Party,
        override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, QueryableState {

    override val participants: List<AbstractParty> = listOf(leadArranger)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is InitiationSchema1 -> InitiationSchema1.PersistentInitiation(
                    this.loanRef,
                    this.issuerName,
                    this.entityType,
                    this.syndicationType,
                    this.loanType,
                    this.tenure,
                    this.leadArranger.name.toString(),
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(InitiationSchema1)
}