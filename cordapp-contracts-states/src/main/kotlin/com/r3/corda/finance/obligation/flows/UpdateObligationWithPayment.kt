package com.r3.corda.finance.obligation.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.finance.obligation.commands.ObligationCommands
import com.r3.corda.finance.obligation.contracts.ObligationContract
import com.r3.corda.finance.obligation.getLinearStateById
import com.r3.corda.finance.obligation.resolver
import com.r3.corda.finance.obligation.states.Obligation
import com.r3.corda.finance.obligation.types.Money
import com.r3.corda.finance.obligation.types.Payment
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@StartableByRPC
class UpdateObligationWithPayment<T : Money>(
        val linearId: UniqueIdentifier,
        val paymentInformation: Payment<T>,
        override val progressTracker: ProgressTracker = UpdateObligationWithPayment.tracker()
) : FlowLogic<SignedTransaction>() {

    companion object {
        object INITIALISING : ProgressTracker.Step("Performing initial steps.")
        object ADDING : ProgressTracker.Step("Adding payment information. @@@@ In UpdateObligationWithPayment.kt @@@@")
        object BUILDING : ProgressTracker.Step("Building transaction.")
        object SIGNING : ProgressTracker.Step("signing transaction.")
        object FINALISING : ProgressTracker.Step("Finalising transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(INITIALISING, ADDING, BUILDING, SIGNING, FINALISING)
    }

    @Suspendable
    override fun call(): SignedTransaction {
        println("!! TESTING in UPDATEOBLIGATION.call() 2.0")

        progressTracker.currentStep = INITIALISING
        println("!! TESTING in UPDATEOBLIGATION.call() 2.1")

        val obligationStateAndRef = getLinearStateById<Obligation<T>>(linearId, serviceHub)
                ?: throw IllegalArgumentException("LinearId not recognised.")
        val obligation = obligationStateAndRef.state.data
        println("!! TESTING in UPDATEOBLIGATION.call() 2.3")

        // 2. This flow should only be started by the obligor!!!!!!!.
        val obligor = obligation.withWellKnownIdentities(resolver).obligor
        check(ourIdentity == obligor) { "This flow can only be started by the obligor. " }
        println("!! TESTING in UPDATEOBLIGATION.call() 2.4")

        // 3. Add payment to obligation.
        progressTracker.currentStep = ADDING
        println("!! TESTING in UPDATEOBLIGATION.call() 2.5")

        val obligationWithNewPayment = obligation.withPayment(paymentInformation)
        println("!! TESTING in UPDATEOBLIGATION.call() 2.6")

        // 4. Creating a sign new transaction.
        progressTracker.currentStep = BUILDING
        println("!! TESTING in UPDATEOBLIGATION.call() 2.7")

        val signingKey = listOf(obligation.obligor.owningKey)
        println("!! TESTING in UPDATEOBLIGATION.call() 2.8")

        val notary = serviceHub.networkMapCache.notaryIdentities.firstOrNull()
                ?: throw FlowException("No available notary.")
        println("!! TESTING in UPDATEOBLIGATION.call() 2.9")

        val utx = TransactionBuilder(notary = notary).apply {
            addInputState(obligationStateAndRef)
            addOutputState(obligationWithNewPayment, ObligationContract.CONTRACT_REF)
            addCommand(ObligationCommands.AddPayment(paymentInformation.paymentReference), signingKey)
        }
        println("!! TESTING in UPDATEOBLIGATION.call() 2.10")

        // 5. Sign transaction.
        progressTracker.currentStep = SIGNING
        println("!! TESTING in UPDATEOBLIGATION.call() 2.11")

        val stx = serviceHub.signInitialTransaction(utx, signingKey)
        println("!! TESTING in UPDATEOBLIGATION.call() 2.12")

        // 6. Finalise transaction and send to participants.
        progressTracker.currentStep = FINALISING
        println("!! TESTING in UPDATEOBLIGATION.call() 2.13")

        return subFlow(FinalityFlow(stx, FINALISING.childProgressTracker()))
    }
}