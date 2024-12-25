package com.kylecorry.trail_sense.tools.celestial_navigation.domain

class StarFinderFactory {

    fun getStarFinder(): StarFinder {
        // TODO: Instead of calculating the clusters on each star finder, threshold the images (binary), merge them (average), re-threshold (votes), and then find the stars
        return ScaledStarFinder(
            GrayscaleStarFinder(
                EnsembleStarFinder(
                    DifferenceOfGaussiansStarFinder(0.3f, 2, 8),
                    StandardDeviationStarFinder(5f),
                    PercentOfMaxStarFinder(0.6f),
                    mergeDistance = 5f
                ),
                inPlace = true
            ), 1000
        )
    }

}