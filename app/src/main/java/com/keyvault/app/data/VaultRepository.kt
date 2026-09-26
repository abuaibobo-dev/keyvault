package com.keyvault.app.data

import com.keyvault.app.crypto.VaultCrypto
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.GeneralSecurityException
import java.util.Base64
import javax.crypto.SecretKey

class VaultRepository(private val dir: File) {
    private val vaultFile = File(dir, "vault.bin")
    private val metaFile = File(dir, "meta.json")

    init { if (!dir.exists()) dir.mkdirs() }

    fun exists(): Boolean = vaultFile.isFile && metaFile.isFile

    fun initVault(password: CharArray): SecretKey {
        val salt = VaultCrypto.newSalt()
        val meta = JSONObject()
            .put("version", 1)
            .put("kdf", "PBKDF2-HMAC-SHA256")
            .put("iterations", VaultCrypto.ITERATIONS)
            .put("salt", Base64.getEncoder().encodeToString(salt))
        val key = VaultCrypto.deriveKey(password, salt)
        writeAtomic(metaFile, meta.toString().toByteArray(Charsets.UTF_8))
        save(key, Vault())
        return key
    }

    fun unlock(password: CharArray): Pair<SecretKey, Vault> {
        val meta = JSONObject(metaFile.readText(Charsets.UTF_8))
        val salt = Base64.getDecoder().decode(meta.getString("salt"))
        val iterations = meta.getInt("iterations")
        val key = VaultCrypto.deriveKey(password, salt, iterations)
        return key to load(key)
    }

    fun load(key: SecretKey): Vault {
        val plain = VaultCrypto.decrypt(key, vaultFile.readBytes())
        return fromJson(JSONObject(String(plain, Charsets.UTF_8)))
    }

    fun save(key: SecretKey, vault: Vault) {
        val plain = toJson(vault).toString().toByteArray(Charsets.UTF_8)
        writeAtomic(vaultFile, VaultCrypto.encrypt(key, plain))
    }

    private fun writeAtomic(target: File, bytes: ByteArray) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeBytes(bytes)
        if (!tmp.renameTo(target)) {
            target.delete()
            if (!tmp.renameTo(target)) throw java.io.IOException("rename failed: ${target.name}")
        }
    }

    private fun toJson(v: Vault): JSONObject {
        val root = JSONObject().put("version", 1)
        val keys = JSONArray()
        for (k in v.keys) keys.put(JSONObject()
            .put("id", k.id).put("name", k.name).put("value", k.value)
            .put("note", k.note).put("tags", JSONArray(k.tags))
            .put("createdAt", k.createdAt).put("updatedAt", k.updatedAt))
        root.put("keys", keys)
        val notes = JSONArray()
        for (n in v.notes) notes.put(JSONObject()
            .put("id", n.id).put("title", n.title).put("body", n.body)
            .put("tags", JSONArray(n.tags))
            .put("createdAt", n.createdAt).put("updatedAt", n.updatedAt))
        root.put("notes", notes)
        return root
    }

    private fun fromJson(root: JSONObject): Vault {
        val vault = Vault()
        root.optJSONArray("keys")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                vault.keys.add(KeyItem(
                    id = o.getString("id"), name = o.getString("name"),
                    value = o.getString("value"), note = o.optString("note", ""),
                    tags = o.optJSONArray("tags")?.toStringList() ?: emptyList(),
                    createdAt = o.optLong("createdAt"), updatedAt = o.optLong("updatedAt"),
                ))
            }
        }
        root.optJSONArray("notes")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                vault.notes.add(NoteItem(
                    id = o.getString("id"), title = o.getString("title"),
                    body = o.getString("body"),
                    tags = o.optJSONArray("tags")?.toStringList() ?: emptyList(),
                    createdAt = o.optLong("createdAt"), updatedAt = o.optLong("updatedAt"),
                ))
            }
        }
        return vault
    }

    private fun JSONArray.toStringList(): List<String> =
        (0 until length()).map { getString(it) }
}
