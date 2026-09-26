package com.keyvault.app.ui

import android.content.Context

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("keyvault_settings", Context.MODE_PRIVATE)
    var lockMinutes: Int
        get() = prefs.getInt("lock_minutes", 0)
        set(value) { prefs.edit().putInt("lock_minutes", value).apply() }
    var theme: String
        get() = prefs.getString("theme", "dark") ?: "dark"
        set(value) { prefs.edit().putString("theme", value).apply() }
    var accent: Int
        get() = prefs.getInt("accent", 0)
        set(value) { prefs.edit().putInt("accent", value).apply() }
    var biometricEnabled: Boolean
        get() = prefs.getBoolean("biometric", false)
        set(value) { prefs.edit().putBoolean("biometric", value).apply() }
}
