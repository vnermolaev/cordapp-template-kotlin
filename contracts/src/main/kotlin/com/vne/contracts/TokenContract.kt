package com.vne.contracts

import com.vne.states.TokenState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************
class TokenContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.template.contracts.TokenContract"
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        requireThat {
            "Only single command is allowed" using (tx.commands.size == 1)
            // All input and output states by design will contain a non-null issuer and holder.
            // All quantities are >=0 by design
        }

        val command = tx.commands.first().value
        if (command is Commands.Move) {
            val ins = tx.inputStates as List<TokenState>
            val insTotal = ins.fold(0L) {total, state -> total + state.quantity}

            val outs = tx.outputStates as List<TokenState>
            val outsTotal = outs.fold(0L) {total, state -> total + state.quantity}

            requireThat { "Sum must be preserved" using(insTotal  == outsTotal) }

        }

    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Issue : Commands
        class Move: Commands
        class Redeem: Commands
    }
}