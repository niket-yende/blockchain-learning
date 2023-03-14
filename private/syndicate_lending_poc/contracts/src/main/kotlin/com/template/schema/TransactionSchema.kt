package com.template.schema

import com.template.states.TransactionState
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.time.Instant
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table




object TransactionSchema

object TransactionSchema1 : MappedSchema(
        schemaFamily = TransactionSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentTransaction::class.java)) {
    @Entity
    @Table(name = "transaction_states")
    class PersistentTransaction(


            @Column(name = "receiver")
            var receiver: String,

            @Column(name = "from_account")
            var from_account: String,

            @Column(name = "to_account")
            var to_account: String,

            @Column(name = "sender")
            var sender: String,

            @Column(name = "amount")
            var amount: Double,

//            @Column(name = "paymentState")
//            var paymentState: TransactionState.Status,

            @Column(name = "tx_Date")
            var txDate: Instant,

            @Column(name = "linear_id")
            var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this( "",  "","","",0.0,Instant.now(), UUID.randomUUID())
    }
}