package com.kylecorry.trail_sense.tools.map.infrastructure

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.map.domain.OfflineMapFile
import com.kylecorry.trail_sense.tools.map.domain.OfflineMapFileType
import org.mapsforge.core.model.Tile
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.datastore.MultiMapDataStore
import org.mapsforge.map.layer.cache.InMemoryTileCache
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.labels.TileBasedLabelStore
import org.mapsforge.map.layer.renderer.DatabaseRenderer
import org.mapsforge.map.layer.renderer.RendererJob
import org.mapsforge.map.model.DisplayModel
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes
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
        tile: com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
    ): Bitmap? {
        val renderer = getRenderer(context, maps) ?: return null
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
            1f,
            false,
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

    private fun getRenderer(context: Context, maps: List<OfflineMapFile>): DatabaseRenderer? {
        val files = maps
            .filter { it.type == OfflineMapFileType.Mapsforge && it.visible }
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
        val newMapDataStore = MultiMapDataStore(MultiMapDataStore.DataPolicy.DEDUPLICATE)
        files.forEachIndexed { index, file ->
            newMapDataStore.addMapDataStore(MapFile(file), index == 0, index == 0)
        }
        val newRenderThemeFuture = RenderThemeFuture(
            AndroidGraphicFactory.INSTANCE,
            MapsforgeThemes.DEFAULT,
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

    companion object {
        private const val TILE_SIZE = 256
    }
}
