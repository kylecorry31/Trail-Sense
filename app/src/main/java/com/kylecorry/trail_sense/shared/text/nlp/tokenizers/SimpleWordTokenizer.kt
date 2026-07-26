package com.kylecorry.trail_sense.shared.text.nlp.tokenizers

class SimpleWordTokenizer(preservedWords: Set<String> = emptySet()) : Tokenizer {

    private val preservedTokenMap = preservedWords
        .filter { it.isNotEmpty() }
        .mapIndexed { index, string ->
            "${PRESERVED_TOKEN_PREFIX}$index" to string
        }.toMap()

    private val preservedTokens = preservedTokenMap.entries
        .sortedByDescending { it.value.length }

    override fun tokenize(text: String): List<String> {
        var preservedText = text
        for ((token, word) in preservedTokens) {
            preservedText = replaceWholeWord(preservedText, word, token)
        }


        val tokens = split(preservedText)
        if (preservedTokenMap.isEmpty()) {
            return tokens
        }

        return tokens.map { preservedTokenMap[it] ?: it }
    }

    companion object {
        // Matches one or more characters that are not letters (\p{L}), combining marks (\p{M}), numbers (\p{N}), or apostrophes.
        private val separatorRegex = Regex("[^\\p{L}\\p{M}\\p{N}'’]+")
        private const val PRESERVED_TOKEN_PREFIX = "PRESERVED00000"

        private fun split(text: String): List<String> {
            return text.split(separatorRegex)
                .map { it.trim('\u0027', '’').trimStart { character -> character.isCombiningMark() } }
                .filter { token -> token.any { character -> character.isLetterOrDigit() } }
        }

        private fun replaceWholeWord(text: String, word: String, replacement: String): String {
            var searchStart = 0
            var copyStart = 0
            var result: StringBuilder? = null

            while (searchStart < text.length) {
                val index = text.indexOf(word, searchStart, ignoreCase = true)
                if (index == -1) {
                    break
                }

                val endIndex = index + word.length
                val isStartBoundary = index == 0 || !text[index - 1].isWordCharacter()
                val isEndBoundary = endIndex == text.length || !text[endIndex].isWordCharacter()
                if (isStartBoundary && isEndBoundary) {
                    val builder = result ?: StringBuilder(text.length).also { result = it }
                    builder.append(text, copyStart, index)
                    builder.append(' ').append(replacement).append(' ')
                    copyStart = endIndex
                    searchStart = endIndex
                } else {
                    searchStart = index + 1
                }
            }

            return result?.append(text, copyStart, text.length)?.toString() ?: text
        }

        private fun Char.isWordCharacter(): Boolean {
            return isLetterOrDigit() || isCombiningMark()
        }

        private fun Char.isCombiningMark(): Boolean {
            return when (Character.getType(this)) {
                Character.NON_SPACING_MARK.toInt(),
                Character.COMBINING_SPACING_MARK.toInt(),
                Character.ENCLOSING_MARK.toInt() -> true

                else -> false
            }
        }
    }

}
