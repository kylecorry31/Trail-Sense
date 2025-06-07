package com.kylecorry.trail_sense.tools.survival_guide

import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.SurvivalGuideSearch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SurvivalGuideSearchTest {

    private val survivalGuideSearch = SurvivalGuideSearch(context)

    @Test
    fun canSearch() {
        search("What to do if bitten by a snake?", "Medical", "Bites, stings, and attacks", "Snakes")
        search("How to catch a fish", "Food", "Fish", "Fishing")
        search("Lighter", "Fire", "Starting a fire", "Lighter")
        search("I'm lost", "Overview", "What to do in a survival situation")
        search("Help", "Overview", "What to do in a survival situation", "Plan")
        search("How do I call for help", "Overview", "Signaling for help", "Cell phone")
        search("How do I use my new backpacking stove", "Fire", "Starting a fire", "Camp stove")
        search("I think I broke my arm", "Medical", "Fractures")
        search("broken arm", "Medical", "Fractures")
        search("chest pain", "Medical", "Heart problems")
        search(
            "My friend fell through the ice",
            "Medical",
            "Submerged in water",
            "Falling through ice"
        )
        search("Perform CPR", "Medical", "Heart problems")
        search("Build a shelter quick", "Shelter", "Building a shelter")
        search("Best type of wood for a fire", "Fire", "Increasing warmth", "Types of wood")
        search("How do I find North with a compass?", "Navigation", "Compass")
        search("How do I read a compass?", "Navigation", "Compass", "Bearings")
        search("The sky is getting dark", "Weather", "Night")
        search("Dark clouds", "Weather", "Forecasting", "Signs of worsening weather")
        search("Cloud types", "Weather", "Forecasting", "Clouds")
        search("How do I tie a bowline knot", "Shelter", "Knots", "Bowline")
        search("Start a fire with flint and steel", "Fire", "Starting a fire", "Flint and steel")
        search("Knots", "Shelter", "Knots")
        search("find location on map", "Navigation", "Map", "Determining location")
        search("help me calm down", "Medical", "Panic attacks and calming techniques")
        search("get help", "Overview", "Signaling for help")
        search("remove tick", "Medical", "Bites, stings, and attacks", "Ticks")

        // Spelling mistakes
        search("how do i reed map", "Navigation", "Map")
        search("get watter", "Water", "Finding water")
    }

    private fun search(
        query: String,
        expectedChapter: String? = null,
        expectedSection: String? = null,
        expectedSubsection: String? = null
    ) = runBlocking {
        val result = survivalGuideSearch.search(query).firstOrNull()
        if (expectedChapter == null) {
            assertEquals(result, null)
            return@runBlocking
        }

        assertEquals(expectedChapter, result?.chapter?.title)
        assertEquals(expectedSection, result?.heading)
        assertEquals(expectedSubsection, result?.bestSubsection?.heading)
    }
}