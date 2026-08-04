package com.kylecorry.trail_sense.tools.field_guide

import android.content.Context
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.extensions.getLongProperty
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerDefinition
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceType
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuideService
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo
import com.kylecorry.trail_sense.tools.field_guide.map_layers.FieldGuideSightingGeoJsonSource
import com.kylecorry.trail_sense.tools.field_guide.quickactions.QuickActionRecordSighting
import com.kylecorry.trail_sense.tools.field_guide.ui.FieldGuideDeepLinks
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolBroadcast
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolEventEmitter
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object FieldGuideToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.FIELD_GUIDE,
            context.getString(R.string.field_guide),
            R.drawable.field_guide,
            R.id.fieldGuideFragment,
            ToolCategory.Books,
            additionalNavigationIds = listOf(
                R.id.fieldGuidePageFragment,
                R.id.sightingListFragment,
                R.id.createFieldGuideSightingFragment,
                R.id.createFieldGuidePageFragment
            ),
            quickActions = listOf(
                ToolQuickAction(
                    Tools.QUICK_ACTION_RECORD_SIGHTING,
                    context.getString(R.string.record_sighting),
                    ::QuickActionRecordSighting
                )
            ),
            guideId = R.raw.guide_tool_field_guide,
            initialize = {
                DependencyRegistry.addSingleton(FieldGuideRepo.getInstance(it))
                DependencyRegistry.addSingleton(
                    FieldGuideService(
                        it,
                        getAppService<FieldGuideRepo>(),
                        ToolEventEmitter
                    )
                )
            },
            broadcasts = listOf(
                ToolBroadcast(BROADCAST_SIGHTING_RECORDED, "Sighting recorded")
            ),
            diagnostics = listOf(
                ToolDiagnosticFactory.externalStorage(context)
            ),
            mapLayers = listOf(
                MapLayerDefinition(
                    FieldGuideSightingGeoJsonSource.SOURCE_ID,
                    context.getString(R.string.sightings),
                    description = context.getString(R.string.map_layer_field_guide_sightings_description),
                    preferences = listOf(
                        MapLayerPreference(
                            id = FieldGuideSightingGeoJsonSource.PREFERENCE_SHOW_IMAGES,
                            title = context.getString(R.string.show_images),
                            type = MapLayerPreferenceType.Switch,
                            defaultValue = false
                        ),
                        MapLayerPreference(
                            id = FieldGuideSightingGeoJsonSource.PREFERENCE_COLOR_BY_CLASSIFICATION,
                            title = context.getString(R.string.color_by_classification),
                            type = MapLayerPreferenceType.Switch,
                            defaultValue = true
                        )
                    ),
                    openFeature = { feature, fragment ->
                        val fieldGuidePageId =
                            feature.getLongProperty(FieldGuideSightingGeoJsonSource.PROPERTY_PAGE_ID)
                                ?: return@MapLayerDefinition
                        val fieldGuideSightingId =
                            feature.getLongProperty(FieldGuideSightingGeoJsonSource.PROPERTY_SIGHTING_ID)
                                ?: return@MapLayerDefinition
                        val navController = fragment.findNavController()
                        FieldGuideDeepLinks.navigateToSighting(navController, fieldGuidePageId, fieldGuideSightingId)
                    },
                    geoJsonSource = ::FieldGuideSightingGeoJsonSource,
                    refreshBroadcasts = listOf(BROADCAST_SIGHTING_RECORDED)
                )
            )
        )
    }

    const val BROADCAST_SIGHTING_RECORDED = "field-guide-broadcast-sighting-recorded"
}
