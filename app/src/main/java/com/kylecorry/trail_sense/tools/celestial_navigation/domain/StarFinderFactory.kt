package com.kylecorry.trail_sense.tools.celestial_navigation.domain

class StarFinderFactory {

    fun getStarFinder(): StarFinder {
        val imageSize = 1000
        // TODO: Instead of calculating the clusters on each star finder, threshold the images (binary), merge them (average), re-threshold (votes), and then find the stars
        return EnsembleStarFinder(
            DifferenceOfGaussiansStarFinder(0.3f, 2, 8, imageSize = imageSize),
            StandardDeviationStarFinder(5f, imageSize = imageSize),
            PercentOfMaxStarFinder(0.6f, imageSize = imageSize),
            mergeDistance = 5f
        )
    }

}