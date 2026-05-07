package com.kylecorry.trail_sense.tools.offline_maps.ui

import android.graphics.Color
import androidx.core.view.doOnLayout
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.useBackgroundEffect
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useTrigger
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.LayerFactory
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.setLayers
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.start
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.stop
import com.kylecorry.trail_sense.tools.map.ui.MapView
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapFile
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.persistence.OfflineMapFileRepo
import com.kylecorry.trail_sense.tools.offline_maps.map_layers.OfflineMapTileSource
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.DeleteOfflineMapCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.EditOfflineMapAttributionCommand
import com.kylecorry.trail_sense.tools.offline_maps.ui.commands.RenameOfflineMapCommand
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class OfflineMapViewFragment : TrailSenseReactiveFragment(R.layout.fragment_offline_map_view) {

    override fun update() {
        val context = useAndroidContext()
        val title = useView<Toolbar>(R.id.title)
        val zoomInButton = useView<FloatingActionButton>(R.id.zoom_in_btn)
        val zoomOutButton = useView<FloatingActionButton>(R.id.zoom_out_btn)
        val mapView = useView<MapView>(R.id.map)
        val mapId = useMemo {
            requireArguments().getLong("offline_map_file_id")
        }
        val (refreshKey, refresh) = useTrigger()
        val map = useOfflineMap(mapId, refreshKey)

        useEffect(mapView) {
            mapView.backgroundColorOverride = Color.rgb(127, 127, 127)
        }

        useEffect(zoomInButton, zoomOutButton, mapView) {
            zoomInButton.setOnClickListener { mapView.zoom(2f) }
            zoomOutButton.setOnClickListener { mapView.zoom(0.5f) }
        }

        useEffect(title, map) {
            map?.let { title.title.text = it.name }
        }

        useEffect(title, map) {
            title.rightButton.setOnClickListener {
                val currentMap = map ?: return@setOnClickListener
                Pickers.menu(
                    it,
                    listOf(
                        getString(R.string.rename),
                        getString(R.string.attribution),
                        getString(R.string.delete)
                    )
                ) { index ->
                    when (index) {
                        0 -> rename(currentMap, refresh)
                        1 -> editAttribution(currentMap, refresh)
                        2 -> delete(currentMap)
                    }
                    true
                }
            }
        }

        useEffectWithCleanup(context, mapView, map) {
            if (map == null) {
                mapView.setLayers(emptyList())
                return@useEffectWithCleanup {}
            }
            val factory = LayerFactory()
            val layer = Tools.getMapLayerDefinition(context, OfflineMapTileSource.SOURCE_ID)!!
            val tileLayer = factory.createLayer(layer)
            tileLayer.setFeatureFilter(map.id.toString())
            mapView.setLayers(listOf(tileLayer))
            mapView.start()
            map.bounds?.let { bounds ->
                mapView.doOnLayout {
                    mapView.fitIntoView(bounds)
                }
            }
            return@useEffectWithCleanup {
                mapView.stop()
            }
        }
    }

    private fun useOfflineMap(mapId: Long, refreshKey: String): OfflineMapFile? {
        val repo = useService<OfflineMapFileRepo>()
        val (map, setMap) = useState<OfflineMapFile?>(null)

        useBackgroundEffect(mapId, refreshKey, lifecycleHookTrigger.onResume()) {
            setMap(repo.get(mapId))
        }

        return map
    }

    private fun rename(map: OfflineMapFile, onRenamed: () -> Unit) {
        inBackground {
            RenameOfflineMapCommand(requireContext()).execute(map)
            onMain {
                onRenamed()
            }
        }
    }

    private fun editAttribution(map: OfflineMapFile, onUpdated: () -> Unit) {
        inBackground {
            EditOfflineMapAttributionCommand(requireContext()).execute(map)
            onMain {
                onUpdated()
            }
        }
    }

    private fun delete(map: OfflineMapFile) {
        inBackground {
            val repo = getAppService<OfflineMapFileRepo>()
            DeleteOfflineMapCommand(requireContext()).execute(map)
            if (repo.get(map.id) == null) {
                findNavController().popBackStack()
            }
        }
    }
}
