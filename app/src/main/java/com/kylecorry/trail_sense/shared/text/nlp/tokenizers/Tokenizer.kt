package com.kylecorry.trail_sense.shared.text.nlp.tokenizers

interface Tokenizer {
    fun tokenize(text: String): List<String>
}