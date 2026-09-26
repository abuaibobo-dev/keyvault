package com.keyvault.app.util

import com.keyvault.app.data.KeyItem
import com.keyvault.app.data.NoteItem

object SearchFilter {
    fun keys(list: List<KeyItem>, q: String, tag: String = "", category: String = ""): List<KeyItem> {
        val query = q.trim().lowercase()
        return list.filter {
            (tag.isEmpty() || it.tags.contains(tag)) &&
            (category.isEmpty() || it.category == category) &&
            (query.isEmpty() || it.name.lowercase().contains(query) ||
                it.note.lowercase().contains(query) ||
                it.category.lowercase().contains(query) ||
                it.tags.any { t -> t.lowercase().contains(query) })
        }.sortedWith(compareByDescending<KeyItem> { it.pinned }.thenByDescending { it.updatedAt })
    }

    fun notes(list: List<NoteItem>, q: String, tag: String = "", category: String = "", favoritesOnly: Boolean = false): List<NoteItem> {
        val query = q.trim().lowercase()
        return list.filter {
            (tag.isEmpty() || it.tags.contains(tag)) &&
            (category.isEmpty() || it.category == category) &&
            (!favoritesOnly || it.favorite) &&
            (query.isEmpty() || it.title.lowercase().contains(query) ||
                it.body.lowercase().contains(query) ||
                it.category.lowercase().contains(query) ||
                it.tags.any { t -> t.lowercase().contains(query) })
        }.sortedWith(compareByDescending<NoteItem> { it.pinned }.thenByDescending { it.updatedAt })
    }
}
