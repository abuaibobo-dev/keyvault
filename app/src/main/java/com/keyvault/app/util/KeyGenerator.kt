package com.keyvault.app.util

import java.security.SecureRandom

object KeyGenerator {
    private const val CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

    fun generate(length: Int = 32, random: SecureRandom = SecureRandom()): String {
        require(length in 8..128) { "length must be 8..128" }
        val sb = StringBuilder(length)
        repeat(length) { sb.append(CHARS[random.nextInt(CHARS.length)]) }
        return sb.toString()
    }
}