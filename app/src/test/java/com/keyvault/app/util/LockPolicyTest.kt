package com.keyvault.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockPolicyTest {
    @Test fun immediateLocksOnReturn() {
        assertTrue(LockPolicy.shouldLock(1000, 1000, 0))
    }
    @Test fun timedLockWaitsForThreshold() {
        assertFalse(LockPolicy.shouldLock(1000, 60_999, 1))
        assertTrue(LockPolicy.shouldLock(1000, 61_000, 1))
    }
    @Test fun neverStoppedDoesNotLock() {
        assertFalse(LockPolicy.shouldLock(0, 100_000, 0))
    }
}
