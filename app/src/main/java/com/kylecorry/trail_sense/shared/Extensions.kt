package com.kylecorry.trail_sense.shared

import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.andromeda.signal.CellNetworkQuality
import com.kylecorry.andromeda.signal.ICellSignalSensor
import com.kylecorry.sol.math.SolMath.roundPlaces
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import com.kylecorry.trail_sense.shared.database.Identifiable
import kotlin.collections.set

fun Fragment.requireMainActivity(): MainActivity {
    return requireActivity() as MainActivity
}

fun IGPS.getPathPoint(pathId: Long): PathPoint {
    return PathPoint(
        -1,
        pathId,
        location,
        altitude,
        time
    )
}

fun <T : Identifiable> Array<T>.withId(id: Long): T? {
    return firstOrNull { it.id == id }
}

fun <T : Identifiable> Collection<T>.withId(id: Long): T? {
    return firstOrNull { it.id == id }
}

fun GeoUri.Companion.from(beacon: Beacon): GeoUri {
    val params = mutableMapOf(
        "label" to beacon.name
    )
    if (beacon.elevation != null) {
        params["ele"] = beacon.elevation.roundPlaces(2).toString()
    }

    return GeoUri(beacon.coordinate, null, params)
}

val ZERO_SPEED = Speed(0f, DistanceUnits.Meters, TimeUnits.Seconds)

fun ICellSignalSensor.networkQuality(): CellNetworkQuality? {
    val signal = signals.maxByOrNull { it.strength }
    return signal?.let { CellNetworkQuality(it.network, it.quality) }
}

fun PixelCoordinate.toVector2(top: Float): Vector2 {
    return Vector2(x, -(y - top))
}

fun Vector2.toPixelCoordinate(top: Float): PixelCoordinate {
    return PixelCoordinate(x, -(y - top))
}

fun ICanvasDrawer.getBounds(rotation: Float = 0f): Rectangle {
    val rectangle = Rectangle(
        0f,
        canvas.height.toFloat(),
        canvas.width.toFloat(),
        0f,
    )

    if (rotation != 0f) {
        return rectangle.rotate(rotation)
    }

    return rectangle
}