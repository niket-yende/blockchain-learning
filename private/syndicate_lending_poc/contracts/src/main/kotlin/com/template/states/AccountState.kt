package com.template.states

import com.template.contracts.AccountContract
import com.template.contracts.TemplateContract
import com.template.schema.AccountSchema1
import com.template.schema.TermSheetSchema1
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

@BelongsToContract(AccountContract::class)
data class AccountState(val loanRef: String,
                        val accountId: String,
                        val accountOwner: Party,
                        val balance: Double,
                        var transactionIds: MutableList<String>,
                        override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, QueryableState {
    override val participants: List<AbstractParty> = listOf(accountOwner)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is AccountSchema1 -> AccountSchema1.PersistentAccount(
                    this.loanRef,
                    this.accountId,
                    this.accountOwner.name.toString(),
                    this.balance,
                    this.transactionIds,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is AccountState) {
            return this.loanRef == other.loanRef &&
                    this.accountId == other.accountId &&
                    this.accountOwner == other.accountOwner &&
                    this.linearId == other.linearId

        } else
            return false
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(AccountSchema1)
}
