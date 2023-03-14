package com.template.states

import com.template.contracts.StaticDataContract
import com.template.schema.StaticDataSchema1
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.time.LocalDate

@BelongsToContract(StaticDataContract::class)
data class StaticDataState(val loanRef: String,
                           val borrower: Party,
                           val borrowerAccountNumber: String,
                           val borrowerBank: String,
                           val borrowerLoanAmount: Double,
                           val lenderA: Party,
                           val lenderAAccountNumber: String,
                           val lenderABank: String,
                           val lenderAContribution: Double?,
                           val lenderB: Party,
                           val lenderBAccountNumber: String,
                           val lenderBBank: String,
                           val lenderBContribution: Double?,
                           val leadArranger: Party,
                           val leadArrangerAccountNumber: String,
                           val leadArrangerContribution: Double?,
                           val rateOfInterest: Double,
                           val frequency: String,
                           val payingBank: String,
                           val tenure: Int,
                           val startDate: LocalDate,
                           val endDate: LocalDate,
                           val paymentDate: Int,
                           override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, QueryableState {
    override val participants: List<AbstractParty> = listOf(leadArranger, borrower)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is StaticDataSchema1 -> StaticDataSchema1.PersistentStaticData(
                    this.loanRef,
                    this.borrower.name.toString(),
                    this.borrowerAccountNumber,
                    this.borrowerBank,
                    this.borrowerLoanAmount,
                    this.lenderA.name.toString(),
                    this.lenderAAccountNumber,
                    this.lenderABank,
                    this.lenderAContribution,
                    this.lenderB.name.toString(),
                    this.lenderBAccountNumber,
                    this.lenderBBank,
                    this.lenderBContribution,
                    this.leadArranger.name.toString(),
                    this.leadArrangerAccountNumber,
                    this.leadArrangerContribution,
                    this.rateOfInterest,
                    this.frequency,
                    this.payingBank,
                    this.tenure,
                    this.startDate,
                    this.endDate,
                    this.paymentDate,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is StaticDataState) {
            return this.loanRef == other.loanRef &&
                    this.borrower == other.borrower &&
                    this.borrowerAccountNumber == other.borrowerAccountNumber &&
                    this.borrowerLoanAmount == other.borrowerLoanAmount &&
                    this.lenderA == other.lenderA &&
                    this.lenderAAccountNumber == other.lenderAAccountNumber &&
                    this.lenderAContribution == other.lenderAContribution &&
                    this.lenderB == other.lenderB &&
                    this.lenderBAccountNumber == other.lenderBAccountNumber &&
                    this.lenderBContribution == other.lenderBContribution &&
                    this.leadArranger == other.leadArranger &&
                    this.leadArrangerAccountNumber == other.leadArrangerAccountNumber &&
                    this.leadArrangerContribution == other.leadArrangerContribution &&
                    this.rateOfInterest == other.rateOfInterest &&
                    this.frequency == other.frequency &&
                    this.payingBank == other.payingBank &&
                    this.tenure == other.tenure &&
                    this.paymentDate == other.paymentDate &&
                    this.linearId == other.linearId
        } else
            return false
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(StaticDataSchema1)
}