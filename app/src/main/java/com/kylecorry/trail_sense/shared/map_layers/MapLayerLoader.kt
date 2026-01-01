package com.kylecorry.trail_sense.shared.map_layers

import android.content.Context
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerDefinition
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.ILayer
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class MapLayerLoader(private val context: Context) {


    private val toolLayers = Tools.getTools(context).flatMap { it.mapLayers }.associateBy { it.id }

    suspend fun getDefinitions(): Map<String, MapLayerDefinition> {
        // TODO: This is where it would query the plugins
        return toolLayers.toMap()
    }

    fun getLayer(
        layerId: String,
        taskRunner: MapLayerBackgroundTask = MapLayerBackgroundTask()
    ): ILayer? {
        // TODO: This is where it would check the layer ID to see if it is a plugin and create a plugin layer
        // The plugin layer IDs should use a consistent format like plugin__package_name__layer_id__tile / plugin__package_name__layer_id__feature
        return toolLayers[layerId]?.create(context, taskRunner)
    }

}

// TODO: Consider loading required attribution with the layer data rather than the definition (definition can contain the long form attribution)
suspend fun MapLayerLoader.getAttribution(layerIds: List<String>): CharSequence? {
    val context = AppServiceRegistry.get<Context>()
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