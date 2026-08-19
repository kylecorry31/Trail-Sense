package com.kylecorry.trail_sense.shared.text.nlp.processors

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class EnglishStopWordRemoverTest {

    private val remover = EnglishStopWordRemover()

    @ParameterizedTest
    @ValueSource(
        strings = ["i", "we", "you", "he", "she", "it", "they", "what", "this", "am", "is",
            "are", "was", "have", "do", "a", "an", "the", "and", "but", "if", "of", "at",
            "for", "with", "to", "from", "in", "on", "when", "how", "all", "no", "not",
            "so", "very", "can", "will", "just", "now", "much", "onto"]
    )
    fun removesStopWords(word: String) {
        assertEquals(emptyList<String>(), remover.clean(listOf(word)))
    }

    @ParameterizedTest
    @ValueSource(strings = ["im", "ive", "youre", "dont", "wasnt", "cant", "shouldve", "aint", "hows"])
    fun removesContractionsWrittenWithoutApostrophes(word: String) {
        assertEquals(emptyList<String>(), remover.clean(listOf(word)))
    }

    @Test
    fun keepsMeaningfulWords() {
        assertEquals(
            listOf("compass", "points", "north"),
            remover.clean(listOf("the", "compass", "points", "to", "the", "north"))
        )
    }

    @Test
    fun preservesOrderAndDuplicates() {
        assertEquals(
            listOf("map", "trail", "map"),
            remover.clean(listOf("map", "and", "trail", "and", "map"))
        )
    }

    @Test
    fun stripsPossessiveSuffix() {
        assertEquals(listOf("hiker", "pack"), remover.clean(listOf("hiker's", "pack")))
    }

    @Test
    fun leavesPluralsAlone() {
        assertEquals(listOf("trails"), remover.clean(listOf("trails")))
    }

    @Test
    fun isCaseSensitive() {
        // Input is expected to already be lowercase
        assertEquals(listOf("The", "Compass"), remover.clean(listOf("The", "Compass")))
    }

    @Test
    fun additionalStopWordsSupplementTheDefaults() {
        val remover = EnglishStopWordRemover(setOf("compass"))

        assertEquals(listOf("north"), remover.clean(listOf("the", "compass", "north")))
    }

    @Test
    fun processDelegatesToClean() {
        assertEquals(listOf("compass"), remover.process(listOf("the", "compass")))
    }

    @Test
    fun handlesEmptyInput() {
        assertEquals(emptyList<String>(), remover.clean(emptyList()))
    }

    @Test
    fun canRemoveEveryWord() {
        assertEquals(emptyList<String>(), remover.clean(listOf("the", "and", "of")))
    }
}
