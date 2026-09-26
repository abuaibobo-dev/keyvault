package com.keyvault.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TagParserTest {
    @Test fun splitsAndTrims() {
        assertEquals(listOf("a", "b"), TagParser.parse(" a , b "))
    }
    @Test fun dropsEmptyAndDedupes() {
        assertEquals(listOf("a", "b"), TagParser.parse("a,, ,b,a"))
    }
    @Test fun handlesChinese() {
        assertEquals(listOf("工作", "私事"), TagParser.parse("工作, 私事"))
    }
    @Test fun emptyInput() {
        assertEquals(emptyList<String>(), TagParser.parse("   "))
    }
}
