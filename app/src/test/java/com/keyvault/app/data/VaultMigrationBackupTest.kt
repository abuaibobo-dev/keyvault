package com.keyvault.app.data

import com.keyvault.app.crypto.VaultCrypto
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Base64
import java.security.GeneralSecurityException

class VaultMigrationBackupTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun readsV1AndWritesV2WithoutLosingData() {
        val password = "old-password".toCharArray()
        val salt = VaultCrypto.newSalt()
        val key = VaultCrypto.deriveKey(password, salt)
        val old = JSONObject().put("version", 1)
            .put("keys", JSONArray().put(JSONObject().put("id", "key-1").put("name", "Legacy")
                .put("value", "secret").put("note", "memo").put("tags", JSONArray().put("work"))
                .put("createdAt", 42).put("updatedAt", 43)))
            .put("notes", JSONArray().put(JSONObject().put("id", "note-1").put("title", "Title")
                .put("body", "Body").put("tags", JSONArray().put("private"))
                .put("createdAt", 44).put("updatedAt", 45)))
        File(tmp.root, "meta.json").writeText(JSONObject().put("version", 1)
            .put("kdf", "PBKDF2-HMAC-SHA256").put("iterations", VaultCrypto.ITERATIONS)
            .put("salt", Base64.getEncoder().encodeToString(salt)).toString())
        File(tmp.root, "vault.bin").writeBytes(VaultCrypto.encrypt(key, old.toString().toByteArray()))

        val repo = VaultRepository(tmp.root)
        val (unlockedKey, vault) = repo.unlock(password)
        assertEquals("secret", vault.keys.single().value)
        assertEquals("memo", vault.keys.single().note)
        assertEquals(listOf("work"), vault.keys.single().tags)
        assertFalse(vault.keys.single().pinned)
        assertEquals("", vault.keys.single().category)
        assertFalse(vault.notes.single().favorite)
        assertEquals("Body", vault.notes.single().body)
        repo.save(unlockedKey, vault)
        val (_, loaded) = repo.unlock(password)
        assertEquals(vault, loaded)
        assertEquals(2, JSONObject(File(tmp.root, "meta.json").readText()).getInt("version"))
        val plain = VaultCrypto.decrypt(unlockedKey, File(tmp.root, "vault.bin").readBytes())
        assertEquals(2, JSONObject(String(plain)).getInt("schemaVersion"))
    }

    @Test fun encryptedExportImportRoundTrip() {
        val repo = VaultRepository(tmp.root)
        val key = repo.initVault("backup-pass".toCharArray())
        val source = Vault(
            mutableListOf(KeyItem(id = "k", name = "API", value = "secret", tags = listOf("work"), pinned = true, category = "Accounts")),
            mutableListOf(NoteItem(id = "n", title = "Note", body = "private", favorite = true, category = "Personal")),
        )
        repo.save(key, source)
        val exported = repo.exportBytes()
        val json = JSONObject(String(exported))
        assertEquals("kvault", json.getString("format"))
        assertEquals(2, json.getInt("version"))
        assertFalse(String(exported).contains("secret"))
        val imported = repo.importBytes(exported, "backup-pass".toCharArray())
        assertEquals(source, imported)
    }

    @Test(expected = GeneralSecurityException::class)
    fun wrongBackupPasswordCannotDecrypt() {
        val repo = VaultRepository(tmp.root)
        repo.initVault("right-password".toCharArray())
        repo.importBytes(repo.exportBytes(), "wrong-password".toCharArray())
    }
}
