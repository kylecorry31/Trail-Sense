package com.kylecorry.trail_sense.tools.map.infrastructure

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.map.domain.OfflineMapFile
import com.kylecorry.trail_sense.tools.map.domain.OfflineMapFileType
import org.mapsforge.core.model.Tile
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.rendertheme.AssetsRenderTheme
import org.mapsforge.map.datastore.MultiMapDataStore
import org.mapsforge.map.layer.cache.InMemoryTileCache
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.labels.TileBasedLabelStore
import org.mapsforge.map.layer.renderer.DatabaseRenderer
import org.mapsforge.map.layer.renderer.RendererJob
import org.mapsforge.map.model.DisplayModel
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.XmlRenderTheme
import org.mapsforge.map.rendertheme.rule.RenderThemeFuture

class MapsforgeTileRenderer {
    private var selectedMapKey: String? = null
    private var mapDataStore: MultiMapDataStore? = null
    private var renderer: DatabaseRenderer? = null
    private var tileCache: TileCache? = null
    private var renderThemeFuture: RenderThemeFuture? = null
    private val displayModel = DisplayModel().apply {
        setFixedTileSize(TILE_SIZE)
    }

    private val files = getAppService<FileSubsystem>()

    @Synchronized
    fun render(
        context: Context,
        maps: List<OfflineMapFile>,
        tile: com.kylecorry.trail_sense.shared.map_layers.tiles.Tile,
        highDetailMode: Boolean
    ): Bitmap? {
        val renderer = getRenderer(context, maps, highDetailMode) ?: return null
        val currentMapDataStore = mapDataStore ?: return null
        val currentRenderThemeFuture = renderThemeFuture ?: return null

        val tile = Tile(tile.x, tile.y, tile.z.toByte(), tile.size.width)
        if (!currentMapDataStore.supportsTile(tile)) {
            return null
        }

        val job = RendererJob(
            tile,
            currentMapDataStore,
            currentRenderThemeFuture,
            displayModel,
            if (highDetailMode) (1.5f / Resources.sp(context, 1f)).coerceIn(0.25f, 1f) else 1f,
            true,
            false
        )

        val tileBitmap = renderer.executeJob(job) ?: return null
        return try {
            Bitmap.createBitmap(AndroidGraphicFactory.getBitmap(tileBitmap))
        } finally {
            tileBitmap.decrementRefCount()
        }
    }

    @Synchronized
    fun clear() {
        renderer = null
        tileCache?.destroy()
        tileCache = null
        renderThemeFuture?.decrementRefCount()
        renderThemeFuture = null
        mapDataStore?.close()
        mapDataStore = null
        selectedMapKey = null
    }

    private fun getRenderer(
        context: Context,
        maps: List<OfflineMapFile>,
        highDetailMode: Boolean
    ): DatabaseRenderer? {
        val files = maps
            .filter { it.type == OfflineMapFileType.Mapsforge }
            .map { files.get(it.path) }
            .filter { it.isFile && it.length() > 0 }
        if (files.isEmpty()) {
            clear()
            return null
        }

        val key = files.joinToString("|") { it.absolutePath }
        if (renderer != null && selectedMapKey == key) {
            return renderer
        }

        clear()
        AndroidGraphicFactory.createInstance(context.applicationContext as Application)
        scaleRenderThemeToTileSize(highDetailMode)
        val newMapDataStore = MultiMapDataStore(MultiMapDataStore.DataPolicy.DEDUPLICATE)
        files.forEachIndexed { index, file ->
            newMapDataStore.addMapDataStore(MapFile(file), index == 0, index == 0)
        }
        val newRenderThemeFuture = RenderThemeFuture(
            AndroidGraphicFactory.INSTANCE,
            createRenderTheme(context),
            displayModel
        )
        newRenderThemeFuture.run()
        val newTileCache = InMemoryTileCache(100)
        val newRenderer = DatabaseRenderer(
            newMapDataStore,
            AndroidGraphicFactory.INSTANCE,
            newTileCache,
            TileBasedLabelStore(100),
            true,
            false,
            null
        )

        selectedMapKey = key
        mapDataStore = newMapDataStore
        tileCache = newTileCache
        renderThemeFuture = newRenderThemeFuture
        renderer = newRenderer
        return newRenderer
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
        return AssetsRenderTheme(context.applicationContext.assets, "", MAPSFORGE_THEME)
    }

    companion object {
        private const val TILE_SIZE = 256
        private const val MAPSFORGE_THEME = "mapsforge/trail_sense_outdoors.xml"
    }
}
