package com.whistleblower.flows

import com.whistleblower.BlowWhistleFlow
import com.whistleblower.BlowWhistleFlowResponder
import com.whistleblower.BlowWhistleState
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.internal.declaredField
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.node.services.api.ServiceHubInternal
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.startFlow
import org.junit.After
import org.junit.Before
import java.security.PublicKey

abstract class FlowTestsBase {
    private lateinit var network: MockNetwork
    protected lateinit var whistleBlower: StartedMockNode
    protected lateinit var firstInvestigator: StartedMockNode
    protected lateinit var badCompany: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(listOf("com.whistleblower"))
        whistleBlower = network.createPartyNode()
        firstInvestigator = network.createPartyNode()
        badCompany = network.createPartyNode()
        listOf(whistleBlower, firstInvestigator, badCompany).forEach {
            it.registerInitiatedFlow(BlowWhistleFlowResponder::class.java)
        }

        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    protected fun blowWhistle(): SignedTransaction {
        val flow = BlowWhistleFlow(badCompany.info.legalIdentities.first(), firstInvestigator.info.legalIdentities.first())
        val future = whistleBlower.services.startFlow(flow)
        network.runNetwork()
        return future.getOrThrow()
    }

    protected fun StartedMockNode.partyFromAnonymous(anonParty: AnonymousParty): Party? {
        val services = this.declaredField<ServiceHubInternal>("services").value
        return services.identityService.wellKnownPartyFromAnonymous(anonParty)
    }

    val StartedMockNode.legalIdentityKeys: List<PublicKey>
        get() = info.legalIdentities.map { it.owningKey }
}