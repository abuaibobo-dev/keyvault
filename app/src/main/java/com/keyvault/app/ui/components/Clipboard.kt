package com.keyvault.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.security.MessageDigest

object SensitiveClipboard {
    private val handler = Handler(Looper.getMainLooper())
    private var pendingHash: ByteArray? = null
    private var expiresAt = 0L

    fun copy(context: Context, value: String) {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText("KeyVault", value))
        val hash = digest(value)
        pendingHash = hash
        expiresAt = SystemClock.elapsedRealtime() + 30_000
        handler.postDelayed({ clearExpired(context.applicationContext) }, 30_000)
    }

    fun clearExpired(context: Context) {
        val expected = pendingHash ?: return
        if (SystemClock.elapsedRealtime() < expiresAt) return
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        try {
            val current = manager.primaryClip
            val text = if (current != null && current.itemCount > 0) current.getItemAt(0).text?.toString() else null
            if (text != null && MessageDigest.isEqual(expected, digest(text)))
                manager.setPrimaryClip(ClipData.newPlainText("", ""))
            pendingHash = null
        } catch (_: Exception) { /* Retry on foreground return. */ }
    }

    private fun digest(value: String): ByteArray = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
}
