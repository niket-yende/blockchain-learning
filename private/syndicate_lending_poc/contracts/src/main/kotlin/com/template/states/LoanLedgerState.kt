package com.template.states

import com.template.contracts.LoanLedgerContract
import com.template.contracts.TermSheetContract
import com.template.schema.LoanLedgerSchema1
import com.template.schema.TermSheetSchema1
import net.corda.core.contracts.*
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogicRefFactory
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable
import java.time.Instant

@BelongsToContract(LoanLedgerContract::class)
data class LoanLedgerState(val borrower: Party,
                           val lenderA: Party,
                           val lenderB: Party,
                           val leadArranger: Party,
                           val loanRef: String,
                           val outstandingLoan: Double,
                           val borrowerAccountId: String,
                           val leadArrangerAccountId: String,
                           val lenderAAccountId: String,
                           val lenderBAccountId: String,
                           val interestPayoutTxId: String,
                           val lenderAPercentage: Double,
                           val lenderBPercentage: Double,
                           val leadArrangerPercentage: Double,
                           val status: LedgerStatus,
                           override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, QueryableState, SchedulableState {

    override val participants: List<AbstractParty> = listOf(borrower, leadArranger, lenderA, lenderB)

    override fun nextScheduledActivity(thisStateRef: StateRef, flowLogicRefFactory: FlowLogicRefFactory): ScheduledActivity? {

        if (status.equals(LedgerStatus.CREATED)) {

            val dataArray = arrayOf(loanRef, leadArranger.name)

            println("Schedule activity for Loan Allocation")
            var sa: ScheduledActivity? = null
            try {
                val flowRef = flowLogicRefFactory.create("com.template.flows.LoanAllocationFlow", args = dataArray)

                val responseTime = Instant.now().minusSeconds(25)
                sa = ScheduledActivity(flowRef, responseTime)
            } catch (e: Exception) {
                println("Exception while scheduling activity: ${e.message}")
            }

            return sa
        } else if (status.equals(LedgerStatus.UPDATED)){
            val dataArray = arrayOf(loanRef, leadArranger.name)

            println("Schedule activity for CreateAccountsOnLoanLedgerFlow")
            var sa: ScheduledActivity? = null
            try {
                val flowRef = flowLogicRefFactory.create("com.template.flows.CreateAccountsOnLoanLedgerFlow", args = dataArray)

                val responseTime = Instant.now().minusSeconds(25)
                sa = ScheduledActivity(flowRef, responseTime)
            } catch (e: Exception) {
                println("Exception while scheduling activity: ${e.message}")
            }
            return sa
        }
        return null
    }

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is LoanLedgerSchema1 -> LoanLedgerSchema1.PersistentLoanLedger(
                    this.loanRef,
                    this.outstandingLoan,
                    this.borrowerAccountId,
                    this.leadArrangerAccountId,
                    this.lenderAAccountId,
                    this.lenderBAccountId,
                    this.interestPayoutTxId,
                    this.status,
                    this.lenderAPercentage,
                    this.lenderBPercentage,
                    this.leadArrangerPercentage,
                    this.linearId.id)
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    @CordaSerializable
    enum class LedgerStatus {
        CREATED,
        UPDATED,
        OUTSTANDING_AMT_UPDATED,
        LOAN_RECOVERED
    }

    override fun equals(other: Any?): Boolean {
        if (other is LoanLedgerState) {
            return this.borrower == other.borrower &&
                    this.lenderA == other.lenderA &&
                    this.lenderB == other.lenderB &&
                    this.leadArranger == other.leadArranger &&
                    this.outstandingLoan == other.outstandingLoan &&
                    this.leadArrangerAccountId == other.leadArrangerAccountId &&
                    this.lenderAAccountId == other.lenderAAccountId &&
                    this.lenderBAccountId == other.lenderBAccountId &&
                    this.borrowerAccountId == other.borrowerAccountId &&
                    this.interestPayoutTxId == other.interestPayoutTxId &&
                    this.status == other.status &&
                    this.lenderAPercentage == other.lenderAPercentage &&
                    this.lenderBPercentage == other.lenderBPercentage &&
                    this.leadArrangerPercentage == other.leadArrangerPercentage &&
                    this.linearId == other.linearId
        } else
            return false
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(LoanLedgerSchema1)
}