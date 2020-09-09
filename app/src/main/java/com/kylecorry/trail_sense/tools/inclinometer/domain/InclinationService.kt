package com.kylecorry.trail_sense.tools.inclinometer.domain

class InclinationService {

    private val riskClassifier = AvalancheRiskClassifier()
    private val heightCalculator = HeightCalculator()

    fun getAvalancheRisk(inclination: Float): AvalancheRisk {
        return riskClassifier.classify(inclination)
    }

    fun estimateHeight(
        distanceAwayMeters: Float,
        inclination: Float,
        phoneHeightMeters: Float
    ): Float {
        return heightCalculator.calculate(distanceAwayMeters, inclination, phoneHeightMeters)
    }


}