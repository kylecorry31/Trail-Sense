package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.trail_maps.mapsforge

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath
import com.kylecorry.trail_sense.tools.offline_maps.domain.trail_maps.TrailMap
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mapsforge.core.model.Tile
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.datastore.MapDataStore
import org.mapsforge.map.datastore.MultiMapDataStore
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.labels.TileBasedLabelStore
import org.mapsforge.map.layer.renderer.RendererJob
import org.mapsforge.map.model.DisplayModel
import org.mapsforge.map.rendertheme.StreamRenderTheme
import org.mapsforge.map.rendertheme.XmlRenderTheme
import org.mapsforge.map.rendertheme.rule.RenderThemeFuture

class MapsforgeTileRenderer(
    private val maps: List<TrailMap>,
    private val highDetailMode: Boolean
) {
    @Volatile
    private var rendererHolder: MapsforgeRendererHolder? = null
    private val rendererMutex = Mutex()
    private val displayModel = DisplayModel().apply {
        setFixedTileSize(TileMath.WORLD_TILE_SIZE)
    }

    private val files = getAppService<FileSubsystem>()
    private val prefs = getAppService<UserPreferences>()
    private val formatter = getAppService<FormatService>()

    suspend fun render(
        context: Context,
        tile: com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
    ): Bitmap? {
        val holder = getRenderer(context)
        val tile = Tile(tile.x, tile.y, tile.z.toByte(), tile.size.width)
        if (holder == null || !holder.dataStore.supportsTile(tile)) {
            return null
        }

        val hasTransparentAreas = !holder.dataStore.supportsFullTile(tile)

        val job = RendererJob(
            tile,
            holder.dataStore,
            holder.renderThemeFuture,
            displayModel,
            if (highDetailMode) (1.5f / Resources.sp(context, 1f)).coerceIn(0.25f, 1f) else 1f,
            hasTransparentAreas,
            false
        )

        val tileBitmap = holder.renderer.executeJob(job)
        return tileBitmap?.let { AndroidGraphicFactory.getBitmap(it) }
    }

    suspend fun clear() = rendererMutex.withLock {
        rendererHolder?.destroy()
        rendererHolder = null
    }

    private suspend fun getRenderer(
        context: Context
    ): MapsforgeRendererHolder? {
        return rendererMutex.withLock {
            rendererHolder ?: createRenderer(context, maps, highDetailMode)?.also {
                rendererHolder = it
            }
        }
    }

    private suspend fun createRenderer(
        context: Context,
        maps: List<TrailMap>,
        highDetailMode: Boolean
    ): MapsforgeRendererHolder? {
        val mapFiles = maps
            .mapNotNull { MapsforgeAdapter.open(it.mapFile.path) }
        if (mapFiles.isEmpty()) {
            return null
        }

        AndroidGraphicFactory.createInstance(context.applicationContext as Application)
        scaleRenderThemeToTileSize(highDetailMode)
        val newMapDataStore = MultiMapDataStore(MultiMapDataStore.DataPolicy.DEDUPLICATE)
        mapFiles.forEachIndexed { index, mapFile ->
            newMapDataStore.addMapDataStore(mapFile, index == 0, index == 0)
        }
        val newRenderThemeFuture = RenderThemeFuture(
            AndroidGraphicFactory.INSTANCE,
            createRenderTheme(context),
            displayModel
        )
        newRenderThemeFuture.run()
        val newTileCache = MapsforgeMockTileCache(100)
        val newRenderer = MapsforgeRenderer(
            newMapDataStore,
            AndroidGraphicFactory.INSTANCE,
            newTileCache,
            TileBasedLabelStore(100),
            listOf(
                PeakElevationMapReadResultModifier(prefs.baseDistanceUnits, formatter),
                AreaLabelMapReadResultModifier(
                    mapOf(
                        "boundary" to setOf("protected_area", "national_park", "nature_reserve"),
                        "leisure" to setOf("nature_reserve"),
                        "natural" to setOf("water"),
                        "landuse" to setOf("reservoir", "basin"),
                    ),
                    referenceZoomLevel = 11,
                    minZoom = 13
                )
            )
        )

        return MapsforgeRendererHolder(
            newRenderer,
            newMapDataStore,
            newTileCache,
            newRenderThemeFuture
        )
    }

    private fun scaleRenderThemeToTileSize(highResolutionMode: Boolean) {
        val deviceScaleFactor = DisplayModel.getDeviceScaleFactor()
        displayModel.userScaleFactor = if (!highResolutionMode && deviceScaleFactor > 0f) {
            1f / deviceScaleFactor
        } else {
            1f
        }
    }

    private fun createRenderTheme(context: Context): XmlRenderTheme {
        val xml = runBlocking { files.streamAsset(MAPSFORGE_THEME)!! }
        val theme = StreamRenderTheme("", xml)
        theme.resourceProvider = DrawableResourceProvider(context)
        return theme
    }

    private class MapsforgeRendererHolder(
        val renderer: MapsforgeRenderer,
        val dataStore: MapDataStore,
        private val tileCache: TileCache,
        val renderThemeFuture: RenderThemeFuture,
    ) {
        fun destroy() {
            renderer.interruptAndDestroy()
            tileCache.destroy()
            renderThemeFuture.decrementRefCount()
            dataStore.close()
        }
    }

    companion object {
        private const val MAPSFORGE_THEME = "mapsforge/trail_sense_outdoors.xml"
    }
}
