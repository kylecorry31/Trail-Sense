package com.kylecorry.trail_sense.shared

import java.text.Normalizer

// TODO: Extract this to Andromeda
object Slugify {

    private val invalidChars = Regex("[^a-z0-9\\s-]")
    private val multipleSpaces = Regex("\\s+")
    private val whitespace = Regex("\\s")
    private val nonSpacingMark = "\\p{Mn}+".toRegex()

    fun String.slugify(): String {
        return this
            .removeAccents()
            .lowercase()
            .replace(invalidChars, "")
            .replace(multipleSpaces, " ")
            .trim()
            .replace(whitespace, "-")
    }

    private fun String.removeAccents(): String {
        // Adapted from https://stackoverflow.com/questions/51731574/removing-accents-and-diacritics-in-kotlin
        return Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace(nonSpacingMark, "")
    }

}