package com.vne

import com.vne.states.TokenState
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import org.junit.Test

class StateTests {
    private val airline = TestIdentity(CordaX500Name("EZFly", "London", "GB"))
    private val alice = TestIdentity(CordaX500Name("Alice", "New York", "US"))

    @Test(expected = Error::class)
    fun `state does not accept negative quantities`() {
        TokenState(airline.party, alice.party, -100)
    }
}