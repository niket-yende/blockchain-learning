package com.template

import com.template.flows.*
import com.template.states.*
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.VaultService
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.node.services.vault.VaultSchemaV1
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class SubscriptionFlowTests {
    private lateinit var network: MockNetwork
    private lateinit var notary: StartedMockNode
    private lateinit var borrower: StartedMockNode
    private lateinit var leadArranger: StartedMockNode
    private lateinit var lenderA: StartedMockNode
    private lateinit var lenderB: StartedMockNode
    private lateinit var initiationState: InitiationState
    private lateinit var termSheetState: TermSheetState
    private lateinit var staticDataState: StaticDataState
    private lateinit var subscriptionState: SubscriptionState
    private lateinit var bidState: BidState
    private lateinit var loanLedgerState: LoanLedgerState
    private lateinit var accountState: AccountState
    private lateinit var interestPaymentState: InterestPaymentState

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("com.template.contracts"),
                TestCordapp.findCordapp("com.template.flows")
        ), threadPerNode = true))

        notary = network.createPartyNode(CordaX500Name.parse("O=Notary, L=London, C=GB"))
        borrower = network.createPartyNode(CordaX500Name.parse("O=Borrower, L=London, C=GB"))
        leadArranger = network.createPartyNode(CordaX500Name.parse("O=LeadArranger, L=New York, C=US"))
        lenderA = network.createPartyNode(CordaX500Name.parse("O=LenderA, L=New York, C=US"))
        lenderB = network.createPartyNode(CordaX500Name.parse("O=LenderB, L=New York, C=US"))

        initiationState = InitiationState("LR1",
                "O=LeadArranger, L=New York, C=US",
                "Corporate",
                "Underwritten",
                "Senior_Loan_Unsecured",
                5,
                leadArranger.info.legalIdentities.get(0))

        termSheetState = TermSheetState("LR1",
                borrower.info.legalIdentities.get(0),
                "Senior_Loan_Unsecured",
                leadArranger.info.legalIdentities.get(0),
                TermSheetState.TermSheetStatus.CREATED,
                SecureHash.zeroHash.toString(),
                UniqueIdentifier())

        staticDataState = StaticDataState("LR1",
                borrower.info.legalIdentities.get(0),
                "bac1",
                "BBank",
                100.0,
                leadArranger.info.legalIdentities.get(0),
                "",
                "",
                0.0,
                leadArranger.info.legalIdentities.get(0),
                "",
                "",
                0.0,
                leadArranger.info.legalIdentities.get(0),
                "laac1",
                0.0,
                5.0,
                "",
                "LABank",
                5,
                LocalDate.now(),
                LocalDate.now().plusYears(5),
                1)


        subscriptionState = SubscriptionState("S111",
                "LR1",
                "SUB1",
                LocalDate.now(),
                LocalDate.now().plusYears(5),
                100.0,
                5.0,
                SecureHash.zeroHash.toString(),
                lenderA.info.legalIdentities.get(0),
                leadArranger.info.legalIdentities.get(0),
                0.0,
                SubscriptionState.SubscriptionStatus.CREATED)

        bidState = BidState("LR1",
                "S111",
                "SUB1",
                LocalDate.now(),
                LocalDate.now().plusYears(5),
                100.0,
                5.0,
                "LenderA",
                20.0,
                "LenderB",
                30.0,
                BidState.BidStatus.CREATED,
                borrower.info.legalIdentities.get(0),
                leadArranger.info.legalIdentities.get(0))

        loanLedgerState = LoanLedgerState(borrower.info.legalIdentities.get(0),
                lenderA.info.legalIdentities.get(0),
                lenderB.info.legalIdentities.get(0),
                leadArranger.info.legalIdentities.get(0),
                "LR1",
                100.0,
                "bac1",
                "laac1",
                "l1c1",
                "l2c1",
                "q",
                20.0,
                30.0,
                5.0,
                LoanLedgerState.LedgerStatus.CREATED)

        accountState = AccountState("LR1",
                "bac1",
                borrower.info.legalIdentities.get(0),
                100.0,
                mutableListOf())

        interestPaymentState = InterestPaymentState("LR1",
                LocalDate.now(),
                "bac1",
                "BBank",
                "laac1",
                "LABank",
                100.0,
                InterestPaymentState.PaymentStatus.BORROWER_PAID,
                borrower.info.legalIdentities.get(0),
                leadArranger.info.legalIdentities.get(0),
                lenderA.info.legalIdentities.get(0),
                lenderB.info.legalIdentities.get(0),
                mutableListOf())

        network.startNodes()
    }

    @After
    fun tearDown() = network.stopNodes()

    private fun StartedMockNode.runFlow(flow: FlowLogic<SignedTransaction>): SignedTransaction {
        val future = startFlow(flow)
        return future.getOrThrow()
    }

    private fun StartedMockNode.runFlowList(flow: FlowLogic<List<SignedTransaction>>): List<SignedTransaction> {
        val future = startFlow(flow)
        return future.getOrThrow()
    }


    @Test
    fun endToEndTest() {
        //CREATE INITIATION STATE
        val createInitiationState = LAInitiationFlow(initiationState)
        println(leadArranger.runFlow(createInitiationState))

        //UPLOAD ATTACHMENT
        val attachmentId = leadArranger.transaction {
            leadArranger.services.attachments.importAttachment(Files.newInputStream(Paths.get("/Users/manikantavarma.k/Downloads/API_DOC.docx")), "", "cordaTestImage.zip")
        }

        //CREATE TERM SHEET
        val createTermSheetFlow = CreateTermSheetFlow("LR1", attachmentId.toString(), leadArranger.info.legalIdentities.get(0))
        println(leadArranger.runFlow(createTermSheetFlow))

        //APPROVE TERM SHEET
        val approveTermSheetFlow = ApproveTermSheetFlow(termSheetState.loanRef)
        println(borrower.runFlow(approveTermSheetFlow))

        //CREATE STATIC DATA
        val createStaticDataFLow = CreateStaticDataFlow(staticDataState)
        println(leadArranger.runFlow(createStaticDataFLow))

        //UPDATE STATIC DATA
        var updateStaticDataFlow = UpdateStaticDataFlow(staticDataState.copy(lenderA = lenderA.info.legalIdentities.get(0),
                lenderAAccountNumber = "l1c1",
                lenderABank = "L1Bank",
                lenderB = lenderB.info.legalIdentities.get(0),
                lenderBAccountNumber = "l2c1",
                lenderBBank = "L2Bank"))

        println(leadArranger.runFlow(updateStaticDataFlow))

        //CREATE SUBSCRIPTION LEDGER
        val createSubscriptionLedgerFlow = CreateSubscriptionLedgerFlow(subscriptionState,
                "O=LenderA, L=New York, C=US",
                "O=LenderB, L=New York, C=US")

        println(leadArranger.runFlowList(createSubscriptionLedgerFlow))

        //APPROVE SUBSCRIPTION LEDGER
        var approveSubscriptionLedgerFlow = ApproveSubscriptionLedgerFlow("LR1", 20.0)
        println(lenderA.runFlow(approveSubscriptionLedgerFlow))

        approveSubscriptionLedgerFlow = ApproveSubscriptionLedgerFlow("LR1", 30.0)
        println(lenderB.runFlow(approveSubscriptionLedgerFlow))

        //CREATE BID
        val createBidFlow = CreateBidStateFlow(bidState)
        println(leadArranger.runFlowList(createBidFlow))

        //APPROVE BID
        val approveBidFlow = UpdateBidStateFlow(bidState)
        println(borrower.runFlow(approveBidFlow))

        //CREATE LOAN LEDGER
        val createLoanLedgerFlow = CreateLoanLedgerFlow(loanLedgerState, accountState)
        println(borrower.runFlowList(createLoanLedgerFlow))

        Thread.sleep(1000 * 10)

        //UPDATE STATIC DATA
        val staticDataLatest: StaticDataState
        staticDataLatest = borrower.services.vaultService.queryBy<StaticDataState>().states.get(0).state.data
        updateStaticDataFlow = UpdateStaticDataFlow(staticDataLatest.copy(frequency = "Quarterly", paymentDate = 1))
        println(borrower.runFlow(updateStaticDataFlow))


        //LOAN SERVICING
        val createInterestPaymentFlow = CreateInterestPaymentFlow("LR1", leadArranger.info.legalIdentities.get(0).name.toString())
        println(leadArranger.runFlow(createInterestPaymentFlow))

//        Thread.sleep(1000 * 5)
        val payInterestFlow = PayInterestFlow(interestPaymentState)
        println(borrower.runFlowList(payInterestFlow))

        Thread.sleep(1000 * 5)

        //VAULT QUERIES
        vaultQueries()
    }

    //VAULT QUERIES
    fun vaultQueries() {
        borrower.transaction {
            var staticData = borrower.services.vaultService.queryBy<StaticDataState>().states
            for (i in staticData) {
                println("\nB Static State: " + i.state.data.toString())
            }
        }

        leadArranger.transaction {
            var staticData = leadArranger.services.vaultService.queryBy<StaticDataState>().states
            for (i in staticData) {
                println("\nLA Static State: " + i.state.data.toString())
            }

            val loanLedger = leadArranger.services.vaultService.queryBy<LoanLedgerState>().states
            for (i in loanLedger) {
                println("\nLA loanLedger State: " + i.state.data.toString())
            }

            val subLedger = leadArranger.services.vaultService.queryBy<SubscriptionState>().states
            for (i in subLedger) {
                println("\nLA subLedger State: " + i.state.data.toString())
            }

            val bidState = leadArranger.services.vaultService.queryBy<BidState>().states
            for (i in bidState) {
                println("\nLA bid State: " + i.state.data.toString())
            }

            val LAAccState = leadArranger.services.vaultService.queryBy<AccountState>().states
            for (i in LAAccState) {
                println("\nLA Account State: " + i.state.data.toString())
            }
        }


        borrower.transaction {
            val BAccState = borrower.services.vaultService.queryBy<AccountState>().states
            for (i in BAccState) {
                println("\nB Account State: " + i.state.data.toString())
            }
        }

        borrower.transaction {
            val jdbcSession = borrower.services.jdbcSession()
            val nativeQuery = "SELECT * FROM account_states;"
            val prepStatement = jdbcSession.prepareStatement(nativeQuery)
            val rs = prepStatement.executeQuery()
            while (rs.next()) {
                println("Count: "+rs.getArray("linear_id"))
                println("Count: "+rs.getArray("balance"))
            }
        }

        lenderA.transaction {
            val L1AccState = lenderA.services.vaultService.queryBy<AccountState>().states
            for (i in L1AccState) {
                println("\nL1 Account State: " + i.state.data.toString())
            }
        }

        lenderA.transaction {
            val jdbcSession = lenderA.services.jdbcSession()
            val nativeQuery = "SELECT * FROM account_states;"
            val prepStatement = jdbcSession.prepareStatement(nativeQuery)
            val rs = prepStatement.executeQuery()
            while (rs.next()) {
                println("Count: "+rs.getArray("linear_id"))
                println("Count: "+rs.getArray("balance"))
            }
        }

        lenderB.transaction {
            val L2AccState = lenderB.services.vaultService.queryBy<AccountState>().states
            for (i in L2AccState) {
                println("\nL2 Account State: " + i.state.data.toString())
            }
        }

        leadArranger.transaction {
            val interestPaymentState = leadArranger.services.vaultService.queryBy<InterestPaymentState>().states
            for (i in interestPaymentState) {
                println("\nL2 Interest Payment State: " + i.state.data.toString())
            }
        }

        borrower.transaction {
            val transactionState = borrower.services.vaultService.queryBy<TransactionState>().states
            for (i in transactionState) {
                println("\nB Transaction State: " + i.state.data.toString())
            }
        }

        lenderA.transaction {
            val transactionState = lenderA.services.vaultService.queryBy<TransactionState>().states
            for (i in transactionState) {
                println("\nL1 Transaction State: " + i.state.data.toString())
            }
        }

        lenderB.transaction {
            val transactionState = lenderB.services.vaultService.queryBy<TransactionState>().states
            for (i in transactionState) {
                println("\nL2 Transaction State: " + i.state.data.toString())
            }
        }
    }
}



//HANDY CODE
//        borrower.transaction {
//            val jdbcSession = borrower.services.jdbcSession()
//            val nativeQuery = "SELECT * FROM bid_states;"
//            val prepStatement = jdbcSession.prepareStatement(nativeQuery)
//            val rs = prepStatement.executeQuery()
//            while (rs.next()) {
//                println("Count: "+rs.getArray("loan_ref"))
//            }
//        }