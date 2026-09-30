package com.kylecorry.trail_sense.tools.light.ui

import android.widget.Button
import android.widget.TextView
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.useTopic
import com.kylecorry.sol.science.optics.Optics
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.DistanceInputView
import com.kylecorry.trail_sense.shared.views.Toolbar

class ToolLightFragment : TrailSenseReactiveFragment(R.layout.fragment_tool_light) {

    override fun update() {
        // Views
        val titleView = useView<Toolbar>(R.id.light_title)
        val beamDistanceTextView = useView<TextView>(R.id.beam_distance_text)
        val lightChartView = useView<LightBarView>(R.id.light_chart)
        val beamDistanceView = useView<DistanceInputView>(R.id.beam_distance)
        val resetButtonView = useView<Button>(R.id.reset_btn)

        // Services
        val sensors = useService<SensorService>()
        val formatter = useService<FormatService>()
        val lightSensor = useMemo(sensors) { sensors.getLightSensor() }

        // State
        val lux = useTopic(lightSensor, lightSensor.illuminance) { it.illuminance }
        val (maxLux, setMaxLux) = useState(0f)
        val (distance, setDistance) = useState<Distance?>(null)
        val candela = useMemo(maxLux, distance) {
            distance?.let { Optics.luxToCandela(maxLux, it) }
        }

        // Tracks the peak reading. Resetting maxLux to 0 re-seeds it from the current reading.
        useEffect(lux, maxLux) {
            if (lux > maxLux) {
                setMaxLux(lux)
            }
        }

        // View - Inputs
        useEffect(beamDistanceView, resetButtonView, lightChartView) {
            beamDistanceView.units =
                formatter.sortDistanceUnits(DistanceUtils.hikingDistanceUnits)
            beamDistanceView.setOnValueChangeListener {
                setMaxLux(0f)
                setDistance(it)
            }
            resetButtonView.setOnClickListener {
                setMaxLux(0f)
            }
        }

        // View - Title
        useEffect(titleView, lux) {
            titleView.title.text = formatter.formatLux(lux)
        }

        useEffect(titleView, candela) {
            titleView.subtitle.text = candela?.let { formatter.formatCandela(it) } ?: ""
        }

        // View - Beam
        useEffect(lightChartView, distance) {
            distance?.let { lightChartView.setDistanceUnits(it.units) }
        }

        useEffect(beamDistanceTextView, lightChartView, candela, distance) {
            if (candela == null || distance == null) {
                beamDistanceTextView.text = ""
                lightChartView.setCandela(0f)
                return@useEffect
            }

            val beamDistance = Optics.lightBeamDistance(candela).convertTo(distance.units)
            beamDistanceTextView.text = getString(
                R.string.beam_distance,
                formatter.formatDistance(beamDistance)
            )
            lightChartView.setCandela(candela)
        }
    }
}
