package com.kylecorry.trail_sense.tools.field_guide.map_layers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.util.Size
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources.GeoJsonSource
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.getPreferences
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconIcon
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTag
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTagType
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuideService
import com.kylecorry.trail_sense.tools.field_guide.ui.FieldGuideFormatService

class FieldGuideSightingGeoJsonSource : GeoJsonSource {

    private val service = DependencyRegistry.get<FieldGuideService>()
    private val files = DependencyRegistry.get<FileSubsystem>()
    private val formatter = DependencyRegistry.get<FieldGuideFormatService>()
    var nameFormat = ""
    private val size = 12f
    private val imageSize = size * 1.5f
    private val bitmapCache = mutableMapOf<Long, Bitmap?>()

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

    private val tagColorMap = mapOf(
        FieldGuidePageTag.Plant to AppColor.Green.color,
        FieldGuidePageTag.Fungus to AppColor.Pink.color,
        FieldGuidePageTag.Animal to AppColor.DarkBlue.color,
        FieldGuidePageTag.Invertebrate to AppColor.Red.color
    )

    override suspend fun load(
        context: Context,
        bounds: CoordinateBounds,
        zoom: Int,
        params: Bundle
    ): GeoJsonObject {
        val preferences = params.getPreferences()
        val showImages =
            preferences.getBoolean(PREFERENCE_SHOW_IMAGES, false)
        val colorByClassification =
            preferences.getBoolean(PREFERENCE_COLOR_BY_CLASSIFICATION, true)
        if (nameFormat.isEmpty()) {
            nameFormat = context.getString(R.string.sighting_label)
        }
        val pages = service.getAllPages()

        val allSightings = pages
            .flatMap { page ->
                page.sightings.map { sighting ->
                    Pair(sighting, page)
                }
            }
            .filter { (sighting, _) -> sighting.location != null && sighting.showOnMap && bounds.contains(sighting.location) }

        // Cache pruning
        val currentPageIds = allSightings.map { it.second.id }.toSet()
        val keysToRemove = bitmapCache.keys.filter { it !in currentPageIds }
        keysToRemove.forEach { bitmapCache.remove(it) }

        val collection = GeoJsonFeatureCollection(
            allSightings.map { (sighting, page) ->
                val icon = getIconForPage(page)
                val bitmap = if (showImages) getBitmapForPage(page, context) else null
                val color = if (bitmap == null && colorByClassification) getColorForPage(page) else Color.BLACK
                val point = GeoJsonFeature.point(
                    sighting.location!!,
                    sighting.id,
                    nameFormat.format(formatter.formatName(page)),
                    color = color,
                    icon = if (bitmap == null) icon.id else null,
                    iconSize = if (bitmap == null) size * 0.75f else imageSize,
                    iconColor = if (bitmap == null) Colors.mostContrastingColor(
                        Color.BLACK,
                        Color.WHITE,
                        color
                    ) else null,
                    markerShape = if (bitmap == null) "circle" else null,
                    strokeColor = if (bitmap == null) Color.WHITE else null,
                    size = size,
                    isClickable = true,
                    layerId = SOURCE_ID,
                    bitmap = bitmap,
                    additionalProperties = mapOf(
                        PROPERTY_PAGE_ID to page.id,
                        PROPERTY_SIGHTING_ID to sighting.id
                    )
                )
                point
            }
        )

        return collection
    }

    private fun getBitmapForPage(page: FieldGuidePage, context: Context): Bitmap? {
        if (page.images.isEmpty()) {
            return null
        }

        if (bitmapCache.containsKey(page.id)) {
            return bitmapCache[page.id]
        }

        // Double the size for increased resolution
        val sizePixels = Resources.dp(context, imageSize * 2).toInt()

        val sourceBitmap = tryOrDefault(null) {
            files.bitmap(page.images.first(), Size(sizePixels, sizePixels))
        }

        val circularBitmap = sourceBitmap?.let { cropToCircle(it, sizePixels) }
        bitmapCache[page.id] = circularBitmap
        return circularBitmap
    }

    private fun cropToCircle(source: Bitmap, size: Int): Bitmap {
        val output = createBitmap(size, size)
        val canvas = Canvas(output)

        val path = Path()
        path.addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW)
        canvas.clipPath(path)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val scaledSource = source.scale(size, size)
        canvas.drawBitmap(scaledSource, 0f, 0f, paint)
        scaledSource.recycle()

        return output
    }

    private fun getIconForPage(page: FieldGuidePage): BeaconIcon {
        val classificationTags = page.tags.filter {
            it.type == FieldGuidePageTagType.Classification
        }

        val lowestTag = classificationTags
            .filter { tagIconMap.containsKey(it) }
            .maxByOrNull { it.parentId != null }

        return tagIconMap[lowestTag] ?: BeaconIcon.Information
    }

    private fun getColorForPage(page: FieldGuidePage): Int {
        val classificationTags = page.tags.filter {
            it.type == FieldGuidePageTagType.Classification
        }

        val filteredTags = classificationTags
            .filter { tagColorMap.containsKey(it) }

        val lowestTag = filteredTags.maxByOrNull { it.parentId != null } ?: filteredTags.firstOrNull()

        return tagColorMap[lowestTag] ?: Color.BLACK
    }

    companion object {
        const val SOURCE_ID = "field_guide_sighting"
        const val PROPERTY_PAGE_ID = "pageId"
        const val PROPERTY_SIGHTING_ID = "sightingId"
        const val PREFERENCE_SHOW_IMAGES = "show_images"
        const val PREFERENCE_COLOR_BY_CLASSIFICATION = "color_by_classification"
    }
}
