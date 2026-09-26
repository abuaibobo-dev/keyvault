package com.keyvault.app.util

import com.keyvault.app.data.KeyItem
import com.keyvault.app.data.NoteItem
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchFilterTest {
    @Test fun matchesNameCaseInsensitive() {
        val list = listOf(KeyItem(name = "DeepSeek", value = "x"), KeyItem(name = "GitHub", value = "y"))
        assertEquals(listOf("DeepSeek"), SearchFilter.keys(list, "deep").map { it.name })
    }
    @Test fun matchesTag() {
        val list = listOf(KeyItem(name = "a", value = "x", tags = listOf("openai")))
        assertEquals(1, SearchFilter.keys(list, "open").size)
    }
    @Test fun blankQueryReturnsAll() {
        val list = listOf(KeyItem(name = "a", value = "x"))
        assertEquals(1, SearchFilter.keys(list, "  ").size)
    }
    @Test fun notesMatchBody() {
        val list = listOf(NoteItem(title = "t", body = "hello world"))
        assertEquals(1, SearchFilter.notes(list, "world").size)
    }
}
