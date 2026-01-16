package com.kylecorry.trail_sense.shared.map_layers.preferences.ui

import android.content.Context
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.DefaultMapLayerDefinitions
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerDefinition
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceRepo
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.copyLayerPreferences
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.getFullDependencyPreferenceKey
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.getFullPreferenceKey
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.removeLayerPreferences
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.converters.MapLayerViewPreferenceConverterFactory
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.tools.map.MapToolRegistration
import com.kylecorry.trail_sense.tools.navigation.NavigationToolRegistration
import com.kylecorry.trail_sense.tools.photo_maps.PhotoMapsToolRegistration

class MapLayerPreferenceManager(
    private val mapId: String,
    private val layers: List<MapLayerDefinition>,
    private val alwaysEnabledLayerIds: List<String>
) {

    private val prefs = AppServiceRegistry.get<PreferencesSubsystem>().preferences
    private val markdown = AppServiceRegistry.get<MarkdownService>()
    private val repo = AppServiceRegistry.get<MapLayerPreferenceRepo>()

    private var lastExpanded: String? = null
    
    var onScrollToTop: (() -> Unit)? = null

    fun populatePreferences(screen: PreferenceScreen, context: Context) {
        val selectedLayers = repo.getActiveLayerIds(mapId).toMutableList()
        val factory = MapLayerViewPreferenceConverterFactory()
        screen.removeAll()
        // Reversing the layers so the highest layer appears at the top of the list like other map apps
        layers.filter { selectedLayers.contains(it.id) }
            .sortedBy { selectedLayers.indexOf(it.id) }
            .reversed()
            .forEachIndexed { index, layer ->
                if (index > 0) {
                    val divider = Preference(context)
                    divider.layoutResource = R.layout.preference_divider
                    divider.isSelectable = false
                    screen.addPreference(divider)
                }

                val header = LayerHeaderPreference(context)
                header.title = layer.name
                header.summary = layer.description
                header.isExpanded = lastExpanded == layer.id
                // NOTE: Up and down are reversed from what is displayed to the user
                val currentIndex = selectedLayers.indexOf(layer.id)
                header.canMoveUp = currentIndex < selectedLayers.size - 1
                header.canMoveDown = currentIndex > 0

                header.onMoveUp = {
                    val index = selectedLayers.indexOf(layer.id)
                    if (index < selectedLayers.size - 1) {
                        val temp = selectedLayers[index + 1]
                        selectedLayers[index + 1] = layer.id
                        selectedLayers[index] = temp
                        repo.setActiveLayerIds(mapId, selectedLayers)
                        if (header.isExpanded) {
                            lastExpanded = layer.id
                        }
                        populatePreferences(screen, context)
                    }
                }

                header.onMoveDown = {
                    val index = selectedLayers.indexOf(layer.id)
                    if (index > 0) {
                        val temp = selectedLayers[index - 1]
                        selectedLayers[index - 1] = layer.id
                        selectedLayers[index] = temp
                        repo.setActiveLayerIds(mapId, selectedLayers)
                        if (header.isExpanded) {
                            lastExpanded = layer.id
                        }
                        populatePreferences(screen, context)
                    }
                }
                screen.addPreference(header)

                val category = createCategory(context)
                category.isVisible = header.isExpanded
                screen.addPreference(category)

                header.onPreferenceClickListener = {
                    header.isExpanded = !header.isExpanded
                    category.isVisible = header.isExpanded
                    true
                }

                val isAlwaysEnabled = alwaysEnabledLayerIds.contains(layer.id)
                val base = DefaultMapLayerDefinitions.getBasePreferences(
                    context,
                    context.getString(R.string.visible)
                )
                val preferences = base + layer.preferences
                val viewPreferences = preferences.map {
                    it to factory.getConverter(it.type).convert(context, mapId, layer.id, it)
                }

                viewPreferences.forEach {
                    val preference = it.second
                    if (isAlwaysEnabled && it.first.id == DefaultMapLayerDefinitions.ENABLED) {
                        preference.isVisible = false
                    }

                    if (it.first.id == DefaultMapLayerDefinitions.ENABLED) {

                        header.isLayerEnabled = if (isAlwaysEnabled) {
                            preference.isVisible = true
                            true
                        } else {
                            val key = it.first.getFullPreferenceKey(mapId, layer.id)
                            prefs.getBoolean(key) ?: (it.first.defaultValue as? Boolean ?: true)
                        }

                        preference.onPreferenceChangeListener = { pref, newValue ->
                            header.isLayerEnabled = newValue as Boolean
                            true
                        }
                    }

                    category.addPreference(preference)

                    val dependency = it.first.getFullDependencyPreferenceKey(mapId, layer.id)
                    if (dependency != null) {
                        preference.dependency = dependency
                    }
                }

                val mapIdToName = mapOf(
                    MapToolRegistration.MAP_ID to context.getString(R.string.map),
                    NavigationToolRegistration.MAP_ID to context.getString(R.string.navigation),
                    PhotoMapsToolRegistration.MAP_ID to context.getString(R.string.photo_maps)
                )

                if (layer.attribution != null) {
                    val licensePreference = createLabelPreference(
                        context,
                        context.getString(R.string.attribution),
                        markdown.toMarkdown(
                            layer.attribution.longAttribution ?: layer.attribution.attribution
                        )
                    )
                    category.addPreference(licensePreference)
                }

                val copyPreference = createLabelPreference(
                    context,
                    context.getString(R.string.copy_settings_to_other_maps)
                ) {
                    val otherMaps = getOtherMapIds()
                    Pickers.items(
                        context,
                        context.getString(R.string.copy_settings_to_other_maps),
                        otherMaps.mapNotNull { mapIdToName[it] },
                        otherMaps.indices.toList()
                    ) { indices ->
                        if (indices == null || indices.isEmpty()) {
                            return@items
                        }

                        val destinationIds = indices.map { otherMaps[it] }
                        prefs.copyLayerPreferences(layer.id, mapId, destinationIds)
                        Alerts.toast(context, context.getString(R.string.settings_copied))
                    }
                }
                category.addPreference(copyPreference)

                val removePreference = createLabelPreference(
                    context,
                    context.getString(R.string.remove_layer)
                ) {
                    Alerts.dialog(
                        context,
                        context.getString(R.string.remove_layer),
                        layer.name
                    ) { cancelled ->
                        if (!cancelled) {
                            selectedLayers.remove(layer.id)
                            repo.setActiveLayerIds(mapId, selectedLayers)
                            prefs.removeLayerPreferences(mapId, layer.id)
                            populatePreferences(screen, context)
                        }
                    }
                }
                category.addPreference(removePreference)
            }

        // Additional layers
        val additionalLayersPreference = createLabelPreference(
            context,
            context.getString(R.string.additional_layers)
        ) {
            val availableLayers = layers.filter { !selectedLayers.contains(it.id) }
            Pickers.items(
                context,
                context.getString(R.string.additional_layers),
                availableLayers.map { it.name }
            ) { selection ->
                if (selection == null) {
                    return@items
                }
                val newLayers = selection.map { availableLayers[it].id }
                newLayers.forEach {
                    prefs.removeLayerPreferences(mapId, it)
                }
                selectedLayers.addAll(newLayers)
                repo.setActiveLayerIds(mapId, selectedLayers)
                populatePreferences(screen, context)
                onScrollToTop?.invoke()
            }
        }
        additionalLayersPreference.icon = Resources.drawable(context, R.drawable.ic_add)
        additionalLayersPreference.icon?.setTint(Resources.androidTextColorSecondary(context))
        screen.addPreference(additionalLayersPreference)

        lastExpanded = null
    }

    private fun createLabelPreference(
        context: Context,
        title: CharSequence? = null,
        summary: CharSequence? = null,
        onClick: (() -> Unit)? = null
    ): Preference {
        val preference = SummaryClickablePreference(context)
        preference.title = title
        preference.summary = summary
        preference.isIconSpaceReserved = false
        preference.isSingleLineTitle = false
        preference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            onClick?.invoke()
            onClick != null
        }
        return preference
    }

    private fun getOtherMapIds(): List<String> {
        val mapIds = listOf(
            MapToolRegistration.MAP_ID,
            NavigationToolRegistration.MAP_ID,
            PhotoMapsToolRegistration.MAP_ID
        )
        return mapIds.filter { it != mapId }
    }

    private fun createCategory(context: Context): PreferenceCategory {
        val category = PreferenceCategory(context)
        category.isSingleLineTitle = false
        category.isIconSpaceReserved = false
        return category
    }
}
