package com.kylecorry.trail_sense.shared

import android.os.Bundle
import android.view.View
import android.view.Window
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.signal.CellNetworkQuality
import com.kylecorry.andromeda.signal.ICellSignalSensor
import com.kylecorry.sol.math.SolMath.roundPlaces
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.math.sumOfFloat
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.data.Identifiable
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import kotlin.collections.set
import kotlin.math.roundToInt

fun Fragment.requireMainActivity(): MainActivity {
    return requireActivity() as MainActivity
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

fun PixelCoordinate.rotateInRect(
    angle: Float,
    currentSize: Size,
    newSizeOverride: Size? = null
): PixelCoordinate {
    val vec = Vector2(x, y).rotateInRect(angle, currentSize, newSizeOverride)
    return PixelCoordinate(vec.x, vec.y)
}

fun Vector2.rotateInRect(angle: Float, currentSize: Size, newSizeOverride: Size? = null): Vector2 {
    val newSize = newSizeOverride ?: currentSize.rotate(angle)
    return minus(Vector2(currentSize.width / 2f, currentSize.height / 2f))
        .rotate(angle)
        .plus(Vector2(newSize.width / 2f, newSize.height / 2f))
}

fun View.getViewBounds(rotation: Float = 0f): Rectangle {
    val rectangle = Rectangle(
        0f,
        height.toFloat(),
        width.toFloat(),
        0f,
    )

    if (rotation != 0f) {
        return rectangle.rotate(rotation)
    }

    return rectangle
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

fun Enum<*>.readableName(): String {
    return name.map { if (it.isUpperCase()) " $it" else it }
        .joinToString("").trim()
}

fun ICanvasDrawer.text(str: String, x: Float, y: Float, lineSpacing: Float) {
    val lines = str.split("\n")
    var lastHeight = 0f
    var lastY = y
    lines.forEachIndexed { index, line ->
        val newY = lastY + lastHeight + if (index == 0) 0f else lineSpacing
        lastY = newY
        lastHeight = textHeight(line)
        text(line, x, newY)
    }
}

fun ICanvasDrawer.textDimensions(str: String, lineSpacing: Float): Pair<Float, Float> {
    val lines = str.split("\n")
    val totalTextHeight = lines.sumOfFloat { textHeight(it) }
    val totalSpacing = lineSpacing * (lines.size - 1)
    val maxWidth = lines.maxOfOrNull { textWidth(it) } ?: 0f
    return maxWidth to totalTextHeight + totalSpacing
}

fun NavController.navigateWithAnimation(@IdRes resId: Int, args: Bundle? = null) {
    try {
        val options = NavOptions.Builder()
            .setEnterAnim(R.anim.slide_in_right)
            .setExitAnim(R.anim.slide_out_left)
            .setPopEnterAnim(R.anim.slide_in_left)
            .setPopExitAnim(R.anim.slide_out_right)
            .build()
        navigate(resId, args, options)
    } catch (e: Exception) {
        // If for some reason the animation fails, just navigate without it
        navigate(resId, args)
    }
}

inline fun List<Float>.forEachLine(action: (x1: Float, y1: Float, x2: Float, y2: Float) -> Unit) {
    for (i in indices step 4) {
        action(this[i], this[i + 1], this[i + 2], this[i + 3])
    }
}

/**
 * Sets the navigation bar color. On SDK < 26, this does nothing because the foreground color is not customizable.
 */
fun Window.setNavigationBarColorCompat(@ColorInt color: Int) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        navigationBarColor = color
    }
}

fun Float.safeRoundToInt(default: Int = 0): Int {
    return tryOrDefault(default) {
        if (isNaN() || isInfinite()) {
            default
        } else {
            roundToInt()
        }
    }
}

fun Double.safeRoundToInt(default: Int = 0): Int {
    return tryOrDefault(default) {
        if (isNaN() || isInfinite()) {
            default
        } else {
            roundToInt()
        }
    }
}

fun <T> List<T>.padRight(minLength: Int, value: T): List<T> {
    return if (size >= minLength) {
        this
    } else {
        this + List(minLength - size) { value }
    }
}