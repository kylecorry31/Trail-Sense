package com.kylecorry.trail_sense.tools.declination.ui

import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useBackgroundMemo
import com.kylecorry.trail_sense.shared.extensions.useCoordinateInputView
import com.kylecorry.trail_sense.shared.extensions.useLastLocation

class ToolDeclinationFragment : TrailSenseReactiveFragment(R.layout.fragment_tool_declination) {
    override fun update() {
        val locationView = useCoordinateInputView(R.id.location, lifecycleHookTrigger)
        val titleView = useView<Toolbar>(R.id.title)
        val initialLocation = useLastLocation()
        val formatter = useService<FormatService>()
        val (location, setLocation) = useState(initialLocation)
        val declination = useBackgroundMemo(location) {
            Geology.getGeomagneticDeclination(location)
        }

        // Purposely excluding initial location from this hook
        useEffect(locationView) {
            locationView.coordinate = initialLocation
            locationView.setOnCoordinateChangeListener {
                if (it != null) {
                    setLocation(it)
                }
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
    }
}