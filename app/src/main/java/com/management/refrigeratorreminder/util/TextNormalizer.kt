package com.management.refrigeratorreminder.util

import java.util.Locale

object TextNormalizer {
    private val noiseRegex = Regex("[\\s\\-_/()\\[\\]{}.,]+")

    fun normalize(value: String): String {
        return value
            .trim()
            .lowercase(Locale.KOREA)
            .replace(noiseRegex, "")
    }
}
