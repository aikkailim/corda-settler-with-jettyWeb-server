package com.r3.corda.finance.obligation.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.finance.obligation.getLinearStateById
import com.r3.corda.finance.obligation.states.Obligation
import com.r3.corda.finance.obligation.types.OffLedgerPayment
import com.r3.corda.finance.obligation.types.SettlementOracleResult
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.SendStateAndRefFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap

@StartableByRPC
class SendToSettlementOracle(
        val linearId: UniqueIdentifier,
        override val progressTracker: ProgressTracker = SendToSettlementOracle.tracker()
) : AbstractSendToSettlementOracle() {

    companion object {
        object INITIALISING : ProgressTracker.Step("Performing initial steps.")
        object SENDING : ProgressTracker.Step("Sending obligation to settlement oracle.")
        object WAITING : ProgressTracker.Step("Waiting for response from settlement oracle.")
        object FINALISING : ProgressTracker.Step("Finalising transaction.") {
            override fun childProgressTracker(): ProgressTracker = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(INITIALISING, SENDING, WAITING)
    }

    @Suspendable
    override fun call(): SignedTransaction {
        println("!! TESTING in SENDTOSETTLEMENTORACLE.call() 3.0")

        progressTracker.currentStep = INITIALISING
        println("!! TESTING in SENDTOSETTLEMENTORACLE.call() 3.1")

        // Resolve the linearId to an obligation.
        val obligationStateAndRef = getLinearStateById<Obligation<*>>(linearId, serviceHub)
                ?: throw IllegalArgumentException("LinearId not recognised.")

        // Get the Oracle from the settlement instructions.
        val obligationState = obligationStateAndRef.state.data
        val settlementMethod = obligationState.settlementMethod as OffLedgerPayment<*>
        println("!! TESTING in SENDTOSETTLEMENTORACLE.call() 3.2")

        // Send the Oracle the ObligationContract state.
        progressTracker.currentStep = SENDING
        println("!! TESTING in SENDTOSETTLEMENTORACLE.call() 3.3")

        val session = initiateFlow(settlementMethod.settlementOracle)
        println("!! TESTING in SENDTOSETTLEMENTORACLE.call() 3.4")

        subFlow(SendStateAndRefFlow(session, listOf(obligationStateAndRef)))
        println("!! TESTING in SENDTOSETTLEMENTORACLE.call() 3.5")

        // Receive a SignedTransaction from the oracle that exits the obligation, or throw an exception if we timed out.
        progressTracker.currentStep = WAITING
        println("!! TESTING in SENDTOSETTLEMENTORACLE.call() 3.6")

        return session.receive<SettlementOracleResult>().unwrap {
            when (it) {
                is SettlementOracleResult.Success -> {
                    println("!! TESTING in SENDTOSETTLEMENTORACLE.call() 3.7")

                    val stx = it.stx
                    subFlow(FinalityFlow(stx, FINALISING.childProgressTracker()))
                }
                is SettlementOracleResult.Failure -> {
                    println("!! TESTING in SENDTOSETTLEMENTORACLE.call() 3.8")

                    throw IllegalStateException(it.message)
                }
            }
        }
    }

}