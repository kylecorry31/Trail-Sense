package com.kylecorry.trail_sense.tools.survival_guide.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.text.TextUtils
import kotlin.math.max

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

    // These are words which the user may type as two words or hyphenated words
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

    // These are words which have nearly the same meaning when searched
    private val synonyms: List<Set<String>> = listOf(
        // MEDICAL
        // Headache
        setOf(
            // Preserved
            "head ache",
            "head aches",
            // Stemmed (so misspellings are expected)
            "headach",
            "migrain",
            "concuss"
        ),
        setOf(
            "bleed",
            "blood"
        ),
        // Defecation
        setOf(
            // Preserved
            "bowel movement",
            "bowel movements",
            // Stemmed (so misspellings are expected)
            "poop",
            "defec", // Defecate
            "diahrrea",
            "bowel",
            "fece" // Feces
        ),
        // Urination
        setOf(
            // Stemmed (so misspellings are expected)
            "urin", // Urine
            "pee"
        ),
        // Painkillers
        setOf(
            "painkiller",
            "aspirin",
            "ibuprofen",
            "acetaminophen",
            "tylonol",
            "advil"
        ),
        // Heartbeat
        setOf(
            // Preserved
            "heart rate",
            "heart beat",
            // Stemmed (so misspellings are expected)
            "heartbeat",
            "puls", // Pulse
            "heartrat" // Heartrate
        ),
        // Trowel
        setOf(
            "trowel",
            "shovel",
            "spade"
        ),
        // Bathroom
        setOf(
            "bathroom",
            "restroom",
            "toilet",
            "latrin", // Latrine
            "outhous", // Outhouse
            "cathol", // Cathole
            "bidet",
        ),
        // Fractures (not just medical)
        setOf(
            // Stemmed (so misspellings are expected)
            "fractur",
            "broken",
            "broke"
        ),
        // Snow
        setOf(
            "snow",
            "ice"
        ),
        // Actions
        // Building
        setOf(
            // Stemmed (so misspellings are expected)
            "build",
            "construct",
            "assembl", // Assemble
            "creat", // Create
            "make",
            "craft"
        ),
        // Gather
        setOf(
            // Stemmed (so misspellings are expected)
            "gather",
            "collect",
            "harvest",
            "pick",
            "forag", // Forage
            "get",
            "catch",
            "find",
            "search",
            "look",
            "seek",
        ),
        // Move
        setOf(
            "move",
            "movement",
            "walk",
            "hike"
        ),
        // River
        setOf(
            "river",
            "stream",
            "creek",
            "brook",
            "waterwai" // Waterway
        ),
        // Location
        setOf(
            "locat", // Location
            "place",
            "spot",
            "area",
            "site",
            "position",
            "coordin" // Coordinate
        ),
        // Issue
        setOf(
            "issu", // Issue
            "problem"
        ),
        // Phone
        setOf(
            "phone",
            "smartphon", // Smartphone
            "cell"
        ),
        // Technique
        setOf(
            "techniqu", // Technique
            "method",
            "approach",
            "process",
            "wai"
        ),
        // Flashlight
        setOf(
            "flashlight",
            "headlamp",
            "lamp",
            "lantern",
            "torch"
        ),
        // Cordage
        setOf(
            "cordag", // Cordage
            "rope",
            "cord",
            "paracord",
            "twine",
            "string",
            "shoelac" // Shoelace
        ),
        // Container
        setOf(
            "contain", // Container
            "bottl", // Bottle
            "pot",
            "jar",
            "bladder",
            "cup"
        ),
        // Glue
        setOf(
            "glue",
            "pitch",
            "adhes" // Adhesive
        ),
        // Tape
        setOf(
            "tape",
            "duct"
        ),
        // Towel
        setOf(
            "towel",
            "washcloth",
            "facecloth",
            "rag"
        ),
        // Utensil
        setOf(
            "utensil",
            "fork",
            "spoon"
        ),
        // Animals
        setOf(
            "anim", // Animal
            "game"
        ),
        // Birds
        setOf(
            "bird",
            "fowl"
        ),
        // Gear
        setOf(
            "gear",
            "equip",
            "stuff",
            "item",
            "thing"
        ),
    )

    override fun getSectionScore(
        query: String,
        section: GuideSection
    ): Float {
        val sectionKeywords = section.keywords.joinToString(", ")

        val additionalPreservedWords =
            section.keywords.filter { it.contains(" ") || it.contains("-") }.toMutableSet()

        // Any keywords with a dash should have a synonym with a space
        val additionalSynonyms = section.keywords
            .filter { it.contains("-") }
            .map { setOf(it, it.replace("-", " ")) }

        // Add the synonyms to the preserved words
        additionalPreservedWords.addAll(additionalSynonyms.flatten())

        var sectionMatch = TextUtils.getQueryMatchPercent(
            query,
            sectionKeywords,
            preservedWords = preservedWords + additionalPreservedWords,
            additionalStopWords = additionalStopWords,
            synonyms = synonyms + additionalSynonyms,
            additionalContractions = additionalContractions,
            additionalStemWords = additionalStemWords
        )

        var headerMatch = TextUtils.getQueryMatchPercent(
            query,
            section.title ?: context.getString(R.string.overview),
            preservedWords = preservedWords + additionalPreservedWords,
            additionalStopWords = additionalStopWords,
            synonyms = synonyms + additionalSynonyms,
            additionalContractions = additionalContractions,
            additionalStemWords = additionalStemWords
        )

        var inverseHeaderMatch = TextUtils.getQueryMatchPercent(
            section.title ?: context.getString(R.string.overview),
            query,
            preservedWords = preservedWords + additionalPreservedWords,
            additionalStopWords = additionalStopWords,
            synonyms = synonyms + additionalSynonyms,
            additionalContractions = additionalContractions,
            additionalStemWords = additionalStemWords
        )

        val chapterMatch = TextUtils.getQueryMatchPercent(
            query,
            section.chapter.title,
            preservedWords = preservedWords + additionalPreservedWords,
            additionalStopWords = additionalStopWords,
            synonyms = synonyms + additionalSynonyms,
            additionalContractions = additionalContractions,
            additionalStemWords = additionalStemWords
        )

        // Rank the be prepared and overview sections lower
        if (section.title?.uppercase()?.trim() == "BE PREPARED" || section.title == null) {
            sectionMatch *= 0.9f
        }

        if (chapterMatch > 0.8f) {
            // If the chapter matches, boost the section match a little
            sectionMatch *= 1.15f
        }

        // If the header has a good match, increase it
        if (headerMatch == 1f && inverseHeaderMatch == 1f) {
            headerMatch = 1.1f
        }

        return max(sectionMatch, headerMatch)
    }
}
