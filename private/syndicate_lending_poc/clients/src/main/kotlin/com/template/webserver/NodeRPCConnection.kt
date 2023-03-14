package com.template.webserver

import net.corda.client.rpc.CordaRPCClient
import net.corda.client.rpc.CordaRPCConnection
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.NetworkHostAndPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

private const val CORDA_USER_NAME = "config.rpc.username"
private const val CORDA_USER_PASSWORD = "config.rpc.password"
private const val CORDA_NODE_HOST = "config.rpc.host"
private const val CORDA_RPC_PORT = "config.rpc.port"

/**
 * Wraps an RPC connection to a Corda node.
 *
 * The RPC connection is configured using command line arguments.
 *
 * @param host The host of the node we are connecting to.
 * @param rpcPort The RPC port of the node we are connecting to.
 * @param username The username for logging into the RPC client.
 * @param password The password for logging into the RPC client.
 * @property proxy The RPC proxy.
 */
@Component
open class NodeRPCConnection : AutoCloseable {

    lateinit var proxy: CordaRPCOps
        private set

    var myRPCMap: HashMap<String, CordaRPCConnection> = hashMapOf()

    var orgList: HashMap<String, List<Any?>> = hashMapOf("Borrower" to listOf("localhost", "user1", "test", 10006),
            "LeadArranger" to listOf("localhost", "user1", "test", 10009),
            "LenderA" to listOf("localhost", "user1", "test", 10012),
            "LenderB" to listOf("localhost", "user1", "test", 10015))

    @PostConstruct
    fun initialiseNodeRPCConnection() {
        for ((party, creds) in orgList) {
            var networkConfig = NetworkHostAndPort(creds[0] as String, creds[3] as Int)
            var rpcClient = CordaRPCClient(networkConfig)
            var rpcConn = rpcClient.start(creds[1] as String, creds[2] as String)
            myRPCMap.put(party, rpcConn)
        }
    }

    @PreDestroy
    override fun close() {
        myRPCMap.forEach { (party, rpcConn) -> rpcConn.notifyServerAndClose() }
    }
}