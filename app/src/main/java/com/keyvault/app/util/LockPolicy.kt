package com.keyvault.app.util

object LockPolicy {
    fun shouldLock(stoppedAtElapsedMs: Long, nowElapsedMs: Long, minutes: Int): Boolean =
        stoppedAtElapsedMs > 0 && nowElapsedMs - stoppedAtElapsedMs >= minutes.coerceAtLeast(0) * 60_000L
}
