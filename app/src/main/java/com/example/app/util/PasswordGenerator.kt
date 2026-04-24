package com.example.app.util

import com.example.app.data.model.PasswordOptions
import java.security.SecureRandom

object PasswordGenerator {

    private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private const val NUMBERS = "0123456789"
    private const val SYMBOLS = "!@#\$%^&*()_+-=[]{}|;:,.<>?"

    fun generate(options: PasswordOptions): String {
        val charPool = StringBuilder()
        val required = mutableListOf<Char>()

        if (options.includeUppercase) {
            charPool.append(UPPERCASE)
            required.add(UPPERCASE.random())
        }
        if (options.includeLowercase) {
            charPool.append(LOWERCASE)
            required.add(LOWERCASE.random())
        }
        if (options.includeNumbers) {
            charPool.append(NUMBERS)
            required.add(NUMBERS.random())
        }
        if (options.includeSymbols) {
            charPool.append(SYMBOLS)
            required.add(SYMBOLS.random())
        }

        if (charPool.isEmpty()) {
            return ""
        }

        val random = SecureRandom()
        val password = CharArray(options.length)

        for (i in required.indices) {
            if (i < options.length) {
                password[i] = required[i]
            }
        }

        for (i in required.size until options.length) {
            password[i] = charPool.random()
        }

        for (i in password.indices) {
            val j = random.nextInt(password.size)
            val temp = password[i]
            password[i] = password[j]
            password[j] = temp
        }

        return password.joinToString("")
    }

    private fun String.random(): Char {
        return this[SecureRandom().nextInt(length)]
    }
}