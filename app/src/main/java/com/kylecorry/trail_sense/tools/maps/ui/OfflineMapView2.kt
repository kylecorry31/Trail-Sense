package com.kylecorry.trail_sense.tools.maps.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.util.AttributeSet
import androidx.core.net.toUri
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.kylecorry.andromeda.canvas.CanvasDrawer
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.cache.ObjectPool
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.files.LocalFiles
import com.kylecorry.sol.science.geology.GeologyService
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.paths.ui.drawing.PathLineDrawerFactory
import com.kylecorry.trail_sense.navigation.paths.ui.drawing.RenderedPath
import com.kylecorry.trail_sense.navigation.paths.ui.drawing.RenderedPathFactory
import com.kylecorry.trail_sense.navigation.ui.IMappableLocation
import com.kylecorry.trail_sense.navigation.ui.IMappablePath
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.toPixel
import com.kylecorry.trail_sense.tools.maps.domain.Map
import kotlin.math.max
import kotlin.math.min


class OfflineMapView2 : SubsamplingScaleImageView {

    private lateinit var drawer: ICanvasDrawer
    private var isSetup = false
    private var myLocation: Coordinate? = null
    private var map: Map? = null
    private val geology = GeologyService()
    private var azimuth = 0f
    private var locations = emptyList<IMappableLocation>()
    private var paths = emptyList<IMappablePath>()
    private var pathPool = ObjectPool { Path() }
    private var renderedPaths = mapOf<Long, RenderedPath>()
    private var pathsRendered = false
    private var lastScale = 1f
    private var highlightedLocation: IMappableLocation? = null

    private val layerScale: Float
        get() = min(1f, max(scale, 0.9f))

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (!isReady || canvas == null) {
            return
        }

        if (!isSetup) {
            drawer = CanvasDrawer(context, canvas)
            setup()
            isSetup = true
        }

        drawer.canvas = canvas
        draw()
    }

    fun setup() {
        setPanLimit(PAN_LIMIT_OUTSIDE)
    }

    fun draw() {
        map ?: return

        if (scale != lastScale){
            pathsRendered = false
            lastScale = scale
        }

        drawPaths()
        drawLocations()
        drawMyLocation()
    }

    fun showMap(map: Map) {
        val file = LocalFiles.getFile(context, map.filename, false)
        setImage(ImageSource.uri(file.toUri()))
        this.map = map
    }

    fun setMyLocation(coordinate: Coordinate?) {
        myLocation = coordinate
        invalidate()
    }

    private fun drawMyLocation() {
        val scale = layerScale
        val location = myLocation ?: return
        val pixels = getPixelCoordinate(location) ?: return

        drawer.stroke(Color.WHITE)
        drawer.strokeWeight(drawer.dp(1f) / scale)
        drawer.fill(AppColor.Orange.color)
        drawer.push()
        drawer.rotate(azimuth, pixels.x, pixels.y)
        drawer.triangle(
            pixels.x, pixels.y - drawer.dp(6f) / scale,
            pixels.x - drawer.dp(5f) / scale, pixels.y + drawer.dp(6f) / scale,
            pixels.x + drawer.dp(5f) / scale, pixels.y + drawer.dp(6f) / scale
        )
        drawer.pop()
    }

    fun setAzimuth(azimuth: Float) {
        this.azimuth = azimuth
        invalidate()
    }

    fun showLocations(locations: List<IMappableLocation>) {
        this.locations = locations
        invalidate()
    }

    fun showPaths(paths: List<IMappablePath>) {
        this.paths = paths
        pathsRendered = false
        invalidate()
    }

    private fun generatePaths(paths: List<IMappablePath>): kotlin.collections.Map<Long, RenderedPath> {
        val metersPerPixel =
            map?.distancePerPixel(sWidth * scale, sHeight * scale)?.meters()?.distance ?: 1f
        val factory = RenderedPathFactory(metersPerPixel, null, 0f, true)
        val map = mutableMapOf<Long, RenderedPath>()
        for (path in paths) {
            val pathObj = pathPool.get()
            pathObj.reset()
            map[path.id] = factory.render(path.points.map { it.coordinate }, pathObj)
        }
        return map
    }

    private fun drawPaths() {
        val scale = layerScale
        if (!pathsRendered) {
            for (path in renderedPaths) {
                pathPool.release(path.value.path)
            }
            renderedPaths = generatePaths(paths)
            pathsRendered = true
        }

        val factory = PathLineDrawerFactory()
        drawer.push()
//        clip(compassPath)
        for (path in paths) {
            val rendered = renderedPaths[path.id] ?: continue
            val lineDrawer = factory.create(path.style)
            val centerPixel = getPixelCoordinate(rendered.origin, false) ?: continue
            drawer.push()
            drawer.translate(centerPixel.x, centerPixel.y)
            lineDrawer.draw(drawer, path.color, strokeScale = scale) {
                path(rendered.path)
            }
            drawer.pop()
        }
        drawer.pop()
        drawer.noStroke()
        drawer.fill(Color.WHITE)
        drawer.noPathEffect()
    }

    fun highlightLocation(location: IMappableLocation?){
        this.highlightedLocation = location
        invalidate()
    }

    private fun drawLocations() {
        val highlighted = highlightedLocation

        val gpsLocation = myLocation
        if (highlighted != null && gpsLocation != null){
            val start = getPixelCoordinate(gpsLocation, false)
            val end = getPixelCoordinate(highlighted.coordinate, false)

            if (start != null && end != null){
                drawer.noFill()
                drawer.stroke(highlighted.color)
                drawer.strokeWeight(drawer.dp(4f) / layerScale)
                drawer.opacity(127)
                drawer.line(start.x, start.y, end.x, end.y)
            }
        }

        var containsHighlighted = false
        for (location in locations) {
            if (location.id == highlighted?.id){
                containsHighlighted = true
            }
            drawLocation(location, location.id == highlighted?.id || highlighted == null)
        }

        if (highlighted != null && !containsHighlighted){
            drawLocation(highlighted, true)
        }

        drawer.opacity(255)
    }

    fun recenter(){
        resetScaleAndCenter()
    }

    private fun drawLocation(location: IMappableLocation, highlighted: Boolean) {
        val scale = layerScale
        val coord = getPixelCoordinate(location.coordinate)
        if (coord != null) {
            drawer.stroke(Color.WHITE)
            drawer.strokeWeight(drawer.dp(1f) / scale)
            drawer.fill(location.color)
            drawer.opacity(
                if (highlighted) {
                    255
                } else {
                    127
                }
            )
            drawer.circle(coord.x, coord.y, drawer.dp(8f) / scale)
        }
    }


    private fun getPixelCoordinate(
        coordinate: Coordinate,
        nullIfOffMap: Boolean = true
    ): PixelCoordinate? {

        val mapSize = sWidth.toFloat() to sHeight.toFloat()

        val bounds = map?.boundary(mapSize.first, mapSize.second) ?: return null

        val pixels = geology.toMercator(coordinate, bounds, mapSize).toPixel()

        if (nullIfOffMap && (pixels.x < 0 || pixels.x > mapSize.first)) {
            return null
        }

        if (nullIfOffMap && (pixels.y < 0 || pixels.y > mapSize.second)) {
            return null
        }

        val view = sourceToViewCoord(pixels.x, pixels.y)!!

        return PixelCoordinate(view.x, view.y)
    }

}