package com.vne.contracts

import com.vne.plus
import com.vne.states.TokenState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import java.lang.IllegalArgumentException

// ************
// * Contract *
// ************
class TokenContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.vne.contracts.TokenContract"
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()

        val ins = tx.inputsOfType<TokenState>()
        val outs = tx.outputsOfType<TokenState>()

        when (command.value) {
            is Commands.Issue ->
                requireThat {
                    "Issuance must not consume states" using ins.isEmpty()
                    "Issuance must create states" using outs.isNotEmpty()
                    "Issuers must sign" using command.signers.containsAll(outs.map { it.issuer.owningKey }.distinct())
                }

            is Commands.Move -> {
                val insTotal = ins.fold(0L) { total, state -> total + state.quantity }
                val outsTotal = outs.fold(0L) { total, state -> total + state.quantity }
                val sameIssuer = (ins.map { it.issuer } + outs.map { it.issuer }).distinct().size == 1

                requireThat {
                    "Moving must consume at least one state" using ins.isNotEmpty()
                    "Moving must produce at least one state" using outs.isNotEmpty()
                    "Moved value must be preserved" using (insTotal == outsTotal)
                    "Moving must not change issuer" using sameIssuer
                    "Current holder must sign" using command.signers.containsAll(ins.map { it.holder.owningKey }.distinct())
                }
            }
            is Commands.Redeem ->
                requireThat {
                    "Redeeming must consume states" using ins.isNotEmpty()
                    "Redeeming must not produce new states" using outs.isEmpty()
                    "Issuers must sign" using command.signers.containsAll(ins.map { it.issuer.owningKey }.distinct())
                    "Holders must sign" using command.signers.containsAll(ins.map { it.holder.owningKey }.distinct())
                }

            else -> throw IllegalArgumentException("Unknown command ${command.value}")
        }

    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Issue : Commands
        class Move: Commands
        class Redeem: Commands
    }
}