package com.kylecorry.trail_sense.tools.field_guide

import android.content.Context
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.getLongProperty
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerDefinition
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceType
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo
import com.kylecorry.trail_sense.tools.field_guide.map_layers.FieldGuideSightingLayer
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

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
            guideId = R.raw.guide_tool_field_guide,
            singletons = listOf(
                FieldGuideRepo::getInstance
            ),
            mapLayers = listOf(
                MapLayerDefinition(
                    FieldGuideSightingLayer.LAYER_ID,
                    context.getString(R.string.sightings),
                    description = context.getString(R.string.map_layer_field_guide_sightings_description),
                    preferences = listOf(
                        MapLayerPreference(
                            id = FieldGuideSightingLayer.PREFERENCE_SHOW_IMAGES,
                            title = context.getString(R.string.show_images),
                            type = MapLayerPreferenceType.Switch,
                            defaultValue = false
                        )
                    ),
                    openFeature = { feature, fragment ->
                        val fieldGuidePageId =
                            feature.getLongProperty(FieldGuideSightingLayer.PROPERTY_PAGE_ID)
                        val navController = fragment.findNavController()
                        navController.navigateWithAnimation(
                            R.id.fieldGuidePageFragment,
                            bundleOf(
                                "page_id" to fieldGuidePageId
                            )
                        )
                    }
                ) { FieldGuideSightingLayer() }
            )
        )
    }
}