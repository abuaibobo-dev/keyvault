package com.keyvault.app.util

import com.keyvault.app.data.KeyItem
import com.keyvault.app.data.NoteItem
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchOrganizationTest {
    @Test fun keysSearchCategoryFilterTagAndPutPinnedFirst() {
        val normal = KeyItem(id = "a", name = "Alpha", value = "x", tags = listOf("work"), category = "Accounts", updatedAt = 2)
        val pinned = KeyItem(id = "b", name = "Beta", value = "y", tags = listOf("work"), category = "Accounts", pinned = true, updatedAt = 1)
        val other = KeyItem(id = "c", name = "Other", value = "z", tags = listOf("home"), category = "Personal")
        assertEquals(listOf("b", "a"), SearchFilter.keys(listOf(normal, other, pinned), "", "work", "Accounts").map { it.id })
        assertEquals(listOf("a", "b"), SearchFilter.keys(listOf(normal, pinned), "account").map { it.id }.sorted())
    }

    @Test fun notesFilterFavoriteAndTagWithPinnedFirst() {
        val one = NoteItem(id = "1", title = "First", body = "a", tags = listOf("todo"), favorite = true, category = "Work")
        val two = NoteItem(id = "2", title = "Second", body = "b", tags = listOf("todo"), favorite = true, pinned = true, category = "Work")
        val three = NoteItem(id = "3", title = "Third", body = "c", tags = listOf("todo"), category = "Work")
        assertEquals(listOf("2", "1"), SearchFilter.notes(listOf(one, two, three), "", "todo", "Work", true).map { it.id })
    }
}
