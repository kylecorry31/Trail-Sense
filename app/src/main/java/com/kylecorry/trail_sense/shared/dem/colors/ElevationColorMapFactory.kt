package com.kylecorry.trail_sense.shared.dem.colors

import android.graphics.Color
import com.kylecorry.andromeda.core.ui.colormaps.InfernoColorMap
import com.kylecorry.andromeda.core.ui.colormaps.PlasmaColorMap
import com.kylecorry.andromeda.core.ui.colormaps.ViridisColorMap
import com.kylecorry.trail_sense.shared.colors.AppColor

class ElevationColorMapFactory {
    fun getElevationColorMap(strategy: ElevationColorStrategy): ElevationColorMap {
        return when (strategy) {
            ElevationColorStrategy.Brown -> SingleColorElevationColorMap(AppColor.Brown.color)
            ElevationColorStrategy.White -> SingleColorElevationColorMap(Color.WHITE)
            ElevationColorStrategy.Black -> SingleColorElevationColorMap(Color.BLACK)
            ElevationColorStrategy.Gray -> SingleColorElevationColorMap(Color.GRAY)
            ElevationColorStrategy.USGS -> USGSElevationColorMap()
            ElevationColorStrategy.Grayscale -> GrayscaleElevationColorMap()
            ElevationColorStrategy.Vibrant -> TrailSenseVibrantElevationColorMap()
            ElevationColorStrategy.Muted -> TomPattersonElevationColorMap()
            ElevationColorStrategy.Viridis -> WrappedElevationColorMap(ViridisColorMap(), 0f, 8000f)
            ElevationColorStrategy.Inferno -> WrappedElevationColorMap(InfernoColorMap(), 0f, 8000f)
            ElevationColorStrategy.Plasma -> WrappedElevationColorMap(PlasmaColorMap(), 0f, 8000f)
        }
    }
}