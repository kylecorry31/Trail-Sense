package com.kylecorry.trail_sense.shared.text.nlp.tokenizers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SimpleWordTokenizerTest {

    private val tokenizer = SimpleWordTokenizer()

    @Test
    fun tokenizesWordsFromInternationalScripts() {
        assertEquals(
            listOf("导航", "café", "नमस्ते", "مرحبا", "123"),
            tokenizer.tokenize("导航 café नमस्ते مرحبا 123")
        )
    }

    @Test
    fun tokenizesLettersWithCombiningMarksAndNonLatinNumbers() {
        assertEquals(
            listOf("café", "١٢٣", "A1"),
            tokenizer.tokenize("\u0301café ١٢٣ A1")
        )
    }

    @Test
    fun tokenizesWordsWithStraightAndCurlyApostrophes() {
        assertEquals(
            listOf("l'été", "don’t"),
            tokenizer.tokenize("l'été don’t")
        )
    }

    @Test
    fun separatesWordsAtPunctuationAndHyphens() {
        assertEquals(
            listOf("north", "east", "trail", "sense"),
            tokenizer.tokenize("north-east, trail/sense")
        )
    }

    @Test
    fun keepsPreservedMultiWordPhrasesTogether() {
        val tokenizer = SimpleWordTokenizer(setOf("heart rate", "heart"))

        assertEquals(
            listOf("heart rate", "monitor"),
            tokenizer.tokenize("heart rate monitor")
        )
    }

    @Test
    fun onlyPreservesWholeWords() {
        val tokenizer = SimpleWordTokenizer(setOf("ember"))

        assertEquals(
            listOf("remember", "ember"),
            tokenizer.tokenize("remember ember")
        )
    }

    @Test
    fun ignoresStandaloneCombiningMarksAndNonWordCharacters() {
        assertEquals(
            emptyList<String>(),
            tokenizer.tokenize("\u0301 🧭 ---  ")
        )
    }

    @Test
    fun ignoresEmptyPreservedWords() {
        val tokenizer = SimpleWordTokenizer(setOf(""))

        assertEquals(
            listOf("hello"),
            tokenizer.tokenize(" hello")
        )
    }
}
