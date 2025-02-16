package com.kylecorry.trail_sense.shared.text

class KeywordTokenizer(
    additionalContractions: Map<String, List<String>> = emptyMap(),
    additionalStopWords: Set<String> = emptySet(),
    additionalStemWords: Map<String, String> = emptyMap()
) : Tokenizer {

    private val wordTokenizer = SimpleWordTokenizer()
    private val contractionSplitter = ContractionSplitter(additionalContractions)
    private val stopWordRemover = StopWordRemover(additionalStopWords)
    private val stemmer = PorterStemmer(additionalStemWords)

    override fun tokenize(text: String): List<String> {
        return stemmer.stem(
            stopWordRemover.clean(
                contractionSplitter.split(
                    wordTokenizer.tokenize(text).map { it.lowercase() }
                )
            )
        )
    }


}