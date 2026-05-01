package com.kylecorry.trail_sense.settings.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.useBackgroundMemo
import com.kylecorry.andromeda.views.list.AndromedaListView
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemTag
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.plugins.PluginSubsystem
import com.kylecorry.trail_sense.plugins.domain.Plugin
import com.kylecorry.trail_sense.plugins.domain.PluginResourceServiceDetails
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerDefinition
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerType
import kotlinx.coroutines.launch

class PluginDetailsFragment : TrailSenseReactiveFragment(R.layout.fragment_plugin_details) {

    override fun update() {
        val titleView = useView<Toolbar>(R.id.title)
        val unavailableView = useView<TextView>(R.id.unavailable)
        val mapLayersView = useView<AndromedaListView>(R.id.map_layers)
        val openPluginButton = useView<MaterialButton>(R.id.open_plugin)
        val appInfoButton = useView<MaterialButton>(R.id.app_info)
        val reloadButton = useView<MaterialButton>(R.id.reload)
        val disconnectButton = useView<MaterialButton>(R.id.disconnect)

        val pluginSubsystem = useService<PluginSubsystem>()
        val packageId = useMemo {
            arguments?.getString("package_id").orEmpty()
        }
        val (reloadKey, setReloadKey) = useState(0)
        val details = useBackgroundMemo(packageId, reloadKey, resetOnResume) {
            loadDetails(pluginSubsystem, packageId)
        }
        val isUnavailable = details?.plugin == null || details.resourceServiceDetails == null
        val plugin = details?.plugin
        val resourceDetails = details?.resourceServiceDetails
        val launchIntent = useMemo(packageId, isUnavailable) {
            if (isUnavailable) {
                null
            } else {
                requireContext().packageManager.getLaunchIntentForPackage(packageId)
            }
        }

        useEffect(
            titleView,
            unavailableView,
            mapLayersView,
            details
        ) {
            titleView.title.text = plugin?.name ?: getString(R.string.plugins)
            val version = plugin?.version ?: resourceDetails?.version ?: getString(R.string.unknown)
            titleView.subtitle.text = "$packageId ($version)"
            unavailableView.isVisible = isUnavailable
            unavailableView.text = if (details == null) {
                getString(R.string.loading)
            } else {
                getString(R.string.plugin_unavailable)
            }
            mapLayersView.setItems(
                getMapLayerItems(
                    resourceDetails?.features?.mapLayers,
                    resourceDetails?.name ?: plugin?.name
                )
            )
        }

        useEffect(
            openPluginButton,
            appInfoButton,
            reloadButton,
            disconnectButton,
            isUnavailable,
            launchIntent
        ) {
            openPluginButton.isVisible = launchIntent != null
            openPluginButton.isEnabled = !isUnavailable && launchIntent != null
            appInfoButton.isEnabled = !isUnavailable
            reloadButton.isEnabled = !isUnavailable
            disconnectButton.isEnabled = !isUnavailable
        }

        useEffect(openPluginButton, launchIntent) {
            openPluginButton.setOnClickListener {
                launchIntent?.let { startActivity(it) }
            }
        }

        useEffect(appInfoButton, packageId) {
            appInfoButton.setOnClickListener {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageId")
                }
                startActivity(intent)
            }
        }

        useEffect(reloadButton, pluginSubsystem, packageId, reloadKey) {
            reloadButton.setOnClickListener {
                lifecycleScope.launch {
                    pluginSubsystem.reloadRegistration(packageId)
                    setReloadKey(reloadKey + 1)
                    Alerts.toast(requireContext(), getString(R.string.done))
                }
            }
        }

        useEffect(disconnectButton, plugin, pluginSubsystem) {
            disconnectButton.setOnClickListener {
                plugin ?: return@setOnClickListener
                Alerts.dialog(
                    requireContext(),
                    getString(R.string.plugin_disconnect_title),
                    getString(R.string.plugin_disconnect_message, plugin.name)
                ) { cancelled ->
                    if (!cancelled) {
                        lifecycleScope.launch {
                            pluginSubsystem.disconnect(plugin)
                            tryOrNothing {
                                findNavController().navigateUp()
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun loadDetails(
        pluginSubsystem: PluginSubsystem,
        packageId: String
    ): PluginDetails {
        val plugin = pluginSubsystem.getConnectedPlugins()
            .firstOrNull { it.packageId == packageId }
        return PluginDetails(
            plugin,
            plugin?.let { pluginSubsystem.getPluginResourceServiceDetails(packageId) }
        )
    }

    private fun getMapLayerItems(
        mapLayers: List<MapLayerDefinition>?,
        pluginName: String?
    ): List<ListItem> {
        val layers = mapLayers.orEmpty()
        return if (layers.isEmpty()) {
            listOf(ListItem(-1, getString(R.string.none)))
        } else {
            layers.map {
                ListItem(
                    it.id.hashCode().toLong(),
                    it.name.withoutPluginPrefix(pluginName),
                    it.description.withoutPluginDescriptionPrefix(pluginName),
                    tags = listOf(
                        ListItemTag(
                            it.layerType.getName(),
                            null,
                            Resources.androidTextColorSecondary(requireContext())
                        )
                    )
                )
            }
        }
    }

    private fun MapLayerType.getName(): String {
        return when (this) {
            MapLayerType.Tile -> getString(R.string.tile_layer)
            MapLayerType.Feature -> getString(R.string.feature_layer)
            MapLayerType.Overlay -> getString(R.string.overlay_layer)
        }
    }

    private fun String.withoutPluginPrefix(pluginName: String?): String {
        val prefix = "${pluginName ?: return this}:"
        return removePrefix(prefix).trimStart()
    }

    private fun String?.withoutPluginDescriptionPrefix(pluginName: String?): String? {
        val prefix = getString(R.string.plugin_name, pluginName ?: return this)
        return this
            ?.removePrefix(prefix)
            ?.trimStart()
            ?.takeIf { it.isNotEmpty() }
    }

    private data class PluginDetails(
        val plugin: Plugin?,
        val resourceServiceDetails: PluginResourceServiceDetails?
    )
}
