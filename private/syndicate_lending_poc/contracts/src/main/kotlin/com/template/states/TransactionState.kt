package com.template.states

import com.template.contracts.TemplateContract
import com.template.contracts.TransactionContract
import com.template.schema.AccountSchema1
import com.template.schema.TermSheetSchema1
import com.template.schema.TransactionSchema1
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

@BelongsToContract(TransactionContract::class)
data class TransactionState(val receiver: Party,
                            val sender: Party,
                            val fromAccount: String,
                            val toAccount: String,
                            val amount: Double,
        //   val paymentState: TransactionState.Status,
                            val txDate: Instant,
                            override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, QueryableState {
    override val participants: List<AbstractParty> = listOf(receiver, sender)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is TransactionSchema1 -> TransactionSchema1.PersistentTransaction(

                    this.receiver.name.toString(),
                    this.fromAccount,
                    this.toAccount,
                    this.sender.name.toString(),
                    this.amount,
                    //  this.paymentState,
                    this.txDate,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is TransactionState) {
            return this.receiver == other.receiver &&
                    this.fromAccount == other.fromAccount &&
                    this.toAccount == other.toAccount &&
                    this.sender == other.sender &&
                    this.amount == other.amount &&
                    //  this.paymentState == other.paymentState &&
                    this.txDate == other.txDate &&
                    this.linearId == other.linearId

        } else
            return false
    }

    //@CordaSerializable
//    enum class Status {
//        INITIATED,
//        COMPLETE
//    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(TransactionSchema1)
}