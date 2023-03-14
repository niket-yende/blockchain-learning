package com.template.schema

import com.template.states.SubscriptionState
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.time.Instant
import java.time.LocalDate
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table


object SubscriptionSchema

object SubscriptionSchema1 : MappedSchema(
        schemaFamily = SubscriptionSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentSubsciption::class.java)) {
    @Entity
    @Table(name = "subscription_states")
    class PersistentSubsciption(
            @Column(name = "subscription_id")
            var subscription_id: String,

            @Column(name = "loan_ref")
            var loan_ref: String,


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

            @Column(name = "term_sheet")
            var term_sheet: String,

            @Column(name = "lender_name")
            var lender_name: String,

            @Column(name = "lead_arranger_name")
            var lead_arranger_name: String,

            @Column(name = "subscription_amount")
            var subscription_amount: Double,

            @Column(name = "subscription_status")
            var subscription_status: SubscriptionState.SubscriptionStatus,

            @Column(name = "linear_id")
            var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("","","", LocalDate.now(),LocalDate.now(),0.0, 0.0,"", "","", 0.0,SubscriptionState.SubscriptionStatus.CREATED, UUID.randomUUID())
    }
}