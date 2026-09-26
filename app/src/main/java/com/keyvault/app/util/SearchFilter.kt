package com.keyvault.app.util

import com.keyvault.app.data.KeyItem
import com.keyvault.app.data.NoteItem

object SearchFilter {
    fun keys(list: List<KeyItem>, q: String): List<KeyItem> {
        val query = q.trim().lowercase()
        if (query.isEmpty()) return list
        return list.filter {
            it.name.lowercase().contains(query) ||
                it.note.lowercase().contains(query) ||
                it.tags.any { t -> t.lowercase().contains(query) }
        }
    }

    fun notes(list: List<NoteItem>, q: String): List<NoteItem> {
        val query = q.trim().lowercase()
        if (query.isEmpty()) return list
        return list.filter {
            it.title.lowercase().contains(query) ||
                it.body.lowercase().contains(query) ||
                it.tags.any { t -> t.lowercase().contains(query) }
        }
    }
}
