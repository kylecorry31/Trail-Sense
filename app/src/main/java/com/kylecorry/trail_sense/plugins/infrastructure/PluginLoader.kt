package com.kylecorry.trail_sense.plugins.infrastructure

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.ServiceInfo
import android.os.Build
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.json.fromJson
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.plugins.domain.Plugin
import com.kylecorry.trail_sense.plugins.domain.PluginResourceServiceDetails
import com.kylecorry.trail_sense.plugins.domain.PluginResourceServiceFeatures
import com.kylecorry.trail_sense.plugins.infrastructure.persistence.PluginRegistrationEntity
import com.kylecorry.trail_sense.plugins.infrastructure.persistence.PluginRegistrationRepo
import com.kylecorry.trail_sense.plugins.map_layers.PluginGeoJsonSource
import com.kylecorry.trail_sense.plugins.map_layers.PluginTileSource
import com.kylecorry.trail_sense.shared.ProguardIgnore
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerAttribution
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerDefinition
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerType
import java.time.Duration

class PluginLoader(private val context: Context) {

    private val registrationRepo = PluginRegistrationRepo.getInstance(context)

    fun getResourceServicePlugins(): List<Plugin> {
        val filter = Intent(PLUGIN_RESOURCE_SERVICE_ACTION)
        val services = context.packageManager.queryIntentServices(filter, 0)
        return services.mapNotNull {
            getPlugin(it.serviceInfo.packageName)
        }.distinctBy { it.packageId }
    }

    fun getPlugin(packageId: String): Plugin? {
        return tryOrDefault(null) {
            val appInfo = context.packageManager.getApplicationInfo(packageId, 0)
            val appName = context.packageManager.getApplicationLabel(appInfo).toString()
            val version = context.packageManager.getPackageInfo(packageId, 0).versionName
            val signatures = Package.getSignatureSha256Fingerprints(context, packageId).sorted()

            Plugin(packageId, appName, version, signatures)
        }
    }

    suspend fun getPluginResourceService(packageId: String): PluginResourceServiceDetails? {
        val filter = pluginResourceServiceIntent(packageId)
        val service = context.packageManager.queryIntentServices(filter, 0).firstOrNull() ?: return null
        return getPluginResourceService(service.serviceInfo)
    }

    private suspend fun getPluginResourceService(serviceInfo: ServiceInfo): PluginResourceServiceDetails {
        val servicePackageId = serviceInfo.packageName
        val appInfo = context.packageManager.getApplicationInfo(servicePackageId, 0)
        val appName = context.packageManager.getApplicationLabel(appInfo).toString()
        val packageInfo = context.packageManager.getPackageInfo(servicePackageId, 0)
        val version = packageInfo.versionName
        val packageVersionCode = packageInfo.getVersionCode()

        val registration = getRegistration(servicePackageId, packageVersionCode)

        return PluginResourceServiceDetails(
            servicePackageId,
            appName,
            version,
            PluginResourceServiceFeatures(
                registration?.features?.weather ?: emptyList(),
                registration?.features?.mapLayers?.mapNotNull {
                    toMapLayerDefinition(servicePackageId, appName, it)
                } ?: emptyList()
            )
        )
    }

    private suspend fun getRegistration(
        packageId: String,
        packageVersionCode: Long
    ): RegistrationResponse? {
        val cached = registrationRepo.get(packageId)
        if (cached?.versionCode == packageVersionCode) {
            return cached.payload.fromJson()
        }

        val payload = PluginResourceServiceConnection(context, packageId).use {
            it.send("/registration")?.payload
        }

        if (payload != null){
            registrationRepo.upsert(PluginRegistrationEntity(packageId, packageVersionCode, payload))
        }

        return payload?.fromJson()
    }

    private fun toMapLayerDefinition(
        packageId: String,
        pluginName: String,
        layer: RegistrationMapLayerResponse
    ): MapLayerDefinition? {
        val layerType = mapLayerType(layer.layerType) ?: return null
        return MapLayerDefinition(
            "plugin::$packageId::${layer.endpoint}",
            "${pluginName}: ${layer.name}",
            isConfigurable = true,
            layerType = layerType,
            attribution = layer.attribution?.let {
                MapLayerAttribution(
                    it.attribution,
                    it.longAttribution,
                    it.alwaysShow
                )
            },
            description = "${context.getString(R.string.plugin_name, pluginName)}\n${layer.description ?: ""}".trim(),
            minZoomLevel = layer.minZoomLevel,
            isTimeDependent = layer.isTimeDependent,
            refreshInterval = layer.refreshInterval?.let { millis -> Duration.ofMillis(millis) },
            refreshBroadcasts = layer.refreshBroadcasts,
            cacheKeys = layer.cacheKeys,
            shouldMultiply = layer.shouldMultiply,
            tileSource = if (layerType == MapLayerType.Tile) {
                { PluginTileSource(packageId, layer.endpoint) }
            } else {
                null
            },
            geoJsonSource = if (layerType == MapLayerType.Feature) {
                { PluginGeoJsonSource(packageId, layer.endpoint) }
            } else {
                null
            }
        )
    }

    private fun mapLayerType(layerType: String): MapLayerType? {
        return when (layerType.lowercase()) {
            getLayerTypeId(MapLayerType.Feature) -> MapLayerType.Feature
            getLayerTypeId(MapLayerType.Tile) -> MapLayerType.Tile
            else -> null
        }
    }

    private fun getLayerTypeId(type: MapLayerType): String {
        return when (type) {
            MapLayerType.Overlay -> "overlay"
            MapLayerType.Feature -> "feature"
            MapLayerType.Tile -> "tile"
        }
    }

    private fun PackageInfo.getVersionCode(): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            longVersionCode
        } else {
            @Suppress("DEPRECATION")
            versionCode.toLong()
        }
    }

    private data class RegistrationFeaturesResponse(
        val weather: List<String> = emptyList(),
        val mapLayers: List<RegistrationMapLayerResponse> = emptyList()
    ) : ProguardIgnore

    private data class RegistrationResponse(
        val features: RegistrationFeaturesResponse
    ) : ProguardIgnore

    private data class RegistrationMapLayerResponse(
        val endpoint: String,
        val name: String,
        val layerType: String,
        val attribution: RegistrationMapLayerAttributionResponse? = null,
        val description: String? = null,
        val minZoomLevel: Int? = null,
        val isTimeDependent: Boolean = false,
        val refreshInterval: Long? = null,
        val refreshBroadcasts: List<String> = emptyList(),
        val cacheKeys: List<String>? = null,
        val shouldMultiply: Boolean = false
    ) : ProguardIgnore

    private data class RegistrationMapLayerAttributionResponse(
        val attribution: String,
        val longAttribution: String? = null,
        val alwaysShow: Boolean = false
    ) : ProguardIgnore
}
