package com.vne.flows

import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder

data class Acquirer(val party: Party, val quantity: Long)

fun TransactionBuilder.addInputStates(inputs: List<StateAndRef<*>>): TransactionBuilder {
    inputs.map { this.addInputState(it) }
    return this
}

fun TransactionBuilder.addOutputStates(outputs: List<ContractState>): TransactionBuilder {
    outputs.map { this.addOutputState(it) }
    return this
}
