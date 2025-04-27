package com.kylecorry.trail_sense.shared.text.nlp.processors

class SequentialProcessor(vararg val processors: TokenProcessor) : TokenProcessor {
    override fun process(tokens: List<String>): List<String> {
        var processedTokens = tokens
        for (processor in processors) {
            processedTokens = processor.process(processedTokens)
        }
        return processedTokens
    }
}