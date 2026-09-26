package com.keyvault.app.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class VaultCryptoTest {
    private val pw = "correct horse".toCharArray()

    @Test fun roundTrip() {
        val salt = VaultCrypto.newSalt()
        val key = VaultCrypto.deriveKey(pw, salt)
        val msg = "秘密：sk-abc123\n多行".toByteArray()
        val blob = VaultCrypto.encrypt(key, msg)
        assertArrayEquals(msg, VaultCrypto.decrypt(key, blob))
    }
    @Test fun ciphertextDiffersFromPlaintext() {
        val key = VaultCrypto.deriveKey(pw, VaultCrypto.newSalt())
        val blob = VaultCrypto.encrypt(key, "hello".toByteArray())
        assertFalse(String(blob).contains("hello"))
    }
    @Test fun randomIvMakesCiphertextsDiffer() {
        val key = VaultCrypto.deriveKey(pw, VaultCrypto.newSalt())
        val a = VaultCrypto.encrypt(key, "same".toByteArray())
        val b = VaultCrypto.encrypt(key, "same".toByteArray())
        assertNotEquals(a.toList(), b.toList())
    }
    @Test(expected = GeneralSecurityException::class)
    fun wrongPasswordFails() {
        val salt = VaultCrypto.newSalt()
        val blob = VaultCrypto.encrypt(VaultCrypto.deriveKey(pw, salt), "data".toByteArray())
        VaultCrypto.decrypt(VaultCrypto.deriveKey("wrong pass".toCharArray(), salt), blob)
    }
    @Test fun saltIs16Bytes() {
        org.junit.Assert.assertEquals(16, VaultCrypto.newSalt().size)
    }
    @Test fun fixedIvCiphertextLayoutRemainsCompatible() {
        val key = SecretKeySpec(ByteArray(32) { it.toByte() }, "AES")
        val iv = ByteArray(12) { (it + 10).toByte() }
        val message = "legacy payload".toByteArray()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val legacyBlob = iv + cipher.doFinal(message)
        assertArrayEquals(message, VaultCrypto.decrypt(key, legacyBlob))
    }
}
