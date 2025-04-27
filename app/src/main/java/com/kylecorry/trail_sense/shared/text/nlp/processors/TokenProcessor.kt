package com.kylecorry.trail_sense.shared.text.nlp.processors

interface TokenProcessor {
    fun process(tokens: List<String>): List<String>
}