package com.whistleblower.contract

import com.whistleblower.BLOW_WHISTLE_CONTRACT_ID
import com.whistleblower.BlowWhistleContract.Commands.HandOverInvestigationCmd
import com.whistleblower.BlowWhistleState
import net.corda.core.identity.CordaX500Name
import net.corda.testing.contracts.DummyState
import net.corda.testing.core.ALICE_NAME
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class HandOverInvestigationTests {
    private val ledgerServices = MockServices(listOf("com.whistleblower"))
    private val badCompany = TestIdentity(CordaX500Name("Bad Company", "Eldoret", "KE")).party
    private val whistleBlower = TestIdentity(CordaX500Name("Whistle Blower", "Nairobi", "KE")).party.anonymise()
    private val oldInvestigator = TestIdentity(CordaX500Name("Old Investigator", "Kisumu", "KE")).party.anonymise()
    private val newInvestigator = TestIdentity(CordaX500Name("New Investigator", "Mombasa", "KE")).party.anonymise()

    private val validInput = BlowWhistleState(badCompany, whistleBlower, oldInvestigator)
    private val validOutput = BlowWhistleState(badCompany, whistleBlower, newInvestigator, validInput.linearId)

    @Test
    fun `A HandOverInvestigation transaction should have a single BlowWhistleState input and a single BlowWhistleState output`() {
        // No input state.
        ledgerServices.ledger {
            transaction {
                output(BLOW_WHISTLE_CONTRACT_ID, validOutput)
                command(listOf(oldInvestigator.owningKey, newInvestigator.owningKey), HandOverInvestigationCmd())
                fails()
            }
        }
        // No output state.
        ledgerServices.ledger {
            transaction {
                input(BLOW_WHISTLE_CONTRACT_ID, validInput)
                command(listOf(oldInvestigator.owningKey, newInvestigator.owningKey), HandOverInvestigationCmd())
                fails()
            }
        }
        // Two input states.
        ledgerServices.ledger {
            transaction {
                input(BLOW_WHISTLE_CONTRACT_ID, validInput)
                input(BLOW_WHISTLE_CONTRACT_ID, validInput)
                output(BLOW_WHISTLE_CONTRACT_ID, validOutput)
                command(listOf(oldInvestigator.owningKey, newInvestigator.owningKey), HandOverInvestigationCmd())
                fails()
            }
        }
        // Two output states.
        ledgerServices.ledger {
            transaction {
                input(BLOW_WHISTLE_CONTRACT_ID, validInput)
                output(BLOW_WHISTLE_CONTRACT_ID, validOutput)
                output(BLOW_WHISTLE_CONTRACT_ID, validOutput)
                command(listOf(oldInvestigator.owningKey, newInvestigator.owningKey), HandOverInvestigationCmd())
                fails()
            }
        }
        // Wrong input state.
        ledgerServices.ledger {
            transaction {
                input(BLOW_WHISTLE_CONTRACT_ID, DummyState(0))
                output(BLOW_WHISTLE_CONTRACT_ID, validOutput)
                command(listOf(oldInvestigator.owningKey, newInvestigator.owningKey), HandOverInvestigationCmd())
                fails()
            }
        }
        // Wrong output state.
        ledgerServices.ledger {
            transaction {
                input(BLOW_WHISTLE_CONTRACT_ID, validInput)
                output(BLOW_WHISTLE_CONTRACT_ID, DummyState(0))
                command(listOf(oldInvestigator.owningKey, newInvestigator.owningKey), HandOverInvestigationCmd())
                fails()
            }
        }

        ledgerServices.ledger {
            transaction {
                input(BLOW_WHISTLE_CONTRACT_ID, validInput)
                output(BLOW_WHISTLE_CONTRACT_ID, validOutput)
                command(listOf(oldInvestigator.owningKey, newInvestigator.owningKey), HandOverInvestigationCmd())
                verifies()
            }
        }
    }

    @Test
    fun `A HandOverInvestigation transaction should be signed by the old investigator and the new investigator`() {
        // No old investigator signature.
        ledgerServices.ledger {
            transaction {
                input(BLOW_WHISTLE_CONTRACT_ID, validInput)
                output(BLOW_WHISTLE_CONTRACT_ID, validOutput)
                command(newInvestigator.owningKey, HandOverInvestigationCmd())
                fails()
            }
        }
        // No new investigator signature.
        ledgerServices.ledger {
            transaction {
                input(BLOW_WHISTLE_CONTRACT_ID, validInput)
                output(BLOW_WHISTLE_CONTRACT_ID, validOutput)
                command(oldInvestigator.owningKey, HandOverInvestigationCmd())
                fails()
            }
        }

        ledgerServices.ledger {
            transaction {
                input(BLOW_WHISTLE_CONTRACT_ID, validInput)
                output(BLOW_WHISTLE_CONTRACT_ID, validOutput)
                command(listOf(newInvestigator.owningKey, oldInvestigator.owningKey), HandOverInvestigationCmd())
                verifies()
            }
        }
    }

    @Test
    fun `A HandOverInvestigation transaction should only change the investigator`() {
        // Change in the company being named.
        ledgerServices.ledger {
            transaction {
                input(BLOW_WHISTLE_CONTRACT_ID, validInput)
                output(BLOW_WHISTLE_CONTRACT_ID, validOutput.copy(badCompany = TestIdentity(ALICE_NAME).party))
                command(listOf(newInvestigator.owningKey, oldInvestigator.owningKey), HandOverInvestigationCmd())
                fails()
            }
        }
        // Change in the whistle-blower.
        ledgerServices.ledger {
            transaction {
                input(BLOW_WHISTLE_CONTRACT_ID, validInput)
                output(BLOW_WHISTLE_CONTRACT_ID, validOutput.copy(whistleBlower = oldInvestigator))
                command(listOf(newInvestigator.owningKey, oldInvestigator.owningKey), HandOverInvestigationCmd())
                fails()
            }
        }

        ledgerServices.ledger {
            transaction {
                input(BLOW_WHISTLE_CONTRACT_ID, validInput)
                output(BLOW_WHISTLE_CONTRACT_ID, validOutput)
                command(listOf(newInvestigator.owningKey, oldInvestigator.owningKey), HandOverInvestigationCmd())
                verifies()
            }
        }
    }
}