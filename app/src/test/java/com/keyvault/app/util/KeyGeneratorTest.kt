package com.keyvault.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.GeneralSecurityException

class KeyGeneratorTest {
    @Test fun defaultLengthIs32() {
        assertEquals(32, KeyGenerator.generate().length)
    }
    @Test fun respectsLength() {
        assertEquals(8, KeyGenerator.generate(8).length)
        assertEquals(128, KeyGenerator.generate(128).length)
    }
    @Test fun charsetIsAlphanumeric() {
        val s = KeyGenerator.generate(128)
        assertTrue(s.all { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' })
    }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsTooShort() { KeyGenerator.generate(7) }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsTooLong() { KeyGenerator.generate(129) }
}