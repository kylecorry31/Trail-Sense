package com.kylecorry.trail_sense.shared.dem.colors

class SlopeColorMapFactory {
    fun getSlopeColorMap(strategy: SlopeColorStrategy): SlopeColorMap {
        return when (strategy) {
            SlopeColorStrategy.WhiteToRed -> WhiteToRedSlopeColorMap()
            SlopeColorStrategy.Grayscale -> GrayscaleSlopeColorMap()
            SlopeColorStrategy.GreenToRed -> GreenToRedSlopeColorMap(true)
        }
    }
}
