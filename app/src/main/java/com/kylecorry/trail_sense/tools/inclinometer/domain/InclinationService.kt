package com.kylecorry.trail_sense.tools.inclinometer.domain

class InclinationService {

    val riskClassifier = AvalancheRiskClassifier()
    val heightCalculator = HeightCalculator()

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