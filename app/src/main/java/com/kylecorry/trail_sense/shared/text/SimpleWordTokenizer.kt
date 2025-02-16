package com.kylecorry.trail_sense.shared.text

class SimpleWordTokenizer : Tokenizer {

    // Letters, numbers, and apostrophes followed by letters
    private val wordRegex = Regex("[a-zA-Z0-9]+(?:'[a-zA-Z]+)?")

    override fun tokenize(text: String): List<String> {
        return wordRegex.findAll(text).map { it.value }.toList()
    }

}