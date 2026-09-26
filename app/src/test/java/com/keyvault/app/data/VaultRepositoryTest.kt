package com.keyvault.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.security.GeneralSecurityException

class VaultRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun initThenUnlockRoundTrip() {
        val repo = VaultRepository(tmp.root)
        assertFalse(repo.exists())
        val key = repo.initVault("password123".toCharArray())
        repo.save(key, Vault().apply { keys.add(KeyItem(name = "K", value = "V")) })
        assertTrue(repo.exists())

        val (k2, vault) = repo.unlock("password123".toCharArray())
        assertEquals(1, vault.keys.size)
        assertEquals("V", vault.keys[0].value)
    }

    @Test(expected = GeneralSecurityException::class)
    fun wrongPasswordThrows() {
        val repo = VaultRepository(tmp.root)
        val key = repo.initVault("password123".toCharArray())
        repo.save(key, Vault().apply { notes.add(NoteItem(title = "T", body = "B")) })
        repo.unlock("nope".toCharArray())
    }

    @Test fun vaultFileIsNotPlaintext() {
        val repo = VaultRepository(tmp.root)
        val key = repo.initVault("password123".toCharArray())
        repo.save(key, Vault().apply { keys.add(KeyItem(name = "DeepSeek", value = "sk-SECRET-123")) })
        val bytes = java.io.File(tmp.root, "vault.bin").readBytes()
        assertFalse(String(bytes, Charsets.ISO_8859_1).contains("sk-SECRET-123"))
    }

    @Test fun largeValueRoundTrips() {
        val repo = VaultRepository(tmp.root)
        val key = repo.initVault("password123".toCharArray())
        val big = "x".repeat(1_000_000)
        repo.save(key, Vault().apply { notes.add(NoteItem(title = "big", body = big)) })
        val (_, vault) = repo.unlock("password123".toCharArray())
        assertEquals(big.length, vault.notes[0].body.length)
    }
}
