package com.kylecorry.trail_sense.tools.augmented_reality.ui.layers

import android.graphics.Color
import android.os.SystemClock
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.TextAlign
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Vector3
import com.kylecorry.trail_sense.tools.augmented_reality.domain.ground.GroundTrackingState
import com.kylecorry.trail_sense.tools.augmented_reality.domain.ground.GroundTrackingStatus
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.AugmentedRealityCoordinate
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Shows the mapped surroundings: ground tiles at their real height, in cool colors by height
 * relative to the user, and cubes for anything standing on the ground, in warm colors by height
 * above it.
 */
class ARSurroundingsLayer(
    private val searchingText: String,
    private val lostText: String
) : ARLayer {

    @Volatile
    private var state: GroundTrackingState? = null

    private val meshLayer = ARPolygonLayer().also { it.setSource(::mesh) }

    fun setState(state: GroundTrackingState?) {
        this.state = state
    }

    override suspend fun update(drawer: ICanvasDrawer, view: AugmentedRealityView) {
        meshLayer.update(drawer, view)
    }

    // Tracking updates a few times a second, so continue the last movement to hide the delay
    private fun position(state: GroundTrackingState): Vector3? {
        val position = state.position ?: return null
        val elapsed = ((SystemClock.elapsedRealtimeNanos() - state.time) / 1e9f).coerceIn(0f, MAX_EXTRAPOLATION)
        return position + state.velocity * elapsed
    }

    // In each column of voxels the lowest is the ground, drawn as a tile at its own height (so
    // slopes and drops show). Anything clearly above it is drawn as a cube.
    private fun mesh(view: AugmentedRealityView): List<ARPolygon> {
        val state = state ?: return emptyList()
        val grid = state.map
        val groundOffset = state.groundOffset ?: return emptyList()
        val camera = position(state) ?: return emptyList()
        val levelZ = camera.z + groundOffset
        val size = grid.voxelSize
        val inset = size * TILE_GAP
        // Keys: ground band, or height band * 2 + (0 for tops, 1 for sides)
        val tiles = HashMap<Int, MutableList<List<AugmentedRealityCoordinate>>>()
        val faces = HashMap<Int, MutableList<List<AugmentedRealityCoordinate>>>()

        fun coordinate(x: Float, y: Float, z: Float) =
            AugmentedRealityCoordinate(Vector3(x - camera.x, y - camera.y, z - camera.z), view.isTrueNorth)

        fun square(x0: Float, y0: Float, x1: Float, y1: Float, z: Float) =
            listOf(coordinate(x0, y0, z), coordinate(x1, y0, z), coordinate(x1, y1, z), coordinate(x0, y1, z))

        val columns = grid.occupied()
            .map { grid.center(it) }
            .filter { hypot(it.x - camera.x, it.y - camera.y) < MAX_DISTANCE }
            .groupBy { (it.x / size).roundToInt() to (it.y / size).roundToInt() }
            .values
            .sortedBy { column -> hypot(column[0].x - camera.x, column[0].y - camera.y) }
            .take(MAX_COLUMNS)

        for (column in columns) {
            val sorted = column.sortedBy { it.z }
            val ground = sorted.first()
            val x0 = ground.x - size / 2 + inset
            val x1 = ground.x + size / 2 - inset
            val y0 = ground.y - size / 2 + inset
            val y1 = ground.y + size / 2 - inset

            val relative = ground.z - levelZ
            val groundBand = ((relative / MAX_GROUND_COLOR_HEIGHT + 1f) / 2f * GROUND_BANDS).toInt().coerceIn(0, GROUND_BANDS - 1)
            tiles.getOrPut(groundBand) { mutableListOf() }.add(square(x0, y0, x1, y1, ground.z))

            for (center in sorted.drop(1)) {
                val above = center.z - ground.z
                // Right on top of the ground is still the ground
                if (above < size * 1.5f) continue
                val z0 = center.z - size / 2 + inset
                val z1 = center.z + size / 2 - inset
                val band = ((above / MAX_COLOR_HEIGHT) * HEIGHT_BANDS).toInt().coerceIn(0, HEIGHT_BANDS - 1)
                val top = faces.getOrPut(band * 2) { mutableListOf() }
                val sides = faces.getOrPut(band * 2 + 1) { mutableListOf() }
                // Only the faces turned toward the camera
                if (camera.z > z1) top.add(square(x0, y0, x1, y1, z1))
                if (camera.z < z0) top.add(square(x0, y0, x1, y1, z0))
                val faceX = if (camera.x < x0) x0 else if (camera.x > x1) x1 else null
                if (faceX != null) sides.add(listOf(coordinate(faceX, y0, z0), coordinate(faceX, y1, z0), coordinate(faceX, y1, z1), coordinate(faceX, y0, z1)))
                val faceY = if (camera.y < y0) y0 else if (camera.y > y1) y1 else null
                if (faceY != null) sides.add(listOf(coordinate(x0, faceY, z0), coordinate(x1, faceY, z0), coordinate(x1, faceY, z1), coordinate(x0, faceY, z1)))
            }
        }

        return tiles.map { (band, parts) -> ARPolygon(parts, groundColor(band)) } +
                faces.map { (key, parts) -> ARPolygon(parts, voxelColor(key / 2, key % 2 == 1)) }
    }

    // Cool colors for the ground: violet below the user's level, cyan at it, teal above
    private fun groundColor(band: Int): Int {
        val fraction = band / (GROUND_BANDS - 1f)
        val hue = 260f - fraction * 100f
        val color = Color.HSVToColor(floatArrayOf(hue, 0.8f, 1f))
        return Color.argb(GROUND_ALPHA, Color.red(color), Color.green(color), Color.blue(color))
    }

    // Warm colors for things on the ground, yellow low to red at 2 m; sides darker so they read as 3D
    private fun voxelColor(band: Int, isSide: Boolean): Int {
        val hue = 60f * (1f - band / (HEIGHT_BANDS - 1f))
        val color = Color.HSVToColor(floatArrayOf(hue, 0.85f, if (isSide) 0.65f else 1f))
        return Color.argb(VOXEL_ALPHA, Color.red(color), Color.green(color), Color.blue(color))
    }

    override fun draw(drawer: ICanvasDrawer, view: AugmentedRealityView) {
        meshLayer.draw(drawer, view)
        val hint = when (state?.status) {
            GroundTrackingStatus.Searching -> searchingText
            GroundTrackingStatus.Lost -> lostText
            else -> null
        }
        hint?.let { drawHint(drawer, view, it) }
    }

    private fun drawHint(drawer: ICanvasDrawer, view: AugmentedRealityView, text: String) {
        val padding = drawer.dp(8f)
        drawer.textSize(drawer.sp(14f))
        drawer.textMode(TextMode.Corner)
        drawer.textAlign(TextAlign.Center)
        val width = drawer.textWidth(text)
        val height = drawer.textHeight(text)
        val x = view.width / 2f
        val y = drawer.dp(HINT_TOP)
        drawer.noStroke()
        drawer.fill(Color.argb(150, 0, 0, 0))
        drawer.rect(x - width / 2 - padding, y, width + padding * 2, height + padding * 2, drawer.dp(4f))
        drawer.fill(Color.WHITE)
        drawer.text(text, x, y + padding + height)
    }

    override fun invalidate() {
        meshLayer.invalidate()
    }

    override fun onClick(
        drawer: ICanvasDrawer,
        view: AugmentedRealityView,
        pixel: PixelCoordinate
    ) = false

    override fun onFocus(drawer: ICanvasDrawer, view: AugmentedRealityView) = false

    private companion object {
        const val MAX_EXTRAPOLATION = 0.3f
        const val MAX_DISTANCE = 12f
        const val MAX_COLUMNS = 600
        const val TILE_GAP = 0.06f
        const val MAX_COLOR_HEIGHT = 2f
        const val MAX_GROUND_COLOR_HEIGHT = 1.5f
        const val HEIGHT_BANDS = 8
        const val GROUND_BANDS = 9
        const val VOXEL_ALPHA = 120
        const val GROUND_ALPHA = 80
        const val HINT_TOP = 140f
    }
}
