package com.vne.states

import com.vne.contracts.TokenContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.Party

// *********
// * State *
// *********
@BelongsToContract(TokenContract::class)
class TokenState(private val issuer: Party, private val holder: Party, quantity: Long) : ContractState {
    val quantity: Long

    // Prevent mutation of the participants list.
    override val participants get() = listOf(holder)

    init {
        if (quantity < 0) {
            throw Error("Only non negative quantities are allowed")
        }

        this.quantity = quantity
    }
}
