package com.kylecorry.trail_sense.tools.survival_guide.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.text.search.EnglishFuzzySearchStrategy
import com.kylecorry.trail_sense.shared.text.search.SearchItem

class EnglishSurvivalGuideFuzzySearch(private val context: Context, loader: GuideLoader) :
    BaseSurvivalGuideSearch(loader) {

    private val additionalContractions = mapOf(
        "saltwater" to listOf("salt", "water"),
        "fishhook" to listOf("fish", "hook"),
        "fishhooks" to listOf("fish", "hooks"),
        "firestarter" to listOf("fire", "starter"),
        "firestarters" to listOf("fire", "starters"),
        "firewood" to listOf("fire", "wood"),
    )

    private val additionalStemWords = mapOf(
        "knives" to "knife",
        "burnt" to "burn",
        "cactus" to "cacti"
    )

    private val preservedWords: Set<String> = setOf(
        "bowel movement",
        "bowel movements",
        "head ache",
        "head aches",
        "heart rate",
        "heart beat",
        "ember"
    )

    private val additionalStopWords = setOf(
        "woods",
        "outdoors",
        "outdoor",
        "got",
        "wild",
        "survival",
        "situation",
        "situations",
        "nature",
        "natural",
        "safe",
        "safely",
        "safety",
        "avoid",
        "best",
        "tell",
        "found",
        "common",
        "nearby",
        "dangerous",
        "deadly",
        "humans",
        "human",
        "people",
        "use",
        "uses",
        "used",
        "using",
        "emergency"
    )

    private val synonyms: List<Set<String>> = listOf(
        setOf(
            "head ache",
            "head aches",
            "headach",
            "migrain",
            "concuss"
        ),
        setOf(
            "bleed",
            "blood"
        ),
        setOf(
            "bowel movement",
            "bowel movements",
            "poop",
            "defec",
            "diahrrea",
            "bowel",
            "fece"
        ),
        setOf(
            "urin",
            "pee"
        ),
        setOf(
            "painkiller",
            "aspirin",
            "ibuprofen",
            "acetaminophen",
            "tylonol",
            "advil"
        ),
        setOf(
            "heart rate",
            "heart beat",
            "heartbeat",
            "puls",
            "heartrat"
        ),
        setOf(
            "trowel",
            "shovel",
            "spade"
        ),
        setOf(
            "bathroom",
            "restroom",
            "toilet",
            "latrin",
            "outhous",
            "cathol",
            "bidet",
        ),
        setOf(
            "fractur",
            "broken",
            "broke"
        ),
        setOf(
            "snow",
            "ice"
        ),
        setOf(
            "build",
            "construct",
            "assembl",
            "creat",
            "make",
            "craft"
        ),
        setOf(
            "gather",
            "collect",
            "harvest",
            "pick",
            "forag",
            "get",
            "catch",
            "find",
            "search",
            "look",
            "seek",
        ),
        setOf(
            "move",
            "movement",
            "walk",
            "hike"
        ),
        setOf(
            "river",
            "stream",
            "creek",
            "brook",
            "waterwai"
        ),
        setOf(
            "locat",
            "place",
            "spot",
            "area",
            "site",
            "position",
            "coordin"
        ),
        setOf(
            "issu",
            "problem"
        ),
        setOf(
            "phone",
            "smartphon",
            "cell"
        ),
        setOf(
            "techniqu",
            "method",
            "approach",
            "process",
            "wai"
        ),
        setOf(
            "flashlight",
            "headlamp",
            "lamp",
            "lantern",
            "torch"
        ),
        setOf(
            "cordag",
            "rope",
            "cord",
            "paracord",
            "twine",
            "string",
            "shoelac"
        ),
        setOf(
            "contain",
            "bottl",
            "pot",
            "jar",
            "bladder",
            "cup"
        ),
        setOf(
            "glue",
            "pitch",
            "adhes"
        ),
        setOf(
            "tape",
            "duct"
        ),
        setOf(
            "towel",
            "washcloth",
            "facecloth",
            "rag"
        ),
        setOf(
            "utensil",
            "fork",
            "spoon"
        ),
        setOf(
            "anim",
            "game"
        ),
        setOf(
            "bird",
            "fowl"
        ),
        setOf(
            "gear",
            "equip",
            "stuff",
            "item",
            "thing"
        ),
    )

    private val search = EnglishFuzzySearchStrategy(
        preservedWords,
        additionalStopWords,
        synonyms,
        additionalContractions,
        additionalStemWords
    )

    override fun getSectionScore(
        query: String,
        section: GuideSection
    ): Float {
        val scoreMultiplier =
            if (section.title?.uppercase()?.trim() == "BE PREPARED" || section.title == null) {
                0.9f
            } else {
                1f
            }

        val item = SearchItem(
            "${section.chapter.title} ${section.title}",
            section.title ?: context.getString(R.string.overview),
            section.keywords,
            parent = SearchItem(section.chapter.title, section.chapter.title),
            scoreMultiplier = scoreMultiplier
        )

        return search.getSearchScore(query, item)
    }
}
