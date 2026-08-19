package com.kylecorry.trail_sense.shared.text.nlp.processors

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class PorterStemmerTest {

    private val stemmer = PorterStemmer()

    /**
     * Words from the rule tables at https://www.tartarus.org/~martin/PorterStemmer/, with the
     * expected output of the full algorithm (not of the individual step being illustrated).
     */
    @ParameterizedTest
    @CsvSource(
        // Step 1a - plurals
        "caresses, caress",
        "ponies, poni",
        "ties, ti",
        "caress, caress",
        "cats, cat",
        // Step 1b - past tense and gerunds
        "feed, feed",
        "agreed, agre",
        "plastered, plaster",
        "bled, bled",
        "motoring, motor",
        "sing, sing",
        "conflated, conflat",
        "troubled, troubl",
        "sized, size",
        "hopping, hop",
        "tanned, tan",
        "falling, fall",
        "hissing, hiss",
        "fizzed, fizz",
        "failing, fail",
        "filing, file",
        // Step 1c - terminal y
        "happy, happi",
        "sky, sky",
        // Step 2
        "relational, relat",
        "conditional, condit",
        "rational, ration",
        "valenci, valenc",
        "hesitanci, hesit",
        "digitizer, digit",
        "conformabli, conform",
        "radicalli, radic",
        "differentli, differ",
        "vileli, vile",
        "analogousli, analog",
        "vietnamization, vietnam",
        "predication, predic",
        "operator, oper",
        "feudalism, feudal",
        "decisiveness, decis",
        "hopefulness, hope",
        "callousness, callous",
        "formaliti, formal",
        "sensitiviti, sensit",
        "sensibiliti, sensibl",
        "analogy, analog",
        // Step 3
        "triplicate, triplic",
        "formative, form",
        "formalize, formal",
        "electriciti, electr",
        "electrical, electr",
        "hopeful, hope",
        "goodness, good",
        // Step 4
        "revival, reviv",
        "allowance, allow",
        "inference, infer",
        "airliner, airlin",
        "gyroscopic, gyroscop",
        "adjustable, adjust",
        "defensible, defens",
        "irritant, irrit",
        "replacement, replac",
        "adjustment, adjust",
        "dependent, depend",
        "adoption, adopt",
        "homologou, homolog",
        "communism, commun",
        "activate, activ",
        "angulariti, angular",
        "homologous, homolog",
        "effective, effect",
        "bowdlerize, bowdler",
        // Step 5
        "probate, probat",
        "rate, rate",
        "cease, ceas",
        "controll, control",
        "roll, roll"
    )
    fun stemsReferenceWords(word: String, expected: String) {
        assertEquals(expected, stemmer.stem(word))
    }

    /**
     * A y is a vowel when it follows a consonant, which changes the measure of the stem
     */
    @ParameterizedTest
    @CsvSource(
        "abysmal, abysm",
        "acrylic, acryl",
        "abeyance, abey",
        "gyroscopic, gyroscop",
        "anonymous, anonym"
    )
    fun countsYAsAVowelWhenItFollowsAConsonant(word: String, expected: String) {
        assertEquals(expected, stemmer.stem(word))
    }

    /**
     * ion is only removed when the stem ends in s or t
     */
    @ParameterizedTest
    @CsvSource(
        "adoption, adopt",
        "abstraction, abstract",
        "abrasion, abras",
        "accordion, accordion",
        "champion, champion",
        "mission, mission"
    )
    fun onlyRemovesIonAfterSOrT(word: String, expected: String) {
        assertEquals(expected, stemmer.stem(word))
    }

    @ParameterizedTest
    @CsvSource("a", "an", "at", "by", "in", "is", "it", "of", "on", "to", "up")
    fun leavesWordsShorterThanThreeCharactersAlone(word: String) {
        assertEquals(word, stemmer.stem(word))
    }

    @Test
    fun stemsAListOfWords() {
        assertEquals(
            listOf("hike", "trail", "elev"),
            stemmer.stem(listOf("hiking", "trails", "elevation"))
        )
    }

    @Test
    fun processDelegatesToStem() {
        assertEquals(listOf("navig", "map"), stemmer.process(listOf("navigation", "maps")))
    }

    @Test
    fun additionalReplacementsTakePrecedence() {
        val stemmer = PorterStemmer(mapOf("running" to "run", "cats" to "feline"))

        assertEquals("run", stemmer.stem("running"))
        assertEquals("feline", stemmer.stem("cats"))
        // Words that are not overridden still use the normal algorithm
        assertEquals("hop", stemmer.stem("hopping"))
    }

    @Test
    fun additionalReplacementsApplyToShortWords() {
        val stemmer = PorterStemmer(mapOf("mi" to "mile"))

        assertEquals("mile", stemmer.stem("mi"))
    }

    @Test
    fun stemmingIsIdempotent() {
        val words = listOf("relational", "conditional", "hopefulness", "adjustment", "gyroscopic")

        val stemmed = stemmer.stem(words)

        assertEquals(stemmed, stemmer.stem(stemmed))
    }

    @Test
    fun stemsEmptyInput() {
        assertEquals("", stemmer.stem(""))
        assertEquals(emptyList<String>(), stemmer.stem(emptyList()))
    }
}
