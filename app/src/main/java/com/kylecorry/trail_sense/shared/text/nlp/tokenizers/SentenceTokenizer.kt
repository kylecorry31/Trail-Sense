package com.kylecorry.trail_sense.shared.text.nlp.tokenizers

class SentenceTokenizer : Tokenizer {

    // A sentence is a sequence of words followed by a period, exclamation point, question mark, or newline
    private val sentenceRegex = Regex("[^.!?\\n]+(?:\\.|!|\\?|\\n|\$)")

    override fun tokenize(text: String): List<String> {
        return sentenceRegex.findAll(text).map { it.value.trim() }.toList()
    }

}