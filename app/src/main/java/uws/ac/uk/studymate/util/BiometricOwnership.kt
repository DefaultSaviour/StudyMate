package uws.ac.uk.studymate.util

/**
 * Pure rules for the device's single biometric / quick sign-in slot.
 *
 * Only one account on a device may own the slot. The biometric store is global
 * (one `enabled` flag + one `storedUserId`), so "is quick sign-in on for THIS
 * account?" must always be answered per-account — never from the global enabled
 * flag alone. Reading the global flag was the bug where a second account's
 * settings toggle showed "enabled" for a slot another account actually owned.
 *
 * Kept Android-free so it can be unit-tested directly (see BiometricOwnershipTest).
 */
object BiometricOwnership {

    /** True only when the slot is enabled AND owned by [currentUserId]. */
    fun isEnabledForUser(enabled: Boolean, storedUserId: Int, currentUserId: Int): Boolean =
        enabled && currentUserId > 0 && storedUserId == currentUserId

    /** True when the slot is enabled and held by a *different* account than [currentUserId]. */
    fun isOwnedByAnotherUser(enabled: Boolean, storedUserId: Int, currentUserId: Int): Boolean =
        enabled && storedUserId > 0 && storedUserId != currentUserId
}
