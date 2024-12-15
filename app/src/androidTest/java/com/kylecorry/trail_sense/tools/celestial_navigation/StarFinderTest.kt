package com.kylecorry.trail_sense.tools.celestial_navigation

import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.andromeda.files.AssetFileSystem
import com.kylecorry.trail_sense.tools.celestial_navigation.domain.DifferenceOfGaussiansStarFinder
import com.kylecorry.trail_sense.tools.celestial_navigation.domain.PercentOfMaxStarFinder
import kotlinx.coroutines.runBlocking
import org.junit.Test

class StarFinderTest {

    @Test
    fun findStars() = runBlocking {
        val images = listOf(
            "stars/20241215_020532.jpg",
            "stars/20241215_020544.jpg",
            "stars/20241215_020631.jpg",
        )

        for (file in images) {
            val assets = AssetFileSystem(InstrumentationRegistry.getInstrumentation().context)
            val image = assets.stream(file).use {
                BitmapFactory.decodeStream(it)
            }

//            val stars = StandardDeviationStarFinder(5f).findStars(image)
            val stars = DifferenceOfGaussiansStarFinder(0.3f).findStars(image)
            assert(stars.isNotEmpty())
        }
    }

}