package uws.ac.uk.studymate.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks in the per-account quick sign-in rules. The original bug: the settings
 * toggle read the device-global "enabled" flag, so a second account showed quick
 * sign-in as ON for a slot a *different* account actually owned. These assertions
 * make sure ownership is always evaluated per-account.
 */
class BiometricOwnershipTest {

    @Test
    fun owner_seesItEnabledForThemselves() {
        assertTrue(BiometricOwnership.isEnabledForUser(enabled = true, storedUserId = 1, currentUserId = 1))
        assertFalse(BiometricOwnership.isOwnedByAnotherUser(enabled = true, storedUserId = 1, currentUserId = 1))
    }

    @Test
    fun nonOwner_doesNotSeeItEnabled_andSeesItTaken() {
        // The reported bug: account 2 must NOT show the slot as enabled for itself,
        // and must see that another account owns it.
        assertFalse(BiometricOwnership.isEnabledForUser(enabled = true, storedUserId = 1, currentUserId = 2))
        assertTrue(BiometricOwnership.isOwnedByAnotherUser(enabled = true, storedUserId = 1, currentUserId = 2))
    }

    @Test
    fun slotDisabled_isNeitherOwnedNorEnabled() {
        assertFalse(BiometricOwnership.isEnabledForUser(enabled = false, storedUserId = 1, currentUserId = 1))
        assertFalse(BiometricOwnership.isOwnedByAnotherUser(enabled = false, storedUserId = 1, currentUserId = 1))
    }

    @Test
    fun noSession_neverCountsAsOwner() {
        // currentUserId 0 = no resolved session yet; must not be treated as the owner.
        assertFalse(BiometricOwnership.isEnabledForUser(enabled = true, storedUserId = 1, currentUserId = 0))
        assertTrue(BiometricOwnership.isOwnedByAnotherUser(enabled = true, storedUserId = 1, currentUserId = 0))
    }

    @Test
    fun enabledButNoStoredOwner_isNotOwnedByAnother() {
        // Defensive: an inconsistent enabled-but-no-owner state must not lock everyone out.
        assertFalse(BiometricOwnership.isOwnedByAnotherUser(enabled = true, storedUserId = -1, currentUserId = 2))
        assertFalse(BiometricOwnership.isEnabledForUser(enabled = true, storedUserId = -1, currentUserId = 2))
    }
}
