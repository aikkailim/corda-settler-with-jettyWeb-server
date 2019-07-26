package com.r3.corda.finance.banking.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.finance.banking.services.BankingService
import com.r3.corda.finance.banking.types.BankingPayment
import com.r3.corda.finance.banking.types.BankingPaymentResponse
import com.r3.corda.finance.banking.types.BankingSettlement
import com.r3.corda.finance.obligation.flows.MakeOffLedgerPayment
import com.r3.corda.finance.obligation.states.Obligation
import com.r3.corda.finance.obligation.types.*
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowException
import net.corda.core.utilities.ProgressTracker
import java.time.Duration
import java.util.*

class MakeBankingPayment<T : Money>(
        amount: Amount<T>,
        obligationStateAndRef: StateAndRef<Obligation<*>>,
        settlementMethod: OffLedgerPayment<*>,
        progressTracker: ProgressTracker
) : MakeOffLedgerPayment<T>(amount, obligationStateAndRef, settlementMethod, progressTracker) {
    companion object {
        object TESTING : ProgressTracker.Step("TESTING IF MakeBankingPayment FLOW HAS BEEN STARTED -- !!")

        fun tracker() = ProgressTracker(TESTING)
    }

    private fun createAndSignAndSubmitPayment(obligation: Obligation<*>, amount: Amount<T>): BankingPaymentResponse {
        println("!! TESTING in MAKEBANKINGPAYMENT.makePayment 1.1")

        if (amount.token !is FiatCurrency)
            throw FlowException("Please only pay in FiatCurrency for now.")
        if (obligation.settlementMethod == null)
            throw FlowException("Please update the SettlementMethod to BankingSettlement for now.")
        if (obligation.settlementMethod !is BankingSettlement)
            throw FlowException("Please use only BankingSettlement for now.")
        println("!! TESTING in MAKEBANKINGPAYMENT.makePayment 1.2")

        val bankingService = serviceHub.cordaService(BankingService::class.java)
        println("!! TESTING in MAKEBANKINGPAYMENT.makePayment 1.3")

        val bankingSettlement = obligation.settlementMethod as BankingSettlement
        println("!! TESTING in MAKEBANKINGPAYMENT.makePayment 1.4")

        // TODO: for now we are taking obligations's linearId as an e2e payment id. This behaviour needs to be changed,
        // we need to let API consumers to provide their own e2e ids as strings, which would also give us idempotence out-of-the-box
        return bankingService.makePayment(
                obligation.linearId.toString(),
                Date.from(obligation.dueBy),
                amount as Amount<FiatCurrency>,
                obligation.obligor.toString(),
                obligation.obligee.toString(),
                bankingSettlement.accountToPay
        )
    }

    @Suspendable
    override fun setup() {
    }

    override fun checkBalance(requiredAmount: Amount<*>) {
    }

    @Suspendable
    override fun makePayment(obligation: Obligation<*>, amount: Amount<T>): Payment<T> {
        println("!! TESTING in MAKEBANKINGPAYMENT.makePayment 0")

//        progressTracker.currentStep = TESTING
        println("!! TESTING in MAKEBANKINGPAYMENT.makePayment 1")

        val paymentResponse = createAndSignAndSubmitPayment(obligation, amount)
        println("!! TESTING in MAKEBANKINGPAYMENT.makePayment 2")

        val paymentReference = paymentResponse.uetr
        println("!! TESTING in MAKEBANKINGPAYMENT.makePayment 3")
        println(paymentReference)

        sleep(Duration.ofMillis(1))

        return BankingPayment(paymentReference, amount as Amount<FiatCurrency>, PaymentStatus.SENT) as Payment<T>
    }
}
