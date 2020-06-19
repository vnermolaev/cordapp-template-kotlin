package com.vne

import com.vne.flows.Acquirer
import com.vne.flows.IssueFlow
import com.vne.flows.MoveFlow
import com.vne.states.TokenState
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test

class MoveFlowTests {
    private val network = MockNetwork(
        MockNetworkParameters(cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("com.vne.contracts"),
            TestCordapp.findCordapp("com.vne.flows")
        )))

    private val airline = network.createNode()

    private val alice = network.createNode()
    private val aliceParty = alice.info.singleIdentity()

    private val bob = network.createNode()
    private val bobParty = bob.info.singleIdentity()

    init {
        listOf(airline, alice, bob).forEach {
            it.registerInitiatedFlow(IssueFlow.Responder::class.java)
            it.registerInitiatedFlow(MoveFlow.Initiator::class.java, MoveFlow.Responder::class.java)
        }
    }


    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()


    @Test
    fun `Valid transaction receives all signatures`() {
        // Issue.
        val acquirer = Acquirer(aliceParty, 25L)
        val issueFlow = IssueFlow.Initiator(listOf(acquirer))
        val issueFut = airline.startFlow(issueFlow)
        network.runNetwork()
        val issuanceTx = issueFut.getOrThrow()

        val issued = issuanceTx
            .toLedgerTransaction(airline.services)
            .outRefsOfType<TokenState>()

        // Move.
        val acquirers = listOf(Acquirer(aliceParty, 15L), Acquirer(bobParty, 10L))
        val moveFlow = MoveFlow.Initiator(issued, acquirers)
        val moveFut = alice.startFlow(moveFlow)
        network.runNetwork()
        val moveTx = moveFut.getOrThrow()

        moveTx.verifyRequiredSignatures()
    }
}