package com.vne.flows

import co.paralleluniverse.fibers.Suspendable
import com.vne.contracts.TokenContract
import com.vne.states.TokenState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

object MoveFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(consumables: List<StateAndRef<TokenState>>, acquirers: List<Acquirer>) : FlowLogic<SignedTransaction>() {
        private val consumables: List<StateAndRef<TokenState>>
        private val acquirers: List<Acquirer>

        init {
            require(consumables.isNotEmpty()) { "List of states to consume must not be empty "}
            require(acquirers.isNotEmpty()) { "List of acquirers must not be empty "}
            // Flow will fail at state creation if any quantity is less or equal to 0.
            // Flow will fail at contract verification if there are more than 1 issuer across all states.

            this.consumables = consumables
            this.acquirers = acquirers
        }

        companion object {
            object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction.")
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Issuer signs transaction.")
            object GATHERING_SIGS : ProgressTracker.Step("Gathering the counterparty's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }
            object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGS,
                FINALISING_TRANSACTION
            )
        }

        override val progressTracker = tracker()

        @Suspendable
        override fun call(): SignedTransaction {
            val holder = ourIdentity

            require(consumables.all { it.state.data.holder == holder }) { "Only owned states can be consumed" }

            // Generate.
            progressTracker.currentStep = GENERATING_TRANSACTION

            // The same notary has to keep track of all states in the chain.
            val notary = consumables.map { it.state.notary }.distinct().single()

            val txBuilder = TransactionBuilder(notary)
                .addCommand(
                    Command(
                        TokenContract.Commands.Move(),
                        acquirers.map { it.party.owningKey } + holder.owningKey
                    ))
                .addInputStates(consumables)
                .addOutputStates(acquirers.map { TokenState(consumables[0].state.data.issuer, it.party, it.quantity) })

            // Verify.
            progressTracker.currentStep = VERIFYING_TRANSACTION
            txBuilder.verify(serviceHub)

            // Sign.
            progressTracker.currentStep = SIGNING_TRANSACTION
            // Only issuer needs to sign. Holder will only accept.
            val partlySignedTx = serviceHub.signInitialTransaction(txBuilder)

            // Gather counter-party sig-s.
            progressTracker.currentStep = GATHERING_SIGS
            val counterPartiesFlows = acquirers.map { it.party }
                .distinct()
                .minus(holder)
                .map { initiateFlow(it) }

            val fullySignedTx = if (counterPartiesFlows.isEmpty()) {
                // This may happen when balance is split into pieces.
                partlySignedTx
            } else {
                subFlow(CollectSignaturesFlow(
                    partlySignedTx,
                    counterPartiesFlows,
                    GATHERING_SIGS.childProgressTracker()))
            }


            // Finalize.
            progressTracker.currentStep = FINALISING_TRANSACTION
            return subFlow(FinalityFlow(
                fullySignedTx,
                counterPartiesFlows,
                FINALISING_TRANSACTION.childProgressTracker()
            ))
        }
    }

    @InitiatedBy(Initiator::class)
    class Responder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val _a = "I must be relevant for this transaction" using
                        stx.toLedgerTransaction(serviceHub, false)
                            .outputsOfType<TokenState>()
                            .any { it.holder == ourIdentity }
                }
            }
            val txId = subFlow(signTransactionFlow).id

            return subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId = txId))
        }
    }
}
