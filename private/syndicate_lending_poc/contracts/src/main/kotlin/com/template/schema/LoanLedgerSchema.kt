package com.template.schema

import com.template.states.LoanLedgerState
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import java.util.function.DoubleUnaryOperator
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table


object LoanLedgerSchema

object LoanLedgerSchema1 : MappedSchema(
        schemaFamily = LoanLedgerSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentLoanLedger::class.java)) {
    @Entity
    @Table(name = "loan_ledger_states")
    class PersistentLoanLedger(

            @Column(name = "loan_ref")
            var loan_ref: String,

            @Column(name = "outstanding_loan")
            var outstanding_loan: Double,

            @Column(name = "borrower_account_id")
            var borrower_account_id: String,

            @Column(name = "lead_arranger_account_id")
            var lead_arranger_account_id: String,

            @Column(name = "lenderA_account_id")
            var lenderA_account_id: String,

            @Column(name = "lenderB_account_id")
            var lenderB_account_id: String,

            @Column(name = "interest_payout_tx_id")
            var interest_payout_tx_id: String,

            @Column(name = "status")
            var status: LoanLedgerState.LedgerStatus,

            @Column(name = "lenderA_Percentage")
            var lenderAPercentage: Double,

            @Column(name = "lenderB_Percentage")
            var lenderBPercentage: Double,

            @Column(name = "leadArranger_Percentage")
            var leadArrangerPercentage: Double,

            @Column(name = "linear_id")
            var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("",0.0,"","","","","",LoanLedgerState.LedgerStatus.CREATED,0.0,0.0,0.0, UUID.randomUUID())
    }
}

