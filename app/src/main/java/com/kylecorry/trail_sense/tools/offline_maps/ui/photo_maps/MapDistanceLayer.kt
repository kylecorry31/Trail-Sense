package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IAsyncLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IDraggableLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.toCoordinate
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.toPixel
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconIcon
import com.kylecorry.trail_sense.tools.navigation.ui.DrawerBitmapLoader
import com.kylecorry.trail_sense.tools.navigation.ui.markers.BitmapMapMarker
import com.kylecorry.trail_sense.tools.navigation.ui.markers.CircleMapMarker
import com.kylecorry.trail_sense.tools.navigation.ui.markers.MapMarker
import com.kylecorry.trail_sense.tools.paths.domain.PathEditState
import com.kylecorry.trail_sense.tools.paths.domain.PathEditor
import kotlin.math.hypot

class MapDistanceLayer : IAsyncLayer, IDraggableLayer {

    /** Called when the path is committed (not on every frame of a drag). */
    var onPathChanged: (points: List<Coordinate>) -> Unit = {}

    /** Called as a point is dragged. */
    var onPathDragged: (points: List<Coordinate>) -> Unit = {}

    /** Called when the selection or undo/redo availability may have changed. */
    var onEditStateChanged: (state: PathEditState) -> Unit = {}

    private val editor = PathEditor()
    private var updateListener: (() -> Unit)? = null
    private var drag: Drag? = null
    private var bitmapLoader: DrawerBitmapLoader? = null

    val editState: PathEditState
        get() = editor.state

    override val isMagnifierVisible: Boolean
        get() = drag?.hasMoved == true

    var isEnabled = true
        set(value) {
            field = value
            clear()
        }

    fun add(location: Coordinate) {
        editor.add(location)
        commit()
    }

    fun undo() {
        if (editor.state.canUndo) {
            editor.undo()
            commit()
        }
    }

    fun redo() {
        if (editor.state.canRedo) {
            editor.redo()
            commit()
        }
    }

    fun clear() {
        editor.clear()
        drag = null
        commit()
    }

    fun getPoints(): List<Coordinate> {
        return editor.points
    }

    private fun commit() {
        onPathChanged(editor.points.toList())
        onEditStateChanged(editor.state)
        updateListener?.invoke()
    }

    override val layerId: String = LAYER_ID

    override fun setPreferences(preferences: Bundle) {
        // Do nothing
    }

    override fun setHasUpdateListener(listener: (() -> Unit)?) {
        updateListener = listener
    }

    override fun draw(context: Context, drawer: ICanvasDrawer, map: IMapView) {
        val points = editor.points
        if (!isEnabled || points.isEmpty()) {
            return
        }

        val pixels = points.map { map.toPixel(it) }

        drawLine(drawer, pixels)
        drawPoints(drawer, map, pixels)
    }

    private fun drawLine(drawer: ICanvasDrawer, pixels: List<PixelCoordinate>) {
        drawer.noFill()
        drawer.strokeWeight(drawer.dp(LINE_WIDTH_DP + LINE_OUTLINE_DP))
        drawer.stroke(Color.WHITE)
        drawSegments(drawer, pixels)
        drawer.strokeWeight(drawer.dp(LINE_WIDTH_DP))
        drawer.stroke(PATH_COLOR)
        drawSegments(drawer, pixels)
    }

    private fun drawSegments(drawer: ICanvasDrawer, pixels: List<PixelCoordinate>) {
        for (i in 0 until pixels.size - 1) {
            drawer.line(pixels[i].x, pixels[i].y, pixels[i + 1].x, pixels[i + 1].y)
        }
    }

    private fun drawPoints(drawer: ICanvasDrawer, map: IMapView, pixels: List<PixelCoordinate>) {
        val loader = bitmapLoader ?: DrawerBitmapLoader(drawer).also { bitmapLoader = it }
        val selected = editor.selectedIndex
        val last = pixels.lastIndex
        val rotation = map.mapAzimuth + map.mapRotation

        fun draw(marker: MapMarker, index: Int) {
            marker.draw(drawer, pixels[index], map.layerScale, rotation, map.resolutionPixels)
        }

        // The end is drawn last so it is never hidden by another point
        (pixels.indices.filter { it != last } + last).forEach { index ->
            val sizeScale = if (index == selected) SELECTED_SCALE else 1f
            if (index == selected) {
                draw(circle(PATH_COLOR, null, HALO_SIZE_DP, 0f, HALO_OPACITY), index)
            }

            if (index == last && last > 0) {
                draw(circle(Color.WHITE, PATH_COLOR, END_SIZE_DP * sizeScale, OUTLINE_DP), index)
                val iconSize = END_ICON_SIZE_DP * sizeScale
                val flag = loader.load(BeaconIcon.Flag.icon, (drawer.dp(iconSize) * 2).toInt())
                draw(BitmapMapMarker(null, flag, iconSize, tint = PATH_COLOR), index)
            } else {
                draw(circle(PATH_COLOR, Color.WHITE, POINT_SIZE_DP * sizeScale, OUTLINE_DP), index)
            }
        }
    }

    private fun circle(
        color: Int,
        strokeColor: Int?,
        size: Float,
        strokeWeight: Float,
        opacity: Int = 255
    ): MapMarker {
        return CircleMapMarker(null, color, strokeColor, opacity, size, strokeWeight)
    }

    override fun drawOverlay(
        context: Context,
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        // Do nothing
    }

    override fun invalidate() {
        // Do nothing
    }

    override fun onClick(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate): Boolean {
        if (!isEnabled) {
            return false
        }

        val pixels = editor.points.map { map.toPixel(it) }

        val point = findPoint(drawer, pixels, pixel)
        if (point != null) {
            add(editor.points[point])
            return true
        }

        add(map.toCoordinate(pixel))
        return true
    }

    override fun onLongPress(
        drawer: ICanvasDrawer,
        map: IMapView,
        pixel: PixelCoordinate
    ): Boolean {
        if (!isEnabled) {
            return false
        }

        val point = findPoint(drawer, editor.points.map { map.toPixel(it) }, pixel) ?: return false
        editor.select(if (editor.selectedIndex == point) null else point)
        onEditStateChanged(editor.state)
        updateListener?.invoke()
        return true
    }

    override fun onDragStart(
        drawer: ICanvasDrawer,
        map: IMapView,
        pixel: PixelCoordinate
    ): Boolean {
        if (!isEnabled) {
            return false
        }

        val point = findPoint(drawer, editor.points.map { map.toPixel(it) }, pixel) ?: return false
        drag = Drag(pixel, point)
        return true
    }

    override fun onDrag(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate) {
        val drag = drag ?: return
        if (!drag.hasMoved) {
            if (distance(drag.startPixel, pixel) < drawer.dp(DRAG_SLOP_DP)) {
                return
            }
            drag.hasMoved = true
            editor.startMove()
        }

        editor.move(drag.pointIndex, map.toCoordinate(pixel))
        onPathDragged(editor.points.toList())
        updateListener?.invoke()
    }

    override fun onDragEnd(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate) {
        val drag = drag ?: return
        this.drag = null

        if (drag.hasMoved) {
            commit()
        }
    }

    /** The point under the touch. The most recently added wins when points share a location. */
    private fun findPoint(
        drawer: ICanvasDrawer,
        pixels: List<PixelCoordinate>,
        pixel: PixelCoordinate
    ): Int? {
        val touchRadius = drawer.dp(TOUCH_RADIUS_DP)
        return pixels.indices.reversed()
            .filter { distance(pixels[it], pixel) <= touchRadius }
            .minByOrNull { distance(pixels[it], pixel) }
    }

    private fun distance(a: PixelCoordinate, b: PixelCoordinate): Float {
        return hypot(a.x - b.x, a.y - b.y)
    }

    override var percentOpacity: Float = 1f

    private class Drag(val startPixel: PixelCoordinate, val pointIndex: Int) {
        var hasMoved = false
    }

    companion object {
        const val LAYER_ID = "map_distance"

        private const val PATH_COLOR = Color.BLACK

        private const val LINE_WIDTH_DP = 2.25f
        private const val LINE_OUTLINE_DP = 2f
        private const val OUTLINE_DP = 1.5f
        private const val POINT_SIZE_DP = 12f
        private const val END_SIZE_DP = 16f
        private const val END_ICON_SIZE_DP = 10f
        private const val SELECTED_SCALE = 1.3f
        private const val HALO_SIZE_DP = 32f
        private const val HALO_OPACITY = 70
        private const val TOUCH_RADIUS_DP = 24f
        private const val DRAG_SLOP_DP = 8f
    }
}
