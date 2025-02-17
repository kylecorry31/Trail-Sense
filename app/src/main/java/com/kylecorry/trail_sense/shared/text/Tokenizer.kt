package com.kylecorry.trail_sense.shared.text

interface Tokenizer {
    fun tokenize(text: String): List<String>
}