package com.vne.flows

import co.paralleluniverse.fibers.Suspendable
import com.vne.contracts.TokenContract
import com.vne.states.TokenState
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

object IssueFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(acquirers: List<Acquirer>) : FlowLogic<SignedTransaction>() {
        private val acquirers: List<Acquirer>

        // This breaks everything.
        // private val notary = serviceHub.networkMapCache.notaryIdentities.first()

        init {
            require(acquirers.isNotEmpty()) { "List of acquirers must not be empty "}
            // Flow will fail at state creation if any quantity is less or equal to 0.

            this.acquirers = acquirers
        }

        companion object {
            object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction.")
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Issuer signs transaction.")
            object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                FINALISING_TRANSACTION
            )
        }

        override val progressTracker = tracker()

        @Suspendable
        override fun call(): SignedTransaction {
            val issuer = ourIdentity

            // Generate.
            progressTracker.currentStep = GENERATING_TRANSACTION

            val notary = serviceHub.networkMapCache.notaryIdentities.first()
            val txBuilder = TransactionBuilder(notary)
                .addCommand(Command(TokenContract.Commands.Issue(), issuer.owningKey))
                .addOutputStates(acquirers.map { TokenState(issuer, it.party, it.quantity) })

            // Verify.
            progressTracker.currentStep = VERIFYING_TRANSACTION
            txBuilder.verify(serviceHub)

            // Sign.
            progressTracker.currentStep = SIGNING_TRANSACTION
            // Only issuer needs to sign. Holder will only accept.
            val fullySignedTx = serviceHub.signInitialTransaction(txBuilder)

            // Finalize.
            progressTracker.currentStep = FINALISING_TRANSACTION
            val counterPartiesFlows = acquirers.map { it.party }
                .distinct()
                .map { initiateFlow(it) }


            return subFlow(FinalityFlow(
                fullySignedTx,

                // This breaks everything
                //counterParties.map { initiateFlow(it) },

                counterPartiesFlows,
                FINALISING_TRANSACTION.childProgressTracker()
            ))
        }
    }

    @InitiatedBy(Initiator::class)
    class Responder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            return subFlow(ReceiveFinalityFlow(counterpartySession))
        }
    }
}
