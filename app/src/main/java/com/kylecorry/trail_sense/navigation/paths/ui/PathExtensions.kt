package com.kylecorry.trail_sense.navigation.paths.ui

import android.content.Context
import android.graphics.Color
import com.kylecorry.trail_sense.navigation.paths.domain.Path
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import com.kylecorry.trail_sense.navigation.paths.domain.PathPointColoringStyle
import com.kylecorry.trail_sense.navigation.paths.domain.factories.*
import com.kylecorry.trail_sense.navigation.ui.IMappablePath
import com.kylecorry.trail_sense.navigation.ui.MappableLocation
import com.kylecorry.trail_sense.navigation.ui.MappablePath

fun List<PathPoint>.asMappable(context: Context, path: Path): IMappablePath {
    val colorFactory = getPointFactory(context, path.style.point)
    val strategy = colorFactory.createColoringStrategy(this)
    return MappablePath(path.id, this.map { point ->
        MappableLocation(
            point.id,
            point.coordinate,
            strategy.getColor(point) ?: Color.TRANSPARENT
        )
    }, path.style.color, path.style.line)
}

// TODO: Extract this and add solid style
private fun getPointFactory(
    context: Context,
    pointColoringStyle: PathPointColoringStyle
): IPointDisplayFactory {
    return when (pointColoringStyle) {
        PathPointColoringStyle.None -> NonePointDisplayFactory()
        PathPointColoringStyle.CellSignal -> CellSignalPointDisplayFactory(context)
        PathPointColoringStyle.Altitude -> AltitudePointDisplayFactory(context)
        PathPointColoringStyle.Time -> TimePointDisplayFactory(context)
        PathPointColoringStyle.Slope -> SlopePointDisplayFactory(context)
    }
}

