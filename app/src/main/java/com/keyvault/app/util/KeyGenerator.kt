package com.keyvault.app.util

import java.security.SecureRandom

object KeyGenerator {
    private const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LOWER = "abcdefghijklmnopqrstuvwxyz"
    private const val DIGITS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()-_=+[]{}?"
    private const val AMBIGUOUS = "O0Il1|"

    fun generate(
        length: Int = 32,
        random: SecureRandom = SecureRandom(),
        upper: Boolean = true,
        lower: Boolean = true,
        digits: Boolean = true,
        symbols: Boolean = false,
        excludeAmbiguous: Boolean = false,
    ): String {
        require(length in 8..128) { "length must be 8..128" }
        val groups = listOfNotNull(
            UPPER.takeIf { upper }, LOWER.takeIf { lower }, DIGITS.takeIf { digits }, SYMBOLS.takeIf { symbols },
        ).map { if (excludeAmbiguous) it.filterNot(AMBIGUOUS::contains) else it }
        require(groups.isNotEmpty()) { "select at least one character set" }
        val pool = groups.joinToString("")
        val chars = MutableList(length) { pool[random.nextInt(pool.length)] }
        groups.forEachIndexed { index, group -> chars[index] = group[random.nextInt(group.length)] }
        for (i in chars.lastIndex downTo 1) {
            val j = random.nextInt(i + 1)
            val temp = chars[i]; chars[i] = chars[j]; chars[j] = temp
        }
        return chars.joinToString("")
    }

    fun entropyBits(length: Int, upper: Boolean, lower: Boolean, digits: Boolean, symbols: Boolean, excludeAmbiguous: Boolean): Int {
        val pool = listOfNotNull(UPPER.takeIf { upper }, LOWER.takeIf { lower }, DIGITS.takeIf { digits }, SYMBOLS.takeIf { symbols })
            .joinToString("").let { if (excludeAmbiguous) it.filterNot(AMBIGUOUS::contains) else it }
        return if (pool.isEmpty()) 0 else (length * kotlin.math.log2(pool.length.toDouble())).toInt()
    }
}
