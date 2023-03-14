package com.template.schema

import com.template.states.InterestPaymentState
import com.template.states.TransactionState
import net.corda.core.contracts.UniqueIdentifier
import java.time.Instant
import java.util.*
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.time.LocalDate
import javax.persistence.Entity
import javax.persistence.Table
import javax.persistence.Column
import javax.persistence.ElementCollection

object InterestPaymentSchema

object InterestPaymentSchema1 : MappedSchema(
        schemaFamily = InterestPaymentSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentInterestPayment::class.java)) {
    @Entity
    @Table(name = "interest_payment_states")
    class PersistentInterestPayment(


            @Column(name = "loan_reference")
            var loan_reference: String,

            @Column(name = "payment_date")
            var payment_date: LocalDate,

            @Column(name = "payment_account")
            var payment_account: String,

            @Column(name = "payingBank")
            var paying_bank: String,

            @Column(name = "lead_arranger_account")
            var lead_arranger_account: String,

            @Column(name = "lead_arranger_bank")
            var lead_arranger_bank: String,

            @Column(name = "interest_obligation")
            var interest_obligation: Double,

            @Column(name = "payment_status")
            var payment_status: InterestPaymentState.PaymentStatus,

            @Column(name = "tx_ids_list")
            @ElementCollection
            var txIdsList: MutableList<String>,

            @Column(name = "linear_id")
            var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", LocalDate.now(),"", "","", "", 0.0, InterestPaymentState.PaymentStatus.CREATED, mutableListOf(), UUID.randomUUID())
    }
}