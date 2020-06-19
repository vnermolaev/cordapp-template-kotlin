package com.vne

import com.vne.flows.IssueFlow
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test

class FlowTests {
    private val network = MockNetwork(
        MockNetworkParameters(cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("com.vne.contracts"),
            TestCordapp.findCordapp("com.vne.flows")
        )))

    private val airline = network.createNode()
    private val airlineParty = airline.info.singleIdentity()

    private val alice = network.createNode()
    private val aliceParty = alice.info.singleIdentity()

    private val bob = network.createNode()
    private val bobParty = bob.info.singleIdentity()

    init {
        listOf(airline, alice, bob).forEach {
            it.registerInitiatedFlow(IssueFlow.Responder::class.java)
        }
    }


    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()


    @Test
    fun `Valid transaction receives all signatures`() {
        val acquirer = IssueFlow.Acquirer(aliceParty, 25L)
        val flow = IssueFlow.Initiator(listOf(acquirer))
        val fut = airline.startFlow(flow)
        network.runNetwork()

        val signedTx = fut.getOrThrow()
        signedTx.verifyRequiredSignatures()
    }
}