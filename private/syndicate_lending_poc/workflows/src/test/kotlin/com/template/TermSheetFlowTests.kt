//package com.template
//
//import com.template.flows.*
//import com.template.states.*
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
//class TermSheetFlowTests {
//    private lateinit var network: MockNetwork
//    private lateinit var notary: StartedMockNode
//    private lateinit var borrower: StartedMockNode
//    private lateinit var leadArranger: StartedMockNode
//    private lateinit var lenderA: StartedMockNode
//    private lateinit var lenderB: StartedMockNode
//    private lateinit var initiationState: InitiationState
//    private lateinit var termSheetState: TermSheetState
//    private lateinit var staticDataState: StaticDataState
//
//    @Before
//    fun setup() {
//        network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
//                TestCordapp.findCordapp("com.template.contracts"),
//                TestCordapp.findCordapp("com.template.flows")
//        ), threadPerNode = true))
//        notary = network.createPartyNode(CordaX500Name.parse("O=Notary, L=London, C=GB"))
//        borrower = network.createPartyNode(CordaX500Name.parse("O=Borrower, L=London, C=GB"))
//        leadArranger = network.createPartyNode(CordaX500Name.parse("O=LeadArranger, L=New York, C=US"))
//        lenderA = network.createPartyNode(CordaX500Name.parse("O=LenderA, L=New York, C=US"))
//        lenderB = network.createPartyNode(CordaX500Name.parse("O=LenderB, L=New York, C=US"))
//
//        initiationState = InitiationState("LR1",
//                "O=LeadArranger, L=New York, C=US",
//                "Corporate",
//                "Underwritten",
//                "Senior_Loan_Unsecured",
//                5,
//                leadArranger.info.legalIdentities.get(0))
//
//        termSheetState = TermSheetState("LR1",
//                borrower.info.legalIdentities.get(0),
//                "Senior_Loan_Unsecured",
//                leadArranger.info.legalIdentities.get(0),
//                TermSheetState.TermSheetStatus.CREATED,
//                SecureHash.zeroHash,
//                UniqueIdentifier())
//
//        staticDataState = StaticDataState(
//                "LR1",
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
//                Instant.now().plus(Duration.ofDays(365 * 5)))
//
//        network.startNodes()
//    }
//
//    @After
//    fun tearDown() = network.stopNodes()
//
//    private fun StartedMockNode.runFlow(flow: FlowLogic<SignedTransaction>): SignedTransaction {
//        val future = startFlow(flow)
////        network.runNetwork()
//        return future.getOrThrow()
//    }
//
//    private fun StartedMockNode.runFlowList(flow: FlowLogic<List<SignedTransaction>>): List<SignedTransaction> {
//        val future = startFlow(flow)
//        return future.getOrThrow()
//    }
//
//    @Test
//    fun LAInitiationTest() {
//        val LAInitiationFlow = LAInitiationFlow(initiationState)
//        print(leadArranger.runFlow(LAInitiationFlow))
//    }
//
////    @Test
////    fun UploadFlowTest(){
////        LAInitiationTest()
////        val uploadFlow = UploadFlow(uploadState)
////        print(leadArranger.runFlow(uploadFlow))
////
////        Thread.sleep(5000)
////
////        leadArranger.transaction {
////            val jdbcSession = leadArranger.services.jdbcSession()
////            val nativeQuery = "SELECT * FROM term_sheet_states;"
////            val prepStatement = jdbcSession.prepareStatement(nativeQuery)
////            val rs = prepStatement.executeQuery()
////            print("=============inside jdbc")
////            while (rs.next()) {
////                println("Count: "+rs.getArray("type_of_loan"))
////            }
////        }
////    }
//
//    @Test
//    fun createTermSheetTest() {
//        LAInitiationTest()
//        val createTermSheetFlow = CreateTermSheetFlow()
//        print(leadArranger.runFlow(createTermSheetFlow))
//    }
//
////    @Test
////    fun approveTermSheetTest(){
////        createTermSheetTest()
////        val approveTermSheetFlow = ApproveTermSheetFlow(termSheetState.loanRef)
////        print(borrower.runFlow(approveTermSheetFlow))
////    }
////
////    @Test
////    fun createStaticDataTest(){
////        val createStaticDataFlow = CreateStaticDataFlow(staticDataState)
////        print(leadArranger.runFlow(createStaticDataFlow))
////    }
//}