package com.r3.corda.finance.banking.types

import com.r3.corda.finance.obligation.types.FiatCurrency
import com.r3.corda.finance.obligation.types.Payment
import com.r3.corda.finance.obligation.types.PaymentReference
import com.r3.corda.finance.obligation.types.PaymentStatus
import net.corda.core.contracts.Amount

data class BankingPayment(
        override val paymentReference: PaymentReference,
        override val amount: Amount<FiatCurrency>,
        override var status: PaymentStatus = PaymentStatus.SENT
) : Payment<FiatCurrency> {
    override fun toString(): String {
        return "Amount: $amount, Banking Reference: $paymentReference, Status: $status"
    }
}