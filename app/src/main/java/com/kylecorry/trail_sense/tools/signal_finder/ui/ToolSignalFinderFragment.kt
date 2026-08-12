package com.kylecorry.trail_sense.tools.signal_finder.ui

import android.os.Bundle
import android.widget.TextView
import androidx.core.text.method.LinkMovementMethodCompat
import androidx.lifecycle.Observer
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.asLiveData
import com.kylecorry.andromeda.fragments.useBackgroundEffect
import com.kylecorry.andromeda.fragments.useCoroutineQueue
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.signal.CellSignal
import com.kylecorry.andromeda.signal.ICellSignalSensor
import com.kylecorry.andromeda.views.list.AndromedaListView
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.ApproximateCoordinate
import com.kylecorry.trail_sense.shared.ErrorBannerReason
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useCellSignalSensor
import com.kylecorry.trail_sense.shared.extensions.useDestroyEffect
import com.kylecorry.trail_sense.shared.extensions.useGPSLocation
import com.kylecorry.trail_sense.shared.extensions.useMainActivity
import com.kylecorry.trail_sense.shared.extensions.useNavController
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.Toolbar
import com.kylecorry.trail_sense.shared.views.UserError
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.signal_finder.infrastructure.CellTowerModel
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.Duration

class ToolSignalFinderFragment : TrailSenseReactiveFragment(R.layout.fragment_tool_signal_finder) {

    override fun update() {
        val context = useAndroidContext()

        // Views
        val list = useView<AndromedaListView>(R.id.list)
        val disclaimer = useView<TextView>(R.id.disclaimer)
        val emptyText = useView<TextView>(R.id.empty_text)
        val title = useView<Toolbar>(R.id.title)
        val navController = useNavController()

        // Services
        val markdown = useService<MarkdownService>()
        val navigator = useService<Navigator>()
        val queue = useCoroutineQueue()
        val sensorService = useService<SensorService>()
        val mainActivity = useMainActivity()

        // State
        val hasLocationPermission = useMemo(sensorService, lifecycleHookTrigger.onResume()) {
            sensorService.hasLocationPermission()
        }
        val signals = useCellSignals(hasLocationPermission)
        val (location, _) = useGPSLocation(Duration.ofSeconds(5))
        val (nearby, setNearby) = useState<List<ApproximateCoordinate>>(emptyList())
        val (loading, setLoading) = useState(false)

        // Only reported when the permission changes so a banner the user closed doesn't come back
        // every time they resume the tool
        useEffect(mainActivity, hasLocationPermission) {
            updateLocationPermissionError(mainActivity, hasLocationPermission)
        }

        useDestroyEffect(mainActivity) {
            mainActivity.errorBanner.dismiss(ErrorBannerReason.LocationPermissionDenied)
        }

        list.emptyView = emptyText

        // Set up the disclaimer
        useEffect(disclaimer) {
            disclaimer.text = markdown.toMarkdown(getString(R.string.cell_tower_disclaimer))
            disclaimer.movementMethod = LinkMovementMethodCompat.getInstance()
        }

        // List items
        useEffect(
            list,
            navController,
            signals,
            nearby,
            distance(location, Distance.meters(100f))
        ) {
            val signalMapper = CellSignalListItemMapper(context)
            val signalItems = signals.map { signalMapper.map(it) }

            val towerMapper =
                CellTowerListItemMapper(context, location) { towerLocation, action ->
                    when (action) {
                        CellTowerListItemAction.Navigate -> {
                            navigator.navigateTo(
                                towerLocation.coordinate,
                                getString(R.string.cell_tower)
                            )
                            navController.openTool(Tools.NAVIGATION)
                        }

                        CellTowerListItemAction.CreateBeacon -> {
                            val bundle = Bundle().apply {
                                putParcelable("initial_location", GeoUri(towerLocation.coordinate))
                            }
                            navController.navigate(R.id.placeBeaconFragment, bundle)
                        }
                    }
                }
            val nearbyItems = nearby.map { towerLocation ->
                towerMapper.map(towerLocation)
            }

            list.setItems(signalItems + nearbyItems)
        }

        // Cell tower updating
        useBackgroundEffect(distance(location, Distance.meters(100f))) {
            setLoading(true)
            queue.replace {
                setNearby(
                    CellTowerModel.getTowers(
                        CoordinateBounds.from(Geofence(location, Distance.kilometers(20f))),
                        5
                    ).sortedBy { location.distanceTo(it.coordinate) })
                setLoading(false)
            }
        }

        // Loading
        useEffect(title, loading) {
            title.subtitle.text = if (loading) {
                getString(R.string.loading)
            } else {
                null
            }
        }
    }

    private fun useCellSignals(vararg values: Any?): List<CellSignal> {
        val cellSignal = useCellSignalSensor(false, *values)
        val (signals, setSignals) = useState<List<CellSignal>>(emptyList())
        val owner = useLifecycleOwner()

        useEffectWithCleanup(cellSignal, owner) {
            val liveData = cellSignal.asLiveData()
            val observer = object : Observer<ICellSignalSensor?> {
                override fun onChanged(value: ICellSignalSensor?) {
                    if (value == null) {
                        return
                    }
                    setSignals(
                        cellSignal.signals.sortedWith(
                            compareByDescending<CellSignal> { signal -> signal.isRegistered }
                                .thenByDescending { signal -> signal.strength }
                                .thenByDescending { signal -> signal.id }
                        )
                    )
                }
            }
            liveData.observe(owner, observer)
            return@useEffectWithCleanup {
                liveData.removeObserver(observer)
            }
        }

        return signals
    }

    private fun updateLocationPermissionError(
        mainActivity: MainActivity,
        hasPermission: Boolean
    ) {
        val banner = mainActivity.errorBanner
        if (hasPermission) {
            banner.dismiss(ErrorBannerReason.LocationPermissionDenied)
            return
        }

        banner.report(
            UserError(
                ErrorBannerReason.LocationPermissionDenied,
                getString(R.string.location_required_for_cell_signals),
                R.drawable.signal_cellular_outline,
                getString(R.string.settings)
            ) {
                startActivity(Intents.appSettings(requireContext()))
            }
        )
    }
}
