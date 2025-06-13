package com.kylecorry.trail_sense.tools.declination.ui

import android.view.View
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.useBackgroundMemo
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.alerts.MultiLoadingIndicator
import com.kylecorry.trail_sense.shared.alerts.ViewLoadingIndicator
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useCoordinateInputView
import com.kylecorry.trail_sense.shared.extensions.useLoadingIndicator
import com.kylecorry.trail_sense.shared.extensions.useLocation

class ToolDeclinationFragment : TrailSenseReactiveFragment(R.layout.fragment_tool_declination) {
    override fun update() {
        val locationView = useCoordinateInputView(R.id.location, lifecycleHookTrigger)
        val titleView = useView<Toolbar>(R.id.title)
        val loadingView = useView<View>(R.id.loading)
        val declinationArrowView = useView<DeclinationView>(R.id.declination)
        val (initialLocation, isLocationUpToDate) = useLocation()
        val formatter = useService<FormatService>()
        val (location, setLocation) = useState(initialLocation)
        val declination = useBackgroundMemo(location) {
            Geology.getGeomagneticDeclination(location)
        }
        val loadingIndicator = useMemo(locationView, loadingView) {
            MultiLoadingIndicator(
                listOf(
                    // TODO: Disable the coordinate view instead
                    ViewLoadingIndicator(locationView, true),
                    ViewLoadingIndicator(loadingView)
                )
            )
        }

        useLoadingIndicator(!isLocationUpToDate, loadingIndicator)

        // Purposely excluding initial location from this hook
        useEffect(locationView) {
            locationView.coordinate = initialLocation
            locationView.setOnCoordinateChangeListener {
                if (it != null) {
                    setLocation(it)
                }
            }
        }

        useEffect(locationView, isLocationUpToDate) {
            if (isLocationUpToDate) {
                locationView.coordinate = location
            }
        }

        useEffect(titleView, declination, formatter) {
            val actualDeclination = declination ?: 0f

            titleView.title.text = if (SolMath.isZero(actualDeclination, 0.05f)) {
                formatter.formatDegrees(actualDeclination, 1)
            } else {
                "${formatter.formatDegrees(actualDeclination, 1)} (${
                    formatter.formatDirection(
                        if (actualDeclination < 0) {
                            CompassDirection.West
                        } else {
                            CompassDirection.East
                        }
                    )
                })"
            }
        }

        useEffect(declinationArrowView, declination) {
            declinationArrowView.declination = declination ?: 0f
        }
    }
}