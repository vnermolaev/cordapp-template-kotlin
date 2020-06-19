package com.vne.states

import com.vne.contracts.TokenContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.Party

// *********
// * State *
// *********
@BelongsToContract(TokenContract::class)
class TokenState(val issuer: Party, val holder: Party, quantity: Long) : ContractState {
    val quantity: Long

    // Prevent mutation of the participants list.
    override val participants get() = listOf(holder)

    init {
        require(quantity > 0) { "Only positive quantities are allowed" }
        require(issuer != holder) { "Cannot issue to itself" }
        this.quantity = quantity
    }
}
