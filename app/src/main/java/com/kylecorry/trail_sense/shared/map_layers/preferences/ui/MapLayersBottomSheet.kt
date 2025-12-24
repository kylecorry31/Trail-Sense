package com.kylecorry.trail_sense.shared.map_layers.preferences.ui

import android.content.DialogInterface
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.CustomUiUtils.replaceChildFragment
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveBottomSheetFragment
import com.kylecorry.trail_sense.shared.map_layers.MapLayerRegistry

class MapLayersBottomSheet(
    private val mapId: String,
    private val layerIds: List<String>,
    private val alwaysEnabledLayerIds: List<String> = emptyList()
) : TrailSenseReactiveBottomSheetFragment(R.layout.fragment_map_layers_bottom_sheet) {

    private var onDismissListener: (() -> Unit)? = null

    fun setOnDismissListener(listener: (() -> Unit)?) {
        onDismissListener = listener
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissListener?.invoke()
    }

    override fun update() {
        val titleView = useView<Toolbar>(R.id.title)
        val mainActivity = useActivity() as MainActivity
        val registry = useService<MapLayerRegistry>()
        val preferences = useMemo {
            val allDefs = registry.getLayers()
            val defs = allDefs.filter { it.isConfigurable && layerIds.contains(it.id) }
                .sortedBy { layerIds.indexOf(it.id) }
            val manager = MapLayerPreferenceManager(mapId, defs, alwaysEnabledLayerIds)
            MapLayersBottomSheetFragment(manager, mainActivity)
        }

        useEffect(titleView) {
            titleView.rightButton.setOnClickListener {
                dismiss()
            }
        }

        useEffect(titleView, preferences) {
            replaceChildFragment(preferences, R.id.preferences_fragment)
        }
    }
}