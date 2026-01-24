package com.kylecorry.trail_sense.tools.map.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.graphics.createBitmap
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.ILayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class MapViewV2(context: Context, attrs: AttributeSet? = null) : WebView(context, attrs) {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var isMapReady = false
    private val tileSources = mutableMapOf<String, TileSource>()
    private val pendingLayers = mutableListOf<ILayer>()
    private var pendingClearLayers = false
    private var pendingCenter: Triple<Double, Double, Float>? = null
    private val tileCache = android.util.LruCache<String, ByteArray>(100) // Cache up to 100 tiles

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
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                setupMapInterface()
                isMapReady = true

                // Process any pending operations
                if (pendingClearLayers) {
                    clearLayers()
                    pendingClearLayers = false
                }

                if (pendingLayers.isNotEmpty()) {
                    setLayers(pendingLayers)
                    pendingLayers.clear()
                }

                pendingCenter?.let { (lat, lon, zoom) ->
                    setCenter(lat, lon, zoom)
                    pendingCenter = null
                }
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

    private fun setupMapInterface() {
        evaluateJavascript(
            """
            window.mapReadyCallback = function() {
                // Map is ready, notify Android if needed
            };
            """, null
        )
    }

    fun clearLayers() {
        if (isMapReady) {
            evaluateJavascript("window.MapInterface.clearLayers();", null)
        } else {
            pendingClearLayers = true
        }
    }

    fun setLayers(layers: List<ILayer>) {
        if (isMapReady) {
            // Clear existing layers
            evaluateJavascript("window.MapInterface.clearLayers();", null)
            // Add new layers in order
            layers.filterIsInstance<TileMapLayer<*>>().forEach { layer ->
                tileSources[layer.layerId] = layer.source
                val url = "https://trailsense.app/tiles/${layer.layerId}/{z}/{x}/{y}.png"
                evaluateJavascript(
                    "window.MapInterface.addTileLayer('${layer.layerId}', '$url', ${layer.percentOpacity});",
                    null
                )
            }
        } else {
            // Queue the operation
            pendingLayers.clear()
            pendingLayers.addAll(layers)
        }
    }

    fun setCenter(latitude: Double, longitude: Double, zoom: Float = 10f) {
        if (isMapReady) {
            evaluateJavascript("window.MapInterface.setCenter($longitude, $latitude, $zoom);", null)
        } else {
            pendingCenter = Triple(latitude, longitude, zoom)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        coroutineScope.launch(Dispatchers.IO) {
            tileSources.values.forEach { it.cleanup() }
        }
    }

    @SuppressLint("WrongThread")
    private fun loadTile(url: String, source: TileSource): WebResourceResponse? {
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

    @SuppressLint("WrongThread")
    private fun handleTileRequest(url: String): WebResourceResponse? {
        // Extract layer ID from URL: https://trailsense.app/tiles/{layerId}/{z}/{x}/{y}.png
        val parts = url.split("/")
        if (parts.size < 6) return null

        val layerId = parts[4] // tiles/{layerId}/...
        val source = tileSources[layerId] ?: return null

        return loadTile(url, source)
    }

}