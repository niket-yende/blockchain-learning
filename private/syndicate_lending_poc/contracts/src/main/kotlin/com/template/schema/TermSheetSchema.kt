package com.template.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object TermSheetSchema

object TermSheetSchema1 : MappedSchema(
        schemaFamily = TermSheetSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentTermSheet::class.java)) {
    @Entity
    @Table(name = "term_sheet_states")
    class PersistentTermSheet(
            @Column(name = "loan_ref")
            var loan_ref: String,

            @Column(name = "borrower")
            var borrower: String,

            @Column(name = "type_of_loan")
            var type_of_loan: String,

            @Column(name = "lead_arranger")
            var lead_arranger: String,

            @Column(name = "status")
            var status: String,

            @Column(name = "file_hash")
            var file_hash: String,

            @Column(name = "linear_id")
            var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor() : this("", "", "", "", "", "", UUID.randomUUID())
    }
}