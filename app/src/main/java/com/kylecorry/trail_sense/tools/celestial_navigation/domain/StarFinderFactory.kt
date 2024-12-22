package com.kylecorry.trail_sense.tools.celestial_navigation.domain

class StarFinderFactory {

    fun getStarFinder(): StarFinder {
        return EnsembleStarFinder(
            DifferenceOfGaussiansStarFinder(0.3f, 2, 8, imageSize = 1000),
            StandardDeviationStarFinder(5f),
            PercentOfMaxStarFinder(0.6f),
            mergeDistance = 5f
        )
    }

}