package com.kylecorry.trail_sense.shared.text.nlp.processors

class EnglishContractionSplitter(private val additionalContractions: Map<String, List<String>> = emptyMap()) :
    TokenProcessor {

    fun split(words: List<String>): List<String> {
        return words.flatMap { split(it) }
    }

    fun split(word: String): List<String> {
        val contractions = mapOf(
            "n't" to "not",
            "'ve" to "have",
            "'ll" to "will",
            "'re" to "are",
            "'d" to "would",
            "'m" to "am",
        )

        // Contractions that aren't just the base word + a suffix
        val specialContractions = mapOf(
            "it's" to listOf("it", "is"),
            "he's" to listOf("he", "is"),
            "she's" to listOf("she", "is"),
            "that's" to listOf("that", "is"),
            "there's" to listOf("there", "is"),
            "what's" to listOf("what", "is"),
            "where's" to listOf("where", "is"),
            "who's" to listOf("who", "is"),
            "how's" to listOf("how", "is"),
            "let's" to listOf("let", "us"),
            "cannot" to listOf("can", "not"),
            "won't" to listOf("will", "not"),
            "shan't" to listOf("shall", "not"),
            "can't" to listOf("can", "not"),
        ) + additionalContractions

        if (word in specialContractions) {
            return specialContractions[word] ?: listOf()
        }

        for ((contraction, replacement) in contractions) {
            if (word.endsWith(contraction)) {
                return listOf(word.substring(0, word.length - contraction.length), replacement)
            }
        }

        return listOf(word)
    }

    override fun process(tokens: List<String>): List<String> {
        return split(tokens)
    }

}