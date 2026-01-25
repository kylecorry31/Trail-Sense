package com.kylecorry.trail_sense.tools.map.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.graphics.createBitmap
import com.kylecorry.andromeda.geojson.GeoJsonConvert
import com.kylecorry.andromeda.json.JsonConvert
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.map_layers.MapViewLayerManager
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentLinkedQueue


data class JSTileLayer(val url: String, val opacity: Float = 1f) {
    val type = "tile"
}

data class JSGeoJSONLayer(
    val url: String,
    val refreshOnZoom: Boolean = false,
    val opacity: Float = 1.0f
) {
    val type = "geojson"
}

interface JSBridge {
    fun onZoomChanged(zoom: Float)
    fun onCenterChanged(lat: Double, lon: Double)
    fun onBoundsChanged(
        north: Double,
        south: Double,
        east: Double,
        west: Double
    )

    fun onSingleClick(lat: Double, lon: Double)
    fun onLongClick(lat: Double, lon: Double)
}

class MapViewV2(context: Context, attrs: AttributeSet? = null) : WebView(context, attrs) {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val tileSources = mutableMapOf<String, TileSource>()
    private val tileCache = android.util.LruCache<String, ByteArray>(100) // Cache up to 100 tiles

    private val commandQueue = ConcurrentLinkedQueue<String>()
    private var isReady = false
    var zoomLevel: Float = 16f
    var mapCenter: Coordinate = Coordinate.zero

    var bounds: CoordinateBounds = CoordinateBounds.empty

    val layerManager = MapViewLayerManager {
        // TODO: Invalidate the layer
    }

    private val transparentTile by lazy {
        val bitmap = createBitmap(256, 256)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()
        bitmap.recycle()
        byteArray
    }

    init {
        setupWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        settings.javaScriptEnabled = true
        // TODO: Address deprecation
        settings.allowUniversalAccessFromFileURLs = true

        addJavascriptInterface(object : JSBridge {
            @JavascriptInterface
            override fun onZoomChanged(zoom: Float) {
                zoomLevel = zoom
            }

            @JavascriptInterface
            override fun onCenterChanged(lat: Double, lon: Double) {
                mapCenter = Coordinate(lat, lon)
            }

            @JavascriptInterface
            override fun onBoundsChanged(
                north: Double,
                south: Double,
                east: Double,
                west: Double
            ) {
                bounds = CoordinateBounds(north, east, south, west)
            }

            @JavascriptInterface
            override fun onSingleClick(lat: Double, lon: Double) {
                val coordinate = Coordinate(lat, lon)
            }

            @JavascriptInterface
            override fun onLongClick(lat: Double, lon: Double) {
                val coordinate = Coordinate(lat, lon)
            }
        }, "AndroidMap")

        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    android.util.Log.d(
                        "MapViewV2",
                        "JS Console: ${it.message()} -- From line ${it.lineNumber()} of ${it.sourceId()}"
                    )
                }
                return true
            }
        }

        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url?.toString() ?: return null
                if (url.startsWith("https://trailsense.app/tiles/")) {
                    return handleTileRequest(url)
                } else if (url.startsWith("https://trailsense.app/geojson/")) {
                    return handleGeoJsonRequest(url)
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isReady = true
                processCommandQueue()
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                android.util.Log.e("MapViewV2", "WebView Error: ${error?.description}")
                super.onReceivedError(view, request, error)
            }
        }

        // Load the HTML file with inlined assets to avoid file access issues
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val html = readAsset("map_view.html")
                val css = readAsset("ol.css")
                val js = readAsset("ol.js")

                val inlinedHtml = html
                    .replace(
                        "<link rel=\"stylesheet\" href=\"file:///android_asset/ol.css\">",
                        "<style>$css</style>"
                    )
                    .replace(
                        "<script src=\"file:///android_asset/ol.js\"></script>",
                        "<script>$js</script>"
                    )

                onMain {
                    loadDataWithBaseURL(
                        "file:///android_asset/",
                        inlinedHtml,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("MapViewV2", "Error loading assets", e)
                e.printStackTrace()
            }
        }
    }

    private fun readAsset(fileName: String): String {
        return context.assets.open(fileName).bufferedReader().use { it.readText() }
    }


    fun setCenter(latitude: Double, longitude: Double) {
        runCommand("window.MapInterface.setCenter($latitude, $longitude);")
    }

    fun setZoom(zoom: Float) {
        runCommand("window.MapInterface.setZoom($zoom);")
    }

    fun start() {
        updateMapLayers()
        layerManager.start()
    }

    fun stop() {
        layerManager.stop()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        coroutineScope.launch(Dispatchers.IO) {
            tileSources.values.forEach { it.cleanup() }
        }
    }

    private fun updateMapLayers() {
        val layerObject = layerManager.getLayers().mapNotNull {
            when (it) {
                is TileMapLayer<*> -> {
                    JSTileLayer(
                        "https://trailsense.app/tiles/${it.layerId}/{z}/{x}/{y}.png",
                        it.percentOpacity
                    )
                }

                is GeoJsonLayer<*> -> {
                    JSGeoJSONLayer(
                        "https://trailsense.app/geojson/${it.layerId}.json?bbox={bbox}",
                        it.refreshOnZoom,
                        it.percentOpacity
                    )
                }

                else -> {
                    null
                }
            }
        }

        val layerJson = JsonConvert.toJson(layerObject)
        runCommand("window.MapInterface.setLayers($layerJson)")
    }

    private fun runCommand(command: String) {
        commandQueue.add(command)
        if (isReady) {
            processCommandQueue()
        }
    }

    private fun processCommandQueue() {
        while (commandQueue.isNotEmpty()) {
            val command = commandQueue.poll() ?: continue
            evaluateJavascript(command, null)
        }
    }

    @SuppressLint("WrongThread")
    private fun loadTile(url: String, source: TileSource): WebResourceResponse {
        try {
            val parts = url.split("/")
            // format: https://trailsense.app/tiles/{layerId}/{z}/{x}/{y}.png
            val layerId = parts[4]
            val z = parts[parts.size - 3].toInt()
            val x = parts[parts.size - 2].toInt()
            val y = parts[parts.size - 1].replace(".png", "").toInt()

            val tile = Tile(x, y, z)

            // Check cache
            val cacheKey = "$layerId/$z/$x/$y"
            tileCache.get(cacheKey)?.let {
                return WebResourceResponse(
                    "image/png",
                    "UTF-8",
                    ByteArrayInputStream(it)
                )
            }

            // This needs to be blocking since WebView expects a response immediately
            val bitmap = runBlocking {
                source.loadTile(tile)
            }

            if (bitmap == null) {
                return WebResourceResponse(
                    "image/png",
                    "UTF-8",
                    200,
                    "OK",
                    null,
                    ByteArrayInputStream(transparentTile)
                )
            }

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray = stream.toByteArray()
            bitmap.recycle()

            // Add to cache
            tileCache.put(cacheKey, byteArray)

            return WebResourceResponse(
                "image/png",
                "UTF-8",
                ByteArrayInputStream(byteArray)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return WebResourceResponse(
                "image/png",
                "UTF-8",
                500,
                "Internal Server Error",
                null,
                ByteArrayInputStream(ByteArray(0))
            )
        }
    }

    private fun handleTileRequest(url: String): WebResourceResponse? {
        // Extract layer ID from URL: https://trailsense.app/tiles/{layerId}/{z}/{x}/{y}.png
        val parts = url.split("/")
        if (parts.size < 6) return null

        val layerId = parts[4]
        val layer = layerManager.getLayers().firstOrNull { it.layerId == layerId } ?: return null
        val source = (layer as? TileMapLayer<*>)?.source ?: return null
        return loadTile(url, source)
    }

    private fun handleGeoJsonRequest(url: String): WebResourceResponse? {
        // Extract layer ID from URL: https://trailsense.app/geojson/{layerId}.json?bbox={bbox}
        val parts = url.split("/")
        if (parts.size < 5) return null

        val layerId = parts[4].split("?")[0].replace(".json", "")
        val bbox = url.substringAfter("bbox=", "")
        // Parse bbox into CoordinateBounds
        val bounds = if (bbox.isNotEmpty()) {
            val coords = bbox.split(",").mapNotNull { it.toDoubleOrNull() }
            if (coords.size == 4) {
                CoordinateBounds(
                    north = coords[3],
                    east = coords[2],
                    south = coords[1],
                    west = coords[0]
                )
            } else {
                CoordinateBounds.world
            }
        } else {
            CoordinateBounds.world
        }

        val layer = layerManager.getLayers().firstOrNull { it.layerId == layerId } ?: return null
        val source = (layer as? GeoJsonLayer<*>)?.source ?: return null

        return try {
            val data = runBlocking {
                source.load(bounds, zoomLevel.toInt())
            } ?: return null

            val json = GeoJsonConvert.toJson(data)

            WebResourceResponse(
                "application/json",
                "UTF-8",
                ByteArrayInputStream(json.toByteArray())
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}