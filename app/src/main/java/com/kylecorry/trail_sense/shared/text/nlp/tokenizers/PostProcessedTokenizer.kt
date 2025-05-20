package com.kylecorry.trail_sense.shared.text.nlp.tokenizers

import com.kylecorry.trail_sense.shared.text.nlp.processors.TokenProcessor

open class PostProcessedTokenizer(
    private val tokenizer: Tokenizer,
    private val postprocessor: TokenProcessor
) : Tokenizer {
    override fun tokenize(text: String): List<String> {
        val tokens = tokenizer.tokenize(text)
        return postprocessor.process(tokens)
    }

}