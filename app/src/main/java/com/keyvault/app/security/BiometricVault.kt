package com.keyvault.app.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class BiometricVault(private val activity: FragmentActivity) {
    private val prefs = activity.getSharedPreferences("keyvault_biometric", Context.MODE_PRIVATE)
    private val alias = "keyvault_biometric_master"
    private val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    fun available(): Boolean = BiometricManager.from(activity)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    fun hasCredential(): Boolean = prefs.contains("blob") && prefs.contains("iv")

    fun clear() { prefs.edit().clear().apply(); store.deleteEntry(alias) }

    private fun secret(): SecretKey {
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationValidityDurationSeconds(-1)
            .build())
        return generator.generateKey()
    }

    private fun prompt(cipher: Cipher, title: String, success: (Cipher) -> Unit, failure: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometric = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                result.cryptoObject?.cipher?.let(success) ?: failure()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { failure() }
        })
        val info = BiometricPrompt.PromptInfo.Builder().setTitle(title)
            .setSubtitle("KeyVault").setNegativeButtonText("使用主密码").build()
        biometric.authenticate(info, BiometricPrompt.CryptoObject(cipher))
    }

    fun enroll(password: CharArray, done: (Boolean) -> Unit) {
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, secret()) }
            prompt(cipher, "启用生物识别", { authorized ->
                try {
                    val encrypted = authorized.doFinal(String(password).toByteArray(Charsets.UTF_8))
                    prefs.edit().putString("iv", Base64.getEncoder().encodeToString(authorized.iv))
                        .putString("blob", Base64.getEncoder().encodeToString(encrypted)).apply()
                    done(true)
                } catch (_: Exception) { done(false) }
                finally { password.fill('\u0000') }
            }, { password.fill('\u0000'); done(false) })
        } catch (_: Exception) { password.fill('\u0000'); done(false) }
    }

    fun unlock(done: (CharArray?) -> Unit) {
        try {
            val iv = Base64.getDecoder().decode(prefs.getString("iv", ""))
            val blob = Base64.getDecoder().decode(prefs.getString("blob", ""))
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, secret(), GCMParameterSpec(128, iv)) }
            prompt(cipher, "解锁 KeyVault", { authorized ->
                try { done(String(authorized.doFinal(blob), Charsets.UTF_8).toCharArray()) }
                catch (_: Exception) { done(null) }
            }, { done(null) })
        } catch (_: Exception) { done(null) }
    }
}
