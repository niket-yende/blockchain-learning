//package com.template
//
//import com.template.flows.ApproveTermSheetFlow
//import com.template.flows.CreateStaticDataFlow
//import com.template.flows.CreateTermSheetFlow
//import com.template.flows.UpdateStaticDataFlow
//import com.template.states.StaticDataState
//import com.template.states.TermSheetState
//import net.corda.core.contracts.UniqueIdentifier
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
//import java.time.Instant
//
//
//class StaticDataFlowTest{
//    private lateinit var network: MockNetwork
//    private lateinit var notary: StartedMockNode
//    private lateinit var borrower: StartedMockNode
//    private lateinit var leadArranger: StartedMockNode
//    private lateinit var lenderA: StartedMockNode
//    private lateinit var lenderB: StartedMockNode
//    private lateinit var staticDataState: StaticDataState
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
//        staticDataState=StaticDataState("L2D5",
//                borrower.info.legalIdentities.get(0),
//                "bac1",
//                "bank1",
//                100000.0,
//                lenderA.info.legalIdentities.get(0),
//                "lac1",
//                "bank2",
//                0.0,
//                lenderB.info.legalIdentities.get(0),
//                "lac2",
//                "bank3",
//                0.0,
//                leadArranger.info.legalIdentities.get(0),
//                "laac2",
//                0.0,
//                5.0,
//                "dbc","bank4",2, Instant.now(), Instant.now().plusSeconds(12312312313123))
//
////        listOf(borrower,leadArranger,lenderA,lenderB).forEach{it.registerInitiatedFlow(CreateTermSheetFlow::class.java)}
//        network.startNodes()
//    }
//
//    @After
//    fun tearDown() = network.stopNodes()
//
//    private fun StartedMockNode.runFlow(flow: FlowLogic<SignedTransaction>): SignedTransaction {
//        val future = startFlow(flow)
//        return future.getOrThrow()
////        network.runNetwork()
//    }
//
//
//    @Test
//    fun createStaticDataTest(){
//
//        val createStaticDataFLow = CreateStaticDataFlow(staticDataState)
//        print(leadArranger.runFlow(createStaticDataFLow))
//    }
//
//    @Test
//    fun updateStaticDataTest(){
//        createStaticDataTest()
//        staticDataState=staticDataState.copy(lenderAAccountNumber = "lac11")
//        val updateStaticDataFLow = UpdateStaticDataFlow(staticDataState)
//        print(leadArranger.runFlow(updateStaticDataFLow))
//    }
//
////    @Test
////    fun issuanceStaticDataTest(){
////        createStaticDataTest()
////        staticDataState=staticDataState.copy(lenderAContribution = 2000.0)
////        val issuanceStaticDataFlow = IssuanceStaticDataFlow(staticDataState)
////        print(leadArranger.runFlow(issuanceStaticDataFlow))
////    }
//}
