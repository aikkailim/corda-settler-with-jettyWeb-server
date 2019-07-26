package com.r3.corda.finance.obligation

import com.r3.corda.finance.obligation.flows.CreateObligation
import com.r3.corda.finance.obligation.states.Obligation
import net.corda.core.contracts.Amount
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.getOrThrow
import java.util.*
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("obligation")
class ObligationApi(val rpcOps: CordaRPCOps) {

    private val myIdentity = rpcOps.nodeInfo().legalIdentities.first()
    private val notaryIdentity = rpcOps.notaryIdentities().first()

    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    fun me() = mapOf("me" to myIdentity)

    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    fun peers() = mapOf("peers" to rpcOps.networkMapSnapshot()
            .filter { nodeInfo ->
                nodeInfo.legalIdentities.first() != myIdentity
                        && nodeInfo.legalIdentities.first() != notaryIdentity
                        && nodeInfo.legalIdentities.first().name.organisation != "Oracle"
            }
            .map { it.legalIdentities.first().name.organisation })

    @GET
    @Path("all-nodes")
    @Produces(MediaType.APPLICATION_JSON)
    fun allNodes() = mapOf("allNodes" to rpcOps.networkMapSnapshot()
            .filter { nodeInfo ->
                nodeInfo.legalIdentities.first() != myIdentity
            }
            .map { it.legalIdentities.first().name.organisation })

    @GET
    @Path("issue-obligation")
    fun issueObligation(@QueryParam(value = "role") role: String,
                        @QueryParam(value = "party") party: String,
                        @QueryParam(value = "currency") currency: String,
                        @QueryParam(value = "amount") amount: Int,
                        @QueryParam(value = "duedate") duedate: Int
    ): Response {
        println("!! TESTING - ObligationApi.kt 1.0")
        println(myIdentity)

        // 1. Get party objects for the counterparty.
        val obligorIdentity = rpcOps.partiesFromName(party, exactMatch = false).singleOrNull()
                ?: throw IllegalStateException("Couldn't lookup node identity for $party.")
        // 2. Create an amount object.
        val issueAmount = Amount(amount.toLong() * 100, Currency.getInstance(currency))

        // 3. Get role class
        val roleClass: CreateObligation.InitiatorRole
        if (role == "OBLIGEE") {
            roleClass = CreateObligation.InitiatorRole.OBLIGEE
        } else {
            roleClass = CreateObligation.InitiatorRole.OBLIGOR
        }

        // 4. Start the IssueObligation flow. We block and wait for the flow to return.
        val (status, message) = try {
            println("!! TESTING - ObligationApi.kt 1.1")

            val flowHandle = rpcOps.startFlowDynamic(
                    CreateObligation.Initiator::class.java,
                    issueAmount,
                    roleClass,
                    obligorIdentity,
                    duedate,
                    true
            )
            println("!! TESTING - ObligationApi.kt 1.2")

            val result = flowHandle.returnValue.getOrThrow()
            flowHandle.close()
            Response.Status.CREATED to "Transaction id ${result.id} committed to ledger.\n${result.outputs}"
        } catch (e: Exception) {
            Response.Status.BAD_REQUEST to e.message
        }

        // 4. Return the result.
        return Response.status(status).entity(message).build()
    }

    @GET
    @Path("obligations")
    @Produces(MediaType.APPLICATION_JSON)
    fun obligations(): List<Obligation<*>> {
        println("!! TESTING - ObligationApi.kt 2.0")

        val statesAndRefs = rpcOps.vaultQuery(Obligation::class.java).states

        return statesAndRefs
                .map { stateAndRef -> stateAndRef.state.data }
                .map { state ->
                    // We map the anonymous lender and borrower to well-known identities if possible.
                    val possiblyWellKnownLender = rpcOps.wellKnownPartyFromAnonymous(state.obligee) ?: state.obligee
                    val possiblyWellKnownBorrower = rpcOps.wellKnownPartyFromAnonymous(state.obligor) ?: state.obligor
                    println("!! TESTING - ObligationApi.kt 2.1")

                    Obligation(state.faceAmount,
                            possiblyWellKnownBorrower,
                            possiblyWellKnownLender,
                            state.dueBy,
                            state.createdAt,
                            state.settlementMethod,
                            state.payments,
                            state.linearId)
                }
    }
}