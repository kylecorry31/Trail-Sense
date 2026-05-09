package com.kylecorry.trail_sense.tools.offline_maps

import android.content.Context
import android.os.Bundle
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.findNavController
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerAttribution
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerDefinition
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceType
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerType
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.persistence.MapRepo
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.OfflineMapFileService
import com.kylecorry.trail_sense.tools.offline_maps.map_layers.MapsforgeTileSource
import com.kylecorry.trail_sense.tools.offline_maps.map_layers.PhotoMapTileSource
import com.kylecorry.trail_sense.tools.offline_maps.quickactions.QuickActionOpenPhotoMap
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIntentHandler
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object OfflineMapsToolRegistration : ToolRegistration {

    private val importMapIntentHandler = ToolIntentHandler { activity, intent ->
        val validTypes = listOf("image/", "application/pdf")
        if (!validTypes.any { intent.type?.startsWith(it) == true }) {
            return@ToolIntentHandler false
        }

        val intentUri = intent.clipData?.getItemAt(0)?.uri ?: return@ToolIntentHandler false
        val bundle = Bundle().apply {
            putParcelable("map_intent_uri", intentUri)
        }
        activity.findNavController()?.navigate(R.id.mapListFragment, bundle)
        true
    }

    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.OFFLINE_MAPS,
            context.getString(R.string.offline_maps),
            R.drawable.photo_maps,
            R.id.mapListFragment,
            ToolCategory.Location,
            context.getString(R.string.offline_maps_summary),
            guideId = R.raw.guide_tool_offline_maps,
            settingsNavAction = R.id.photoMapSettingsFragment,
            quickActions = listOf(
                ToolQuickAction(
                    Tools.QUICK_ACTION_OPEN_PHOTO_MAP,
                    context.getString(R.string.open_photo_map),
                    ::QuickActionOpenPhotoMap
                )
            ),
            additionalNavigationIds = listOf(
                R.id.photoMapsFragment,
                R.id.offlineMapListFragment,
                R.id.offlineMapViewFragment
            ),
            diagnostics = listOf(
                ToolDiagnosticFactory.gps(context),
                ToolDiagnosticFactory.camera(context),
                *ToolDiagnosticFactory.compass(context)
            ).distinctBy { it.id },
            intentHandlers = listOf(importMapIntentHandler),
            singletons = listOf(
                MapRepo::getInstance,
                MapService::getInstance,
                { OfflineMapFileService() }
            ),
            mapLayers = listOf(
                MapLayerDefinition(
                    PhotoMapTileSource.SOURCE_ID,
                    context.getString(R.string.photo_maps),
                    layerType = MapLayerType.Tile,
                    description = context.getString(R.string.map_layer_photo_maps_description),
                    preferences = listOf(
                        MapLayerPreference(
                            id = PhotoMapTileSource.LOAD_PDFS,
                            title = context.getString(R.string.load_pdf_tiles),
                            type = MapLayerPreferenceType.Switch,
                            defaultValue = PhotoMapTileSource.DEFAULT_LOAD_PDFS,
                            summary = context.getString(R.string.load_pdf_tiles_summary)
                        )
                    ),
                    tileSource = ::PhotoMapTileSource,
                    minZoomLevel = 4,
                    cacheKeys = listOf(
                        PhotoMapTileSource.LOAD_PDFS
                    )
                ),
                MapLayerDefinition(
                    MapsforgeTileSource.SOURCE_ID,
                    context.getString(R.string.vector_maps),
                    layerType = MapLayerType.Tile,
                    description = context.getString(R.string.map_layer_vector_maps_description),
                    preferences = listOf(
                        MapLayerPreference(
                            id = "offline_maps",
                            title = context.getString(R.string.manage_maps),
                            type = MapLayerPreferenceType.Label,
                            navActionOnClick = R.id.mapListFragment
                        )
                    ),
                    tileSource = ::MapsforgeTileSource,
                    attributionLoader = {
                        val attributions = MapRepo.getInstance(it)
                            .getVectorMaps()
                            .filter { it.visible }
                            .mapNotNull { it.attribution?.trim()?.takeIf { attribution -> attribution.isNotBlank() } }
                            .distinct()

                        if (attributions.isEmpty()) {
                            null
                        } else {
                            MapLayerAttribution(
                                attributions.joinToString("\n"),
                                alwaysShow = true
                            )
                        }
                    }
                )
            )
        )
    }

    const val PHOTO_MAPS_ID = "photo_maps"
}
