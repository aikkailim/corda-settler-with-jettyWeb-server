package com.r3.corda.finance.obligation

import com.r3.corda.finance.obligation.types.FiatCurrency
import net.corda.core.contracts.*
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.AbstractParty
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.LedgerTransaction
import net.corda.finance.AMOUNT
import java.util.*

val GBP = FiatCurrency(Currency.getInstance("GBP"))
fun POUNDS(amount: Int): Amount<FiatCurrency> = AMOUNT(amount, GBP)
fun POUNDS(amount: Long): Amount<FiatCurrency> = AMOUNT(amount, GBP)
fun POUNDS(amount: Double): Amount<FiatCurrency> = AMOUNT(amount, GBP)
val Int.GBP: Amount<FiatCurrency> get() = POUNDS(this)
val Long.GBP: Amount<FiatCurrency> get() = POUNDS(this)
val Double.GBP: Amount<FiatCurrency> get() = POUNDS(this)

val USD = FiatCurrency(Currency.getInstance("USD"))
fun DOLLARS(amount: Int): Amount<FiatCurrency> = AMOUNT(amount, USD)
fun DOLLARS(amount: Long): Amount<FiatCurrency> = AMOUNT(amount, USD)
fun DOLLARS(amount: Double): Amount<FiatCurrency> = AMOUNT(amount, USD)
val Int.USD: Amount<FiatCurrency> get() = DOLLARS(this)
val Long.USD: Amount<FiatCurrency> get() = DOLLARS(this)
val Double.USD: Amount<FiatCurrency> get() = DOLLARS(this)

val EUR = FiatCurrency(Currency.getInstance("EUR"))
fun EUROS(amount: Int): Amount<FiatCurrency> = AMOUNT(amount, EUR)
fun EUROS(amount: Long): Amount<FiatCurrency> = AMOUNT(amount, EUR)
fun EUROS(amount: Double): Amount<FiatCurrency> = AMOUNT(amount, EUR)
val Int.EUR: Amount<FiatCurrency> get() = EUROS(this)
val Long.EUR: Amount<FiatCurrency> get() = EUROS(this)
val Double.EUR: Amount<FiatCurrency> get() = EUROS(this)

/** Get single input/output from ledger transaction. */
inline fun <reified T : ContractState> LedgerTransaction.singleInput() = inputsOfType<T>().single()

inline fun <reified T : ContractState> LedgerTransaction.singleOutput() = outputsOfType<T>().single()

/** Gets a linear state by unique identifier. */
inline fun <reified T : LinearState> getLinearStateById(linearId: UniqueIdentifier, services: ServiceHub): StateAndRef<T>? {
    val query = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
    return services.vaultService.queryBy<T>(query).states.singleOrNull()
}

/** Lambda for resolving an [AbstractParty] to a [Party]. */
val FlowLogic<*>.resolver
    get() = { abstractParty: AbstractParty ->
        serviceHub.identityService.requireWellKnownPartyFromAnonymous(abstractParty)
    }