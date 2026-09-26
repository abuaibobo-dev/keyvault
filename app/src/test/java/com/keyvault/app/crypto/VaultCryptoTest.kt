package com.keyvault.app.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.security.GeneralSecurityException

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
}