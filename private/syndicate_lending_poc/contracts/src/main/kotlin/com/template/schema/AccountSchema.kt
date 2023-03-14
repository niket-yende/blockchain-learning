package com.template.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.Table


object AccountSchema

object AccountSchema1 : MappedSchema(
        schemaFamily = AccountSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentAccount::class.java)) {
    @Entity
    @Table(name = "account_states")
    class PersistentAccount(
            @Column(name = "loan_ref")
            var loan_ref: String,

            @Column(name = "account_id")
            var account_id: String,

            @Column(name = "account_owner")
            var account_owner: String,

            @Column(name = "balance")
            var balance: Double,


            @Column(name = "transaction_ids")
            @ElementCollection
            var transaction_ids: MutableList<String>,

            @Column(name = "linear_id")
            var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", "", "", 0.0, mutableListOf(), UUID.randomUUID())
    }
}
