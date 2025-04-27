package com.kylecorry.trail_sense.shared.text.nlp.processors

class LowercaseProcessor : TokenProcessor {
    override fun process(tokens: List<String>): List<String> {
        return tokens.map { it.lowercase() }
    }
}