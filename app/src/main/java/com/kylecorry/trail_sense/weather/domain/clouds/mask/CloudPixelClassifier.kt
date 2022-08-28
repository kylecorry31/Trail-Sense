package com.kylecorry.trail_sense.weather.domain.clouds.mask

import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.trail_sense.shared.specifications.FalseSpecification
import kotlin.math.max

class CloudPixelClassifier(
    private val isSky: Specification<Int>,
    private val isObstacle: Specification<Int>
) : ICloudPixelClassifier {
    override fun classify(pixel: Int): SkyPixelClassification {
        return when {
            isSky.isSatisfiedBy(pixel) -> SkyPixelClassification.Sky
            isObstacle.isSatisfiedBy(pixel) -> SkyPixelClassification.Obstacle
            else -> SkyPixelClassification.Cloud
        }
    }

    companion object {
        fun default(
            skyDetectionSensitivity: Int,
            obstacleRemovalSensitivity: Int
        ): CloudPixelClassifier {
            val isSky = NRBRIsSkySpecification(skyDetectionSensitivity / 200f)

            val isObstacle =
                SaturationIsObstacleSpecification(
                    max(0.4f, 1 - obstacleRemovalSensitivity / 100f)
                )
                    .or(BrightnessIsObstacleSpecification(150 * obstacleRemovalSensitivity / 100f))
                    .or(if (obstacleRemovalSensitivity > 0) IsSunSpecification() else FalseSpecification())

            return CloudPixelClassifier(isSky, isObstacle)
        }
    }

}