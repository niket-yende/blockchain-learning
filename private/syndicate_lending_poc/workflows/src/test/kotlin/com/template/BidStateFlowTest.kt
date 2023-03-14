//package com.template
//
//import com.template.flows.*
//import com.template.states.BidState
//import com.template.states.StaticDataState
//import com.template.states.SubscriptionState
//import com.template.states.TermSheetState
//import net.corda.core.contracts.UniqueIdentifier
//import net.corda.core.crypto.SecureHash
//import net.corda.core.flows.FlowLogic
//import net.corda.core.identity.CordaX500Name
//import net.corda.core.transactions.SignedTransaction
//import net.corda.core.utilities.getOrThrow
//import net.corda.testing.node.MockNetwork
//import net.corda.testing.node.MockNetworkParameters
//import net.corda.testing.node.StartedMockNode
//import net.corda.testing.node.TestCordapp
//import org.junit.After
//import org.junit.Before
//import org.junit.Test
//import java.time.Duration
//import java.time.Instant
//
//class BidStateFlowTest {
//    private lateinit var network: MockNetwork
//    private lateinit var notary: StartedMockNode
//    private lateinit var borrower: StartedMockNode
//    private lateinit var leadArranger: StartedMockNode
//    private lateinit var lenderA: StartedMockNode
//    private lateinit var lenderB: StartedMockNode
//    private lateinit var termSheetState: TermSheetState
//    private lateinit var staticDataState: StaticDataState
//    private lateinit var subscriptionState: SubscriptionState
//    private lateinit var bidState: BidState
//
//    @Before
//    fun setup(){
//        network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
//                TestCordapp.findCordapp("com.template.contracts"),
//                TestCordapp.findCordapp("com.template.flows")
//        ), threadPerNode = true))
//        notary = network.createPartyNode(CordaX500Name.parse("O=Notary,L=London,C=GB"))
//        borrower = network.createPartyNode(CordaX500Name.parse("O=Borrower,L=London,C=GB"))
//        leadArranger = network.createPartyNode(CordaX500Name.parse("O=LeadArranger,L=New York,C=US"))
//        lenderA = network.createPartyNode(CordaX500Name.parse("O=LenderA,L=New York,C=US"))
//        lenderB = network.createPartyNode(CordaX500Name.parse("O=LenderB,L=New York,C=US"))
//        termSheetState = TermSheetState("L2D4",
//                borrower.info.legalIdentities.get(0),
//                "Senior_Loan_Unsecured",
//                leadArranger.info.legalIdentities.get(0),
//                268157.33,
//                TermSheetState.TermSheetStatus.CREATED,
//                SecureHash.zeroHash,
//                UniqueIdentifier())
////        listOf(borrower,leadArranger,lenderA,lenderB).forEach{it.registerInitiatedFlow(CreateTermSheetFlow::class.java)}
//
//        staticDataState= StaticDataState(
//                "L2D4",
//                borrower.info.legalIdentities.get(0),
//                "bac1",
//                "",
//                25000.0,
//                lenderA.info.legalIdentities.get(0),
//                "lac1",
//                "",
//                0.0,
//                lenderB.info.legalIdentities.get(0),
//                "lac2",
//                "",
//                0.0,
//                leadArranger.info.legalIdentities.get(0),
//                "laac2",
//                0.0,
//                5.0,
//                "dbc",
//                "",
//                5,
//                Instant.now(),
//                Instant.now().plusSeconds(12312312313123))
//
//        subscriptionState = SubscriptionState("SUB1",
//                "L2D4",
//                "SUB1",
//                Instant.now(),
//                Instant.now().plus(Duration.ofDays(365 * 5)),
//                100000.0,
//                5.0,
//                "Term_Sheet_Hash",
//                lenderA.info.legalIdentities.get(0),
//                leadArranger.info.legalIdentities.get(0),
//                0.0,
//                SubscriptionState.SubscriptionStatus.CREATED,
//                UniqueIdentifier())
//
//        bidState = BidState("L2D4",
//                "SUB1",
//                "SUB1",
//                Instant.now(),
//                Instant.now().plus(Duration.ofDays(365 * 5)),
//                100000.0,
//                5.0,
//                "LenderA",
//                20000.0,
//                "LenderB",
//                30000.0,
//                BidState.BidStatus.CREATED,
//                borrower.info.legalIdentities.get(0),
//                leadArranger.info.legalIdentities.get(0))
//
//        network.startNodes()
//    }
//
//    @After
//    fun tearDown() = network.stopNodes()
//
//    private fun StartedMockNode.runFlow(flow: FlowLogic<SignedTransaction>): SignedTransaction {
//        val future = startFlow(flow)
//        return future.getOrThrow()
//    }
//
//    private fun StartedMockNode.runFlowList(flow: FlowLogic<List<SignedTransaction>>): List<SignedTransaction> {
//        val future = startFlow(flow)
//        return future.getOrThrow()
//    }
//
//    @Test
//    fun createLoanAllocationTest(){
//        val createTermSheetFlow = CreateTermSheetFlow(termSheetState)
//        print(leadArranger.runFlow(createTermSheetFlow))
//
//        val approveTermSheetFlow = ApproveTermSheetFlow(termSheetState.loanRef)
//        print(borrower.runFlow(approveTermSheetFlow))
//
//        val createStaticDataFlow = CreateStaticDataFlow(staticDataState)
//        print(leadArranger.runFlow(createStaticDataFlow))
//
//        val createSubscriptionLedgerFlow = CreateSubscriptionLedgerFlow(subscriptionState,
//                "O=LenderA,L=New York,C=US",
//                "O=LenderB,L=New York,C=US")
//        print(leadArranger.runFlowList(createSubscriptionLedgerFlow))
//
//        var approveSubscriptionLedgerFlow = ApproveSubscriptionLedgerFlow("SUB1", 20000.0)
//        print(lenderA.runFlow(approveSubscriptionLedgerFlow))
//
//        approveSubscriptionLedgerFlow = ApproveSubscriptionLedgerFlow("SUB1", 30000.0)
//        print(lenderB.runFlow(approveSubscriptionLedgerFlow))
//
//        val approveBidFlow = CreateBidStateFlow("L2D4",
//                "O=LenderA,L=New York,C=US",
//                "O=LenderB,L=New York,C=US",
//                "O=Borrower,L=London,C=GB")
//        print(leadArranger.runFlow(approveBidFlow))
//
//        val createLoanAllocationFlow = LoanAllocationFlow(arrayOf("L2D4", "O=LeadArranger,L=New York,C=US"))
//        print(leadArranger.runFlowList(createLoanAllocationFlow))
//    }
//}