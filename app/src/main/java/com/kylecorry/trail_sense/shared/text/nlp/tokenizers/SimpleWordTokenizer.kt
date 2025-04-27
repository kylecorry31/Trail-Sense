package com.kylecorry.trail_sense.shared.text.nlp.tokenizers

class SimpleWordTokenizer(private val preservedWords: Set<String> = emptySet()) : Tokenizer {

    // Letters, numbers, and apostrophes followed by letters
    private val wordRegex = Regex("[a-zA-Z0-9]+(?:'[a-zA-Z]+)?")

    override fun tokenize(text: String): List<String> {

        val preservedWordRegex = Regex(preservedWords.joinToString("|") { Regex.escape(it) })
        val fullWordRegex = if (preservedWords.any()) {
            Regex("\\b(?:${preservedWordRegex.pattern}|${wordRegex.pattern})\\b", RegexOption.IGNORE_CASE)
        } else {
            wordRegex
        }

        return fullWordRegex.findAll(text).map { it.value }.toList()
    }

}