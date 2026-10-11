package com.kylecorry.trail_sense.tools.paths.ui.commands

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.view.doOnLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.setLayers
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.start
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.stop
import com.kylecorry.trail_sense.shared.views.MaterialSpinnerView
import com.kylecorry.trail_sense.tools.map.ui.MapView
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.PathSimplificationQuality
import com.kylecorry.trail_sense.tools.paths.domain.PathSimplifier
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.paths.map_layers.StaticLineGeoJsonSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class SimplifyPathCommand(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val pathService: IPathService = PathService.getInstance(context)
) : IPathCommand {

    override fun execute(path: Path) {
        lifecycleOwner.inBackground {
            val loading = withContext(Dispatchers.Main) {
                Alerts.loading(context, context.getString(R.string.loading))
            }
            val points = withContext(Dispatchers.Default) {
                pathService.getWaypoints(path.id).sortedBy { it.id }
            }
            withContext(Dispatchers.Main) {
                loading.dismiss()
                showPreview(path, points)
            }
        }
    }

    private fun showPreview(path: Path, points: List<PathPoint>) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_simplify_path, null)
        val map = view.findViewById<MapView>(R.id.simplify_preview_map)
        val removedText = view.findViewById<TextView>(R.id.simplify_points_removed)
        val qualitySpinner = view.findViewById<MaterialSpinnerView>(R.id.simplify_quality)
        val loadingIndicator = view.findViewById<View>(R.id.simplify_loading)

        val originalCoordinates = points.map { it.coordinate }
        val originalLayer = GeoJsonLayer(
            StaticLineGeoJsonSource(originalCoordinates, Color.GRAY, thicknessScale = 1.5f),
            ORIGINAL_LAYER_ID
        ).also { it.percentOpacity = ORIGINAL_PATH_OPACITY }
        val simplifiedSource =
            StaticLineGeoJsonSource(emptyList(), path.style.color, thicknessScale = 0.75f)
        val simplifiedLayer = GeoJsonLayer(simplifiedSource, SIMPLIFIED_LAYER_ID)

        fun selectedQuality() =
            QUALITIES.getOrElse(qualitySpinner.selectedItemPosition) { QUALITIES[0] }.first

        val simplified = mutableMapOf<PathSimplificationQuality, List<PathPoint>>()
        var updateJob: Job? = null

        fun update() {
            val quality = selectedQuality()
            updateJob?.cancel()
            updateJob = lifecycleOwner.lifecycleScope.launch {
                loadingIndicator.visibility = View.VISIBLE
                val kept = simplified.getOrPut(quality) {
                    withContext(Dispatchers.Default) { PathSimplifier.simplify(points, quality) }
                }
                val removed = points.size - kept.size
                simplifiedSource.line = kept.map { it.coordinate }
                simplifiedLayer.invalidate()
                map.invalidate()
                removedText.text = context.resources.getQuantityString(
                    R.plurals.waypoints_to_be_deleted,
                    removed,
                    removed
                )
                loadingIndicator.visibility = View.GONE
            }
        }

        map.setLayers(listOf(originalLayer, simplifiedLayer))
        map.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = map.start()
            override fun onViewDetachedFromWindow(v: View) {
                updateJob?.cancel()
                map.stop()
            }
        })
        if (originalCoordinates.size >= 2) {
            map.doOnLayout { map.fitIntoView(CoordinateBounds.from(originalCoordinates)) }
        }

        qualitySpinner.setHint(context.getString(R.string.quality))
        qualitySpinner.setItems(QUALITIES.map { context.getString(it.second) })
        qualitySpinner.setOnItemSelectedListener { update() }
        qualitySpinner.setSelection(0)

        Alerts.dialog(
            context,
            context.getString(R.string.simplify),
            contentView = view
        ) { cancelled ->
            if (!cancelled) {
                applySimplification(path, selectedQuality())
            }
        }
    }

    private fun applySimplification(path: Path, quality: PathSimplificationQuality) {
        lifecycleOwner.inBackground {
            val loading = withContext(Dispatchers.Main) {
                Alerts.loading(context, context.getString(R.string.simplifying))
            }
            val deleted = withContext(Dispatchers.IO) {
                pathService.simplifyPath(path.id, quality)
            }
            withContext(Dispatchers.Main) {
                loading.dismiss()
                Alerts.toast(
                    context,
                    context.resources.getQuantityString(
                        R.plurals.waypoints_deleted,
                        deleted,
                        deleted
                    )
                )
            }
        }
    }

    companion object {
        private val QUALITIES = listOf(
            PathSimplificationQuality.High to R.string.high,
            PathSimplificationQuality.Medium to R.string.moderate,
            PathSimplificationQuality.Low to R.string.low
        )
        private const val ORIGINAL_PATH_OPACITY = 50 / 255f
        private const val ORIGINAL_LAYER_ID = "simplify_preview_original"
        private const val SIMPLIFIED_LAYER_ID = "simplify_preview_simplified"
    }
}
