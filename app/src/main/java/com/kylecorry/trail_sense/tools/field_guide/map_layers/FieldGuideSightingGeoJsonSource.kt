package com.kylecorry.trail_sense.tools.field_guide.map_layers

import android.graphics.Color
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources.GeoJsonSource
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconIcon
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTag
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTagType
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo

class FieldGuideSightingGeoJsonSource : GeoJsonSource {

    private val repo = AppServiceRegistry.get<FieldGuideRepo>()
    var nameFormat = ""

    private val tagIconMap = mapOf(
        FieldGuidePageTag.Plant to BeaconIcon.Plant,
        FieldGuidePageTag.Fungus to BeaconIcon.Fungus,
        FieldGuidePageTag.Animal to BeaconIcon.Animal,
        FieldGuidePageTag.Mammal to BeaconIcon.Mammal,
        FieldGuidePageTag.Bird to BeaconIcon.Bird,
        FieldGuidePageTag.Reptile to BeaconIcon.Reptile,
        FieldGuidePageTag.Amphibian to BeaconIcon.Amphibian,
        FieldGuidePageTag.Fish to BeaconIcon.Fish,
        FieldGuidePageTag.Invertebrate to BeaconIcon.Invertebrate,
        FieldGuidePageTag.Rock to BeaconIcon.Rock,
        FieldGuidePageTag.Weather to BeaconIcon.Weather,
        FieldGuidePageTag.Other to BeaconIcon.Information
    )

    override suspend fun load(
        bounds: CoordinateBounds,
        metersPerPixel: Float
    ): GeoJsonObject {
        val pages = repo.getAllPages()

        val allSightings = pages
            .flatMap { page ->
                page.sightings.map { sighting ->
                    Pair(sighting, page)
                }
            }
            .filter { (sighting, _) -> sighting.location != null }

        val collection = GeoJsonFeatureCollection(
            allSightings.map { (sighting, page) ->
                val icon = getIconForPage(page)
                val point = GeoJsonFeature.point(
                    sighting.location!!,
                    sighting.id,
                    nameFormat.format(page.name),
                    color = Color.BLACK,
                    icon = icon.id,
                    iconSize = 12f * 0.75f,
                    iconColor = Color.WHITE,
                    markerShape = "circle",
                    size = 12f,
                    isClickable = true,
                    layerId = FieldGuideSightingLayer.LAYER_ID,
                    additionalProperties = mapOf(
                        FieldGuideSightingLayer.PROPERTY_PAGE_ID to page.id
                    )
                )
                point
            }
        )

        return collection
    }

    private fun getIconForPage(page: com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage): BeaconIcon {
        val classificationTags = page.tags.filter {
            it.type == FieldGuidePageTagType.Classification
        }

        val lowestTag = classificationTags
            .filter { tagIconMap.containsKey(it) }
            .maxByOrNull { it.parentId != null }

        return tagIconMap[lowestTag] ?: BeaconIcon.Information
    }
}

