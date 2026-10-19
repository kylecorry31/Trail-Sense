package com.kylecorry.trail_sense.tools.paths.ui

import android.content.Context
import android.graphics.Color
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.navigation.ui.IMappablePath
import com.kylecorry.trail_sense.tools.navigation.ui.MappableLocation
import com.kylecorry.trail_sense.tools.navigation.ui.MappablePath
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.PathPointColoringStyle
import com.kylecorry.trail_sense.tools.paths.domain.factories.AltitudePointDisplayFactory
import com.kylecorry.trail_sense.tools.paths.domain.factories.CellSignalPointDisplayFactory
import com.kylecorry.trail_sense.tools.paths.domain.factories.IPointDisplayFactory
import com.kylecorry.trail_sense.tools.paths.domain.factories.NonePointDisplayFactory
import com.kylecorry.trail_sense.tools.paths.domain.factories.SlopePointDisplayFactory
import com.kylecorry.trail_sense.tools.paths.domain.factories.TimePointDisplayFactory
import com.kylecorry.trail_sense.tools.paths.domain.waypointcolors.NoDrawPointColoringStrategy
import com.kylecorry.trail_sense.tools.paths.domain.waypointcolors.SelectedPointDecorator

fun List<PathPoint>.asMappable(context: Context, path: Path): IMappablePath {
    return MappablePath(
        path.id,
        toMappableLocations(context),
        path.style.color,
        path.style.line,
        path.name
    )
}

fun List<PathPoint>.toMappableLocations(
    context: Context,
    coloringStyle: PathPointColoringStyle = PathPointColoringStyle.None
): List<MappableLocation> {
    val colorFactory = getPointFactory(context, coloringStyle)
    val strategy = colorFactory.createColoringStrategy(this)
    return this.map { point ->
        MappableLocation(
            point.id,
            point.coordinate,
            strategy.getColor(point) ?: Color.TRANSPARENT,
            null,
            point.elevation
        )
    }
}

fun List<PathPoint>.asBeacons(
    context: Context,
    coloringStyle: PathPointColoringStyle = PathPointColoringStyle.None,
    selected: Long? = null
): List<Beacon> {
    val colorFactory = getPointFactory(context, coloringStyle)
    val baseStrategy = colorFactory.createColoringStrategy(this)
    val strategy = if (selected != null) {
        SelectedPointDecorator(
            selected,
            baseStrategy,
            NoDrawPointColoringStrategy()
        )
    } else {
        baseStrategy
    }
    return this.map { point ->
        val color = strategy.getColor(point) ?: Color.TRANSPARENT
        Beacon(
            point.id,
            "",
            point.coordinate,
            elevation = point.elevation,
            color = if (point.id == selected && color == Color.TRANSPARENT) {
                Color.WHITE
            } else {
                color
            },
        )
    }
}

fun getLineStyleNames(context: Context): Map<LineStyle, String> {
    return mapOf(
        LineStyle.Solid to context.getString(R.string.solid),
        LineStyle.Dotted to context.getString(R.string.dotted),
        LineStyle.Arrow to context.getString(R.string.arrow),
        LineStyle.Dashed to context.getString(R.string.dashed),
        LineStyle.Square to context.getString(R.string.square),
        LineStyle.Diamond to context.getString(R.string.diamond),
        LineStyle.Cross to context.getString(R.string.cross)
    )
}

fun getPointStyleNames(context: Context): Map<PathPointColoringStyle, String> {
    return mapOf(
        PathPointColoringStyle.None to context.getString(R.string.none),
        PathPointColoringStyle.CellSignal to context.getString(R.string.cell_signal),
        PathPointColoringStyle.Altitude to context.getString(R.string.elevation),
        PathPointColoringStyle.Time to context.getString(R.string.time),
        PathPointColoringStyle.Slope to context.getString(R.string.path_slope)
    )
}

// TODO: Extract this and add solid style
fun getPointFactory(
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

