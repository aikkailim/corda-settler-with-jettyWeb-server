package com.r3.corda.finance.obligation.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.finance.obligation.commands.ObligationCommands
import com.r3.corda.finance.obligation.contracts.ObligationContract
import com.r3.corda.finance.obligation.getLinearStateById
import com.r3.corda.finance.obligation.resolver
import com.r3.corda.finance.obligation.states.Obligation
import com.r3.corda.finance.obligation.types.Money
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class RetrieveObligation(
        val linearId: UniqueIdentifier) : FlowLogic<WireTransaction>() {

    companion object {
        object INITIALISING : ProgressTracker.Step("Performing initial step to retrieve obligation. ")
        object BUILDING : ProgressTracker.Step("Building and verifying original transaction. -There should be other way to do this..")
        object SIGNING : ProgressTracker.Step("Signing original transaction. -For testing")
        object FAILED : ProgressTracker.Step("In RetrieveObligation.kt - should not come here if running on PartA/B nodes")
        object FINALISING : ProgressTracker.Step("Finalising transaction. -Finalising testing") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(INITIALISING, BUILDING, SIGNING, FINALISING)
    }

    override val progressTracker: ProgressTracker = tracker()

    @Suspendable
    override fun call(): WireTransaction {
        // 1. Retrieve obligation
        progressTracker.currentStep = INITIALISING
        val obligationStateAndRef = getLinearStateById<Obligation<Money>>(linearId, serviceHub)
                ?: throw IllegalArgumentException("LinearId not recognised.")
        val obligation = obligationStateAndRef.state.data

        // 2. Build original obligation
        progressTracker.currentStep = RetrieveObligation.Companion.BUILDING
        val obligor = obligation.withWellKnownIdentities(resolver).obligor
        val obligee = obligation.withWellKnownIdentities(resolver).obligee

        if (ourIdentity == obligor) {
            val signingKey = listOf(obligation.obligor.owningKey)
            val notary = serviceHub.networkMapCache.notaryIdentities.firstOrNull()
                    ?: throw FlowException("No available notary.")
            val utx = TransactionBuilder(notary = notary).apply {
                addInputState(obligationStateAndRef)
                addOutputState(obligation, ObligationContract.CONTRACT_REF)
                addCommand(ObligationCommands.RetrieveObligation(), signingKey)
            }

            // 3. Sign transaction.
            progressTracker.currentStep = RetrieveObligation.Companion.SIGNING
            val stx = serviceHub.signInitialTransaction(utx, signingKey)

            // 4. Finalise transaction and send to participants.
            progressTracker.currentStep = RetrieveObligation.Companion.FINALISING
            return subFlow(FinalityFlow(stx, RetrieveObligation.Companion.FINALISING.childProgressTracker())).tx
        } else if (ourIdentity == obligee) {
            val signingKey = listOf(obligation.obligee.owningKey)
            val notary = serviceHub.networkMapCache.notaryIdentities.firstOrNull()
                    ?: throw FlowException("No available notary.")
            val utx = TransactionBuilder(notary = notary).apply {
                addInputState(obligationStateAndRef)
                addOutputState(obligation, ObligationContract.CONTRACT_REF)
                addCommand(ObligationCommands.RetrieveObligation(), signingKey)
            }
            // 3. Sign transaction.
            progressTracker.currentStep = RetrieveObligation.Companion.SIGNING
            val stx = serviceHub.signInitialTransaction(utx, signingKey)

            // 4. Finalise transaction and send to participants.
            progressTracker.currentStep = RetrieveObligation.Companion.FINALISING
            return subFlow(FinalityFlow(stx, RetrieveObligation.Companion.FINALISING.childProgressTracker())).tx
        }

        val notary = serviceHub.networkMapCache.notaryIdentities.firstOrNull()
                ?: throw FlowException("No available notary.")
        val signers = obligation.participants.map { it.owningKey }
        val utx = TransactionBuilder(notary = notary).apply {
            addInputState(obligationStateAndRef)
            addCommand(ObligationCommands.Cancel(), signers)
        }

        progressTracker.currentStep = RetrieveObligation.Companion.FAILED
        val stx = serviceHub.signInitialTransaction(utx, signers)
        return subFlow(FinalityFlow(stx, RetrieveObligation.Companion.FINALISING.childProgressTracker())).tx
    }
}