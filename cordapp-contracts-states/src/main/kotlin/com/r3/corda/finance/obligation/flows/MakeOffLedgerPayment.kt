package com.r3.corda.finance.obligation.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.finance.obligation.states.Obligation
import com.r3.corda.finance.obligation.types.Money
import com.r3.corda.finance.obligation.types.OffLedgerPayment
import com.r3.corda.finance.obligation.types.Payment
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

abstract class MakeOffLedgerPayment<T : Money>(
        val amount: Amount<T>,
        private val obligationStateAndRef: StateAndRef<Obligation<*>>,
        open val settlementMethod: OffLedgerPayment<*>,
        override val progressTracker: ProgressTracker = MakeOffLedgerPayment.tracker()
) : AbstractMakeOffLedgerPayment() {

    companion object {
        object SETUP : ProgressTracker.Step("Setting up payment method. -This is in MakeOffLedgerPayment.ky, in cordapp")
        object CHECKING : ProgressTracker.Step("Checking balance. -This is in MakeOffLedgerPayment.ky, in cordapp")
        object PAYING : ProgressTracker.Step("Making payment. -This is in MakeOffLedgerPayment.ky, in cordapp")
        object UPDATING : ProgressTracker.Step("Updating obligation with payment details. -This is in MakeOffLedgerPayment.ky, in cordapp") {
            override fun childProgressTracker() = UpdateObligationWithPayment.tracker()
        }

        fun tracker() = ProgressTracker(SETUP, CHECKING, PAYING, UPDATING)
    }

    @Suspendable
    abstract fun checkBalance(requiredAmount: Amount<*>)

    @Suspendable
    abstract fun makePayment(obligation: Obligation<*>, amount: Amount<T>): Payment<T>


    @Suspendable
    abstract fun setup()

    @Suspendable
    override fun call(): SignedTransaction {
        // 1. This flow should only be started by the beneficiary.
        val obligation = obligationStateAndRef.state.data
        val identityResolver = { abstractParty: AbstractParty ->
            serviceHub.identityService.requireWellKnownPartyFromAnonymous(abstractParty)
        }
        val obligor = obligation.withWellKnownIdentities(identityResolver).obligor
        check(ourIdentity == obligor) { "This flow can only be started by the obligor." }

        // 1. Do any setup stuff.
        progressTracker.currentStep = SETUP
        println("!! TESTING in progressTracker.currentStep = SETUP in MAKEOFFLEDGERPAYMENT")
        setup()

        // 2. Check balance.
        progressTracker.currentStep = CHECKING
        println("!! TESTING in progressTracker.currentStep = CHECKING in MAKEOFFLEDGERPAYMENT")
        checkBalance(amount)

        // 4. Make payment and manually checkpoint
        progressTracker.currentStep = PAYING
        println("!! TESTING in progressTracker.currentStep = PAYING in MAKEOFFLEDGERPAYMENT")
        val paymentInformation = makePayment(obligation, amount)
        println("!! TESTING in after progressTracker.currentStep = PAYING in MAKEOFFLEDGERPAYMENT")

        // 5. Add payment reference to settlement instructions and update state.
        progressTracker.currentStep = UPDATING
        return subFlow(UpdateObligationWithPayment(
                linearId = obligation.linearId,
                paymentInformation = paymentInformation,
                progressTracker = UPDATING.childProgressTracker()
        ))
    }
}