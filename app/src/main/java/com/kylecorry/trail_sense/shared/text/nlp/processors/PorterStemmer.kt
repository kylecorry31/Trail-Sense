package com.kylecorry.trail_sense.shared.text.nlp.processors

// From https://www.tartarus.org/~martin/PorterStemmer/
class PorterStemmer(private val additionalReplacements: Map<String, String> = emptyMap()) :
    TokenProcessor {
    private fun step1a(word: String): String {
        for ((suffix, replacement) in replacements1a) {
            if (word.endsWith(suffix)) {
                return replaceEnd(word, suffix, replacement)
            }
        }

        return word
    }

    private fun step1b(word: String): String {
        var completedRemoval = false
        var updated = word
        if (word.endsWith("eed")) {
            if (measure(replaceEnd(word, "eed", "")) > 0) {
                return replaceEnd(word, "eed", "ee")
            }
        } else if (word.endsWith("ed") && hasVowel(replaceEnd(word, "ed", ""))) {
            updated = replaceEnd(word, "ed", "")
            completedRemoval = true
        } else if (word.endsWith("ing") && hasVowel(replaceEnd(word, "ing", ""))) {
            updated = replaceEnd(word, "ing", "")
            completedRemoval = true
        }

        if (!completedRemoval) {
            return updated
        }

        if (updated.endsWith("at") || updated.endsWith("bl") || updated.endsWith("iz")) {
            return updated + "e"
        }

        if (endsWithDoubleConsonant(updated) && getCharacter(updated, -1) !in lsz) {
            return removeEnd(updated, 1)
        }

        if (measure(updated) == 1 && endsWithCVC(updated)) {
            return updated + "e"
        }

        return updated
    }

    private fun step1c(word: String): String {
        return if (word.endsWith("y") && hasVowel(replaceEnd(word, "y", ""))) {
            replaceEnd(word, "y", "i")
        } else {
            word
        }
    }

    private fun step2(word: String): String {
        for ((suffix, replacement) in replacements2.entries) {
            if (word.endsWith(suffix) && measure(replaceEnd(word, suffix, "")) > 0) {
                return replaceEnd(word, suffix, replacement)
            }
        }
        return word
    }

    private fun step3(word: String): String {
        for ((suffix, replacement) in replacements3.entries) {
            if (word.endsWith(suffix) && measure(replaceEnd(word, suffix, "")) > 0) {
                return replaceEnd(word, suffix, replacement)
            }
        }
        return word
    }

    private fun step4(word: String): String {
        for (suffix in replacements4) {
            if (word.endsWith(suffix)) {
                if (measure(replaceEnd(word, suffix, "")) > 1) {
                    return replaceEnd(word, suffix, "")
                }
                return word
            }
        }

        if (word.endsWith("ion") && measure(replaceEnd(word, "ion", "")) > 1 && getCharacter(
                word,
                -4
            ) !in st
        ) {
            return replaceEnd(word, "ion", "")
        }

        return word
    }

    private fun step5(word: String): String {
        var updated = word
        if (word.endsWith("e")) {
            val trimmed = removeEnd(word, 1)
            if (measure(trimmed) > 1) {
                updated = trimmed
            } else if (measure(trimmed) == 1 && !endsWithCVC(trimmed)) {
                updated = trimmed
            }
        }

        if (measure(updated) > 1 && endsWithDoubleConsonant(updated) && getCharacter(
                updated,
                -1
            ) == 'l'
        ) {
            return removeEnd(updated, 1)
        }

        return updated
    }

    /**
     * Stems a word. Assumes the words have been run through a contraction splitter and are all lowercase.
     */
    fun stem(word: String): String {
        var stemmed = word
        if (stemmed in additionalReplacements) {
            return additionalReplacements[stemmed]!!
        }

        if (stemmed.length < 3) {
            return stemmed
        }

        stemmed = step1a(stemmed)
        stemmed = step1b(stemmed)
        stemmed = step1c(stemmed)
        stemmed = step2(stemmed)
        stemmed = step3(stemmed)
        stemmed = step4(stemmed)
        stemmed = step5(stemmed)

        return stemmed
    }

    fun stem(words: List<String>): List<String> {
        return words.map { stem(it) }
    }

    override fun process(tokens: List<String>): List<String> {
        return stem(tokens)
    }

    companion object {
        private val vowels = setOf('a', 'e', 'i', 'o', 'u')
        private val wxy = setOf('w', 'x', 'y')
        private val lsz = setOf('l', 's', 'z')
        private val st = setOf('s', 't')

        private val replacements1a = listOf(
            "sses" to "ss",
            "ies" to "i",
            "ss" to "ss",
            "s" to ""
        )

        private val replacements2 = mapOf(
            "ational" to "ate", "tional" to "tion", "enci" to "ence", "anci" to "ance",
            "izer" to "ize", "bli" to "ble", "alli" to "al", "entli" to "ent",
            "eli" to "e", "ousli" to "ous", "ization" to "ize", "ation" to "ate",
            "ator" to "ate", "alism" to "al", "iveness" to "ive", "fulness" to "ful",
            "ousness" to "ous", "aliti" to "al", "iviti" to "ive", "biliti" to "ble"
        )

        private val replacements3 = mapOf(
            "icate" to "ic", "ative" to "", "alize" to "al", "iciti" to "ic",
            "ical" to "ic", "ful" to "", "ness" to ""
        )

        private val replacements4 = listOf(
            "al", "ance", "ence", "er", "ic", "able", "ible", "ant", "ement",
            "ment", "ent", "ou", "ism", "ate", "iti", "ous", "ive", "ize"
        )

        private fun getIndex(word: String, i: Int): Int {
            return if (i < 0) {
                word.length + i
            } else {
                i
            }
        }

        private fun getCharacter(word: String, i: Int): Char? {
            val index = getIndex(word, i)
            return if (index < 0 || index > word.lastIndex) {
                null
            } else {
                word[index]
            }
        }

        private fun isConsonant(word: String, i: Int): Boolean {
            val actualIndex = getIndex(word, i)
            val char = getCharacter(word, actualIndex)
            if (char in vowels) {
                return false
            }

            if (char == 'y' && actualIndex != 0 && !isConsonant(word, actualIndex - 1)) {
                return false
            }

            return true
        }

        private fun endsWithCVC(word: String): Boolean {
            if (word.length < 3) {
                return false
            }


            val c2 = getCharacter(word, -1)

            return isConsonant(word, -3) && !isConsonant(word, -2) && isConsonant(
                word,
                -1
            ) && c2 !in wxy
        }

        private fun measure(word: String): Int {
            var count = 0
            var vowelFound = false
            for (i in word.indices) {
                if (isConsonant(word, i)) {
                    if (vowelFound) {
                        count++
                        vowelFound = false
                    }
                } else {
                    vowelFound = true
                }
            }
            return count
        }

        private fun hasVowel(word: String): Boolean {
            for (i in word.indices) {
                if (!isConsonant(word, i)) {
                    return true
                }
            }
            return false
        }

        private fun endsWithDoubleConsonant(word: String): Boolean {
            if (word.length < 2) {
                return false
            }
            val c = getCharacter(word, -1)
            return c == getCharacter(word, -2) && isConsonant(word, -1)
        }

        private fun replaceEnd(
            word: String,
            suffix: String,
            replacement: String
        ): String {
            if (!word.endsWith(suffix)) {
                return word
            }
            return word.substring(0, word.length - suffix.length) + replacement
        }

        private fun removeEnd(
            word: String,
            count: Int
        ): String {
            return word.substring(0, word.length - count)
        }
    }

}