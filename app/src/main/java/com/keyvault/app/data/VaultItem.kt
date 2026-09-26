package com.keyvault.app.data

import java.util.UUID

data class KeyItem(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var value: String,
    var note: String = "",
    var tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
)

data class NoteItem(
    val id: String = UUID.randomUUID().toString(),
    var title: String,
    var body: String,
    var tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
)

data class Vault(
    var keys: MutableList<KeyItem> = mutableListOf(),
    var notes: MutableList<NoteItem> = mutableListOf(),
)

object TagParser {
    fun parse(raw: String): List<String> =
        raw.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
}
