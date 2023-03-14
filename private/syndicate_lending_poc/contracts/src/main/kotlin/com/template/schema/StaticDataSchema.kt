package com.template.schema

import com.sun.corba.se.impl.protocol.INSServerRequestDispatcher
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.time.Instant
import java.time.LocalDate
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table


object StaticDataSchema

object StaticDataSchema1 : MappedSchema(
        schemaFamily = StaticDataSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentStaticData::class.java)) {
    @Entity
    @Table(name = "static_data_states")
    class PersistentStaticData(
            @Column(name = "loan_ref")
            var loan_ref: String,

            @Column(name = "borrower_name")
            var borrower_name: String,

            @Column(name = "borrower_account")
            var borrower_account: String,

            @Column(name = "borrower_bank")
            var borrower_bank: String,

            @Column(name = "borrower_loan_amount")
            var borrower_loan_amount: Double,

            @Column(name = "lenderA_name")
            var lenderA_name: String?="",

            @Column(name = "lenderA_account")
            var lenderA_account: String,

            @Column(name = "lenderA_bank")
            var lenderA_bank: String,

            @Column(name = "lenderA_contribution")
            var lenderA_contribution: Double?=null,

            @Column(name = "lenderB_name")
            var lenderB_name: String?="",

            @Column(name = "lenderB_account")
            var lenderB_account: String,

            @Column(name = "lenderB_bank")
            var lenderB_bank: String,

            @Column(name = "lenderB_contribution")
            var lenderB_contribution: Double?=null,

            @Column(name = "lead_arranger_name")
            var lead_arranger_name: String,

            @Column(name = "lead_arranger_account")
            var lead_arranger_account: String,

            @Column(name = "lead_arranger_contribution")
            var lead_arranger_contribution: Double?=null,

            @Column(name = "rateOfInterest")
            var rate_of_interest: Double,

            @Column(name = "frequency")
            var frequency: String,

            @Column(name = "payingBank")
            var paying_bank: String,

            @Column(name = "tenure")
            var tenure: Int,

            @Column(name = "start_date")
            var start_date: LocalDate?=null,

            @Column(name = "end_date")
            var end_date: LocalDate?=null,

            @Column(name = "payment_date")
            var payment_date: Int,

            @Column(name = "linear_id")
            var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.

        constructor(): this("","","","",0.0,"","","",0.0,"", "", "",0.0,"","",0.0, 0.0, "","",0, LocalDate.now(), LocalDate.now(),1, UUID.randomUUID())

    }
}

