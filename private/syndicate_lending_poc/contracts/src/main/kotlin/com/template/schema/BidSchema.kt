package com.template.schema

import com.template.states.BidState
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.time.Instant
import java.time.LocalDate
import java.util.*
import javax.persistence.Entity
import javax.persistence.Table
import javax.persistence.Column

object BidSchema

object BidSchema1 : MappedSchema(
        schemaFamily = BidSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentBid::class.java)) {
    @Entity
    @Table(name = "bid_states")
    class PersistentBid(
            @Column(name = "loan_ref")
            var loan_ref: String,

            @Column(name = "subscription_id")
            var subscription_id: String,

            @Column(name = "subscription_name")
            var subscription_name: String,

            @Column(name = "start_date")
            var start_date: LocalDate,

            @Column(name = "end_date")
            var end_date: LocalDate,

            @Column(name = "loan_amount")
            var loan_amount: Double,

            @Column(name = "tenure")
            var tenure: Double,

            @Column(name = "lenderA_name")
            var lenderA_name: String,

            @Column(name = "lenderA_subs_amount")
            var lenderA_subs_amount: Double,

            @Column(name = "lenderB_name")
            var lenderB_name: String,

            @Column(name = "lenderB_subs_amount")
            var lenderB_subs_amount: Double,

            @Column(name = "bid_status")
            var bid_status: BidState.BidStatus,

            @Column(name = "linear_id")
            var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("","","", LocalDate.now(), LocalDate.now(),0.0, 0.0,"", 0.0,"", 0.0, BidState.BidStatus.CREATED, UUID.randomUUID())
    }
}