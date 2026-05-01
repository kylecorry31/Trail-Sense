package com.kylecorry.trail_sense.shared.map_layers

import android.content.Context
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.plugins.PluginSubsystem
import com.kylecorry.trail_sense.shared.debugging.isDebug
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerDefinition
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.ILayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.LayerFactory
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class MapLayerLoader(context: Context) {

    private val toolLayers = Tools.getTools(context).flatMap { it.mapLayers }.associateBy { it.id }
    private val factory = LayerFactory()

    private val plugins = getAppService<PluginSubsystem>()

    suspend fun getDefinitions(): Map<String, MapLayerDefinition> {
        val pluginDefinitions: Map<String, MapLayerDefinition> = if (plugins.arePluginsEnabled()) {
            plugins.getPluginResourceServiceDetails().flatMap { it.features.mapLayers }.associateBy { it.id }
        } else {
            emptyMap()
        }
        return toolLayers.toMap() + pluginDefinitions
    }

    fun getLayer(layerId: String, definitions: Map<String, MapLayerDefinition>): ILayer? {
        return definitions[layerId]?.let { factory.createLayer(it) }
    }

}

// TODO: Consider loading required attribution with the layer data rather than the definition (definition can contain the long form attribution)
suspend fun MapLayerLoader.getAttribution(context: Context, layerIds: List<String>): CharSequence? {
    val definitions = getDefinitions()
    val markdown = AppServiceRegistry.get<MarkdownService>()
    val attributions = layerIds.mapNotNull { layerId ->
        val attribution = definitions[layerId]?.attribution
            ?: return@mapNotNull null
        if (attribution.alwaysShow) {
            attribution.attribution
        } else {
            null
        }
    }.distinct()

    if (attributions.isEmpty()) {
        return null
    }

    return markdown.toMarkdown(
        context.getString(
            R.string.map_attribution_format,
            attributions.joinToString(", ")
        )
    )
}
