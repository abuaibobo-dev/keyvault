package com.keyvault.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratorStrengthTest {
    @Test fun selectedCharacterGroupsArePresent() {
        val value = KeyGenerator.generate(32, upper = true, lower = true, digits = true, symbols = true, excludeAmbiguous = true)
        assertTrue(value.any { it.isUpperCase() })
        assertTrue(value.any { it.isLowerCase() })
        assertTrue(value.any { it.isDigit() })
        assertTrue(value.any { !it.isLetterOrDigit() })
        assertTrue(value.none { it in "O0Il1|" })
    }
    @Test fun strengthIncreasesWithLengthAndCharset() {
        assertTrue(KeyGenerator.entropyBits(32, true, true, true, true, false) >
            KeyGenerator.entropyBits(8, true, false, false, false, false))
        assertEquals(0, KeyGenerator.entropyBits(32, false, false, false, false, false))
    }
}
