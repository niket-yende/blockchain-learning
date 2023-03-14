package com.template.states

import com.template.contracts.InterestPaymentContract
import com.template.schema.InterestPaymentSchema1
import net.corda.core.contracts.*
import net.corda.core.flows.FlowLogicRefFactory
import java.time.Instant
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*


@BelongsToContract(InterestPaymentContract::class)
data class InterestPaymentState(val loanRef: String,
                                val paymentDate: LocalDate,
                                val paymentAccount: String,
                                val borrowerBank: String,
                                val leadArrangerAccount: String,
                                val leadArrangerBank: String,
                                val interestObligation: Double,
                                val paymentStatus: PaymentStatus,
                                val borrower: Party,
                                val leadArranger: Party,
                                val lenderA: Party,
                                val lenderB: Party,
                                var txIdsList: MutableList<String>,
                                override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, QueryableState, SchedulableState {

    override val participants: List<AbstractParty> = listOf(leadArranger,lenderA, lenderB, borrower)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is InterestPaymentSchema1 -> InterestPaymentSchema1.PersistentInterestPayment(
                    this.loanRef,
                    this.paymentDate,
                    this.paymentAccount,
                    this.borrowerBank,
                    this.leadArrangerAccount,
                    this.leadArrangerBank,
                    this.interestObligation,
                    this.paymentStatus,
                    this.txIdsList,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    //auto triggers Borrower Payment once Interest is calculated
    override fun nextScheduledActivity(thisStateRef: StateRef, flowLogicRefFactory: FlowLogicRefFactory): ScheduledActivity? {

        if (paymentStatus.equals(PaymentStatus.BORROWER_PAID)) {
            println("Payment Initiated by Borrower to LA. Started UpdateAccountBalance Flow.")
            var sa: ScheduledActivity? = null
            try {
                val dataArray: Array<Any> = arrayOf(leadArranger.name.toString(), linearId.toString(), txIdsList.get(0), InterestPaymentState.PaymentStatus.PAID_LA,loanRef)
                val flowRef = flowLogicRefFactory.create("com.template.flows.UpdateAccountBalance", args = dataArray)
                val responseTime = Instant.now().minusSeconds(15)
                sa = ScheduledActivity(flowRef, responseTime)
            } catch (e: Exception) {
                println("Exception while scheduling activity: ${e.message}")

            }
            return sa
        }
        else if (paymentStatus.equals(PaymentStatus.PAID_LA)) {
            println("Payment Initiated by LA-LenderA. Started DisbursePaymentsFlow.")
            var sa: ScheduledActivity? = null
            try {
                //Call disburse Payments from La to L1 and L2
                val dataArray: Array<Any> = arrayOf(leadArranger.name.toString(), linearId.toString())
                val flowRef = flowLogicRefFactory.create("com.template.flows.DisbursePaymentsFlow", args = dataArray)
                 val responseTime = Instant.now().minusSeconds(15)
                 sa = ScheduledActivity(flowRef, responseTime)
            } catch (e: Exception) {
                println("Exception while scheduling activity: ${e.message}")
            }
            return sa
        }
        else if (paymentStatus.equals(PaymentStatus.PAY_LENDERS_INITIATED)) {
            println("Payment Initiated for LA-LenderB. Started UpdateAccountBalance Flow.")
            var sa: ScheduledActivity? = null
            try {
                val dataArray: Array<Any> = arrayOf(lenderA.name.toString(), linearId.toString(), txIdsList[1], InterestPaymentState.PaymentStatus.LENDERA_PAYMENT_COMPLETE,loanRef)

                val flowRef = flowLogicRefFactory.create("com.template.flows.UpdateAccountBalance", args = dataArray)
                val responseTime = Instant.now().minusSeconds(5)
                sa = ScheduledActivity(flowRef, responseTime)
            } catch (e: Exception) {
                println("Exception while scheduling activity: ${e.message}")
            }
            return sa
        }
        else if (paymentStatus.equals(PaymentStatus.LENDERA_PAYMENT_COMPLETE)) {
            println("Payment Initiated for LenderB. Started UpdateAccountBalance Flow.")
            var sa: ScheduledActivity? = null
            try {
                //Receiver,IP uniqueID,associatedTx Id,TargetIPstatus
                val dataArray: Array<Any> = arrayOf(lenderB.name.toString(), linearId.toString(), txIdsList[2], InterestPaymentState.PaymentStatus.LENDERB_PAYMENT_COMPLETE,loanRef)
                val flowRef = flowLogicRefFactory.create("com.template.flows.UpdateAccountBalance", args = dataArray)
                val responseTime = Instant.now().minusSeconds(15)
                sa = ScheduledActivity(flowRef, responseTime)
            } catch (e: Exception) {
                println("Exception while scheduling activity: ${e.message}")
            }
            return sa
        }
        return null
    }

    @CordaSerializable
    enum class PaymentStatus {
        CREATED,
        BORROWER_PAID,//PayintetstFlow
        PAID_LA,//updatebalance
        PAY_LENDERS_INITIATED,//disburse
        LENDERA_PAYMENT_COMPLETE,//LAPayment
        LENDERB_PAYMENT_COMPLETE//LBPayment
    }

    override fun equals(other: Any?): Boolean {
        if (other is InterestPaymentState) {
            return this.loanRef == other.loanRef &&
                    this.paymentDate == other.paymentDate &&
                    this.paymentAccount == other.paymentAccount &&
                    this.borrowerBank == other.borrowerBank &&
                    this.leadArrangerAccount == other.leadArrangerAccount &&
                    this.leadArrangerBank == other.leadArrangerBank &&
                    this.interestObligation == other.interestObligation &&
                    this.linearId == other.linearId
        } else
            return false
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(InterestPaymentSchema1)
}