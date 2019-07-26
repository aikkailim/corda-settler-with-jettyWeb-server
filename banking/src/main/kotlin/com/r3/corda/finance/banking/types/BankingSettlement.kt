package com.r3.corda.finance.banking.types

//import com.r3.corda.finance.banking.utilities.BankingSettlementAccountId
import com.r3.corda.finance.banking.flows.MakeBankingPayment
import com.r3.corda.finance.obligation.types.OffLedgerPayment
import net.corda.core.identity.Party

data class BankingSettlement(
        override val accountToPay: String,
        override val settlementOracle: Party,
        override val paymentFlow: Class<MakeBankingPayment<*>> = MakeBankingPayment::class.java
) : OffLedgerPayment<MakeBankingPayment<*>> {
    override fun toString(): String {
        return "Pay to $accountToPay and use $settlementOracle as settlement Oracle."
    }
}