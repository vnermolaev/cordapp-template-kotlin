package com.vne

import com.vne.contracts.TokenContract

import com.vne.states.TokenState
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class ContractTests {
    private val ledgerServices = MockServices(listOf("com.vne.contracts"))
    private val airline = TestIdentity(CordaX500Name("EZFly", "London", "GB"))
    private val alice = TestIdentity(CordaX500Name("Alice", "New York", "US"))
    private val bob = TestIdentity(CordaX500Name("Bob", "New York", "US"))
    private val miles = 100L

    @Test
    fun `tx must contain 1 command`() {
        ledgerServices.ledger {
            transaction {
                input(TokenContract.ID, TokenState(airline.party, alice.party, miles))
                output(TokenContract.ID, TokenState(airline.party, bob.party, miles))
                fails()
                command(listOf(alice.publicKey, bob.publicKey), TokenContract.Commands.Move())
                verifies()
                command(listOf(alice.publicKey, bob.publicKey), TokenContract.Commands.Move())
                fails()
            }
        }
    }

    @Test
    fun `issue tx must not contain inputs`() {
        ledgerServices.ledger {
            transaction {
                command(listOf(airline.publicKey, alice.publicKey), TokenContract.Commands.Issue())
                input(TokenContract.ID, TokenState(airline.party, alice.party, miles))
                output(TokenContract.ID, TokenState(airline.party, alice.party, miles))
                fails()
            }

            transaction {
                command(listOf(airline.publicKey, alice.publicKey), TokenContract.Commands.Issue())
                output(TokenContract.ID, TokenState(airline.party, alice.party, miles))
                verifies()
            }
        }
    }

    @Test
    fun `move tx must contain at least 1 input`() {
        ledgerServices.ledger {
            transaction {
                command(listOf(airline.publicKey, alice.publicKey), TokenContract.Commands.Move())
                output(TokenContract.ID, TokenState(airline.party, bob.party, miles))
                fails()

                input(TokenContract.ID, TokenState(airline.party, alice.party, miles))
                verifies()
            }
        }
    }

    @Test
    fun `move tx must contain at least 1 output`() {
        ledgerServices.ledger {
            transaction {
                command(listOf(alice.publicKey, bob.publicKey), TokenContract.Commands.Move())
                input(TokenContract.ID, TokenState(airline.party, alice.party, miles))
                fails()

                output(TokenContract.ID, TokenState(airline.party, bob.party, miles))
                verifies()
            }
        }
    }

    @Test
    fun `move tx must preserve value`() {
        ledgerServices.ledger {
            transaction {
                command(listOf(alice.publicKey, bob.publicKey), TokenContract.Commands.Move())
                input(TokenContract.ID, TokenState(airline.party, alice.party, miles))
                output(TokenContract.ID, TokenState(airline.party, bob.party, miles + 1))
                fails()
            }

            transaction {
                command(listOf(alice.publicKey, bob.publicKey), TokenContract.Commands.Move())
                input(TokenContract.ID, TokenState(airline.party, alice.party, miles))
                output(TokenContract.ID, TokenState(airline.party, bob.party, miles))
                verifies()
            }
        }
    }

    @Test
    fun `move tx must not change issuer`() {
        val anotherAirline = TestIdentity(CordaX500Name("Local", "London", "GB"))

        ledgerServices.ledger {
            transaction {
                command(listOf(alice.publicKey, bob.publicKey), TokenContract.Commands.Move())
                input(TokenContract.ID, TokenState(airline.party, alice.party, miles))
                output(TokenContract.ID, TokenState(anotherAirline.party, alice.party, miles))
                fails()
            }
        }
    }

    @Test
    fun `redeem tx must not create new states`() {
        ledgerServices.ledger {
            transaction {
                command(listOf(airline.publicKey, alice.publicKey), TokenContract.Commands.Redeem())
                input(TokenContract.ID, TokenState(airline.party, alice.party, miles))
                output(TokenContract.ID, TokenState(airline.party, alice.party, miles))
                fails()
            }

            transaction {
                command(listOf(airline.publicKey, alice.publicKey), TokenContract.Commands.Redeem())
                input(TokenContract.ID, TokenState(airline.party, alice.party, miles))
                verifies()
            }
        }
    }
}