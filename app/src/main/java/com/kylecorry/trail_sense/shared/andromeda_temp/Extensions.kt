package com.kylecorry.trail_sense.shared.andromeda_temp

import android.graphics.Bitmap
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.subscriptions.ISubscription
import com.kylecorry.andromeda.core.ui.ReactiveComponent
import com.kylecorry.andromeda.fragments.observeFlow
import com.kylecorry.andromeda.fragments.useBackgroundEffect
import com.kylecorry.luna.coroutines.ParallelCoroutineRunner
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext
import kotlin.math.ceil
import kotlin.math.floor

fun Fragment.observe(
    subscription: ISubscription,
    state: BackgroundMinimumState = BackgroundMinimumState.Any,
    collectOn: CoroutineContext = Dispatchers.Default,
    observeOn: CoroutineContext = Dispatchers.Main,
    listener: suspend () -> Unit
) {
    observeFlow(subscription.flow(), state, collectOn, observeOn) { listener() }
}

fun <T> Fragment.observe(
    subscription: com.kylecorry.andromeda.core.subscriptions.generic.ISubscription<T>,
    state: BackgroundMinimumState = BackgroundMinimumState.Any,
    collectOn: CoroutineContext = Dispatchers.Default,
    observeOn: CoroutineContext = Dispatchers.Main,
    listener: suspend (T) -> Unit
) {
    observeFlow(subscription.flow(), state, collectOn, observeOn, listener)
}

fun <T> ReactiveComponent.useBackgroundMemo2(
    vararg values: Any?,
    state: BackgroundMinimumState = BackgroundMinimumState.Resumed,
    cancelWhenBelowState: Boolean = true,
    cancelWhenRerun: Boolean = false,
    block: suspend CoroutineScope.() -> T
): T? {
    val (currentState, setCurrentState) = useState<T?>(null)

    useBackgroundEffect(
        *values,
        state = state,
        cancelWhenBelowState = cancelWhenBelowState,
        cancelWhenRerun = cancelWhenRerun
    ) {
        setCurrentState(block())
    }

    return currentState
}

fun Bitmap.getPixels(): IntArray {
    val pixels = IntArray(this.width * this.height)
    this.getPixels(pixels, 0, this.width, 0, 0, this.width, this.height)
    return pixels
}

fun Bitmap.setPixels(pixels: IntArray) {
    this.setPixels(pixels, 0, this.width, 0, 0, this.width, this.height)
}

suspend inline fun parallelForEachIndex(
    size: Int,
    maxParallel: Int = Runtime.getRuntime().availableProcessors(),
    crossinline action: (Int) -> Unit
) {
    val parallelRunner = ParallelCoroutineRunner(maxParallel)
    val chunkSize = size / maxParallel + 1

    val coroutines = mutableListOf<suspend () -> Unit>()
    for (t in 0 until maxParallel) {
        val start = t * chunkSize
        val end = minOf(start + chunkSize, size)
        if (start >= end) {
            continue
        }
        coroutines.add {
            for (i in start until end) {
                action(i)
            }
        }
    }

    parallelRunner.run(coroutines)
}

suspend inline fun <reified T> parallelReduceIndex(
    size: Int,
    initialValue: T,
    maxParallel: Int = Runtime.getRuntime().availableProcessors(),
    crossinline reducer: (T, Int) -> T,
    crossinline combiner: (T, T) -> T
): T {
    val parallelRunner = ParallelCoroutineRunner(maxParallel)
    val chunkSize = size / maxParallel + 1

    val coroutines = mutableListOf<suspend () -> Unit>()
    val results = Array<T?>(maxParallel) { null }
    for (t in 0 until maxParallel) {
        val start = t * chunkSize
        val end = minOf(start + chunkSize, size)
        if (start >= end) {
            continue
        }
        coroutines.add {
            var acc = initialValue
            for (i in start until end) {
                acc = reducer(acc, i)
            }
            results[t] = acc
        }
    }

    parallelRunner.run(coroutines)
    var finalAcc = initialValue
    for (res in results) {
        if (res != null) {
            finalAcc = combiner(finalAcc, res)
        }
    }
    return finalAcc
}

suspend inline fun <reified T> Bitmap.reducePixels(
    initialValue: T,
    crossinline operation: (acc: T, pixel: Int) -> T,
    crossinline combiner: (a: T, b: T) -> T
): T {
    val pixels = getPixels()
    return parallelReduceIndex(pixels.size, initialValue, reducer = { acc, i ->
        operation(acc, pixels[i])
    }, combiner = combiner)
}

fun IntArray.get(x: Int, y: Int, width: Int): Int {
    return this[y * width + x]
}

fun IntArray.set(x: Int, y: Int, width: Int, value: Int) {
    this[y * width + x] = value
}

fun CoordinateBounds.grid(resolution: Double): List<Coordinate> {
    val latitudes = Interpolation.getMultiplesBetween(
        south - resolution,
        north + resolution,
        resolution
    )

    val longitudes = Interpolation.getMultiplesBetween(
        west - resolution,
        (if (west < east) east else east + 360) + resolution,
        resolution
    )

    val points = mutableListOf<Coordinate>()
    for (lat in latitudes) {
        for (lon in longitudes) {
            points.add(Coordinate(lat, lon))
        }
    }
    return points
}

fun Interpolation.getMultiplesBetween2(
    start: Double,
    end: Double,
    multiple: Double
): DoubleArray {
    val startMultiple = ceil(start / multiple).toInt()
    val endMultiple = floor(end / multiple).toInt()
    val size = endMultiple - startMultiple + 1
    if (size <= 0) return DoubleArray(0)

    val result = DoubleArray(size)
    var value = startMultiple * multiple
    for (i in 0 until size) {
        result[i] = value
        value += multiple
    }
    return result
}