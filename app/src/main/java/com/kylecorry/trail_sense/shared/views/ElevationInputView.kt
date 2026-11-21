package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.google.android.material.button.MaterialButton
import androidx.core.view.isVisible
import com.google.android.material.loadingindicator.LoadingIndicator
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.sol.math.MathExtensions.roundPlaces
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.tools.beacons.infrastructure.BeaconPickers
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.ClosestBeaconSort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ElevationInputView(context: Context?, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {

    private val sensorService by lazy { SensorService(getContext()) }
    private val altimeter by lazy { sensorService.getAltimeter(tag = "ElevationInputView") }
    private val location by lazy { LocationSubsystem.getInstance(getContext()) }
    private val formatter by lazy { FormatService.getInstance(getContext()) }

    var elevation: Distance?
        get() = elevationInput.value
        set(value) {
            elevationInput.value = value
        }

    var hint: CharSequence?
        get() = elevationInput.hint
        set(value) {
            elevationInput.hint = value
        }

    private var changeListener: ((elevation: Distance?) -> Unit)? = null
    private var autofillListener: (() -> Unit)? = null
    private val autofillTimeout = CoroutineTimer { pause() }

    private lateinit var elevationInput: DistanceInputView
    private lateinit var gpsBtn: MaterialButton

    private lateinit var gpsLoadingIndicator: LoadingIndicator

    init {
        context?.let {
            inflate(it, R.layout.view_elevation_input, this)
            elevationInput = findViewById(R.id.elevation_input)
            gpsLoadingIndicator = findViewById(R.id.gps_loading)
            gpsBtn = findViewById(R.id.gps_btn)

            elevationInput.defaultHint = it.getString(R.string.elevation)
            elevationInput.hint = it.getString(R.string.elevation)
            elevationInput.showFeetAndInches = false
            elevationInput.allowNegative = true
            elevationInput.units = formatter.sortDistanceUnits(DistanceUtils.elevationDistanceUnits)
            elevationInput.setOnValueChangeListener { distance ->
                changeListener?.invoke(distance)
            }

            gpsBtn.isVisible = true
            gpsLoadingIndicator.isVisible = false

            gpsBtn.setOnLongClickListener {
                autofillWithAltimeter()
                true
            }

            gpsBtn.setOnClickListener {
                Pickers.item(
                    getContext(), getContext().getString(R.string.autofill_source), listOf(
                        getContext().getString(R.string.pref_altimeter_calibration_title),
                        getContext().getString(R.string.beacon)
                    ), 0
                ) {
                    when (it) {
                        0 -> autofillWithAltimeter()
                        1 -> autofillWithBeacon()
                    }
                }
            }
        }
    }

    private fun autofillWithAltimeter() {
        autofillListener?.invoke()
        autofill()
    }

    private fun autofillWithBeacon() {
        CoroutineScope(Dispatchers.Main).launch {
            val beacon = BeaconPickers.pickBeacon(
                context, sort = ClosestBeaconSort(
                    BeaconService(context), location::location
                )
            ) { beacons ->
                beacons.filter { beacon ->
                    beacon is BeaconGroup || (beacon is Beacon && beacon.elevation != null)
                }
            } ?: return@launch
            changeElevation(beacon.elevation?.let { ele -> Distance.meters(ele) })
        }
    }

    private fun changeElevation(elevation: Distance?) {
        val converted = elevation?.convertTo(elevationInput.unit ?: DistanceUnits.Meters)
        val rounded = converted?.let {
            Distance.from(
                converted.value.roundPlaces(Units.getDecimalPlaces(converted.units)),
                converted.units
            )
        }
        elevationInput.value = rounded
    }

    private fun onAltimeterUpdate(): Boolean {
        autofillTimeout.stop()
        changeElevation(Distance.meters(altimeter.altitude))
        gpsBtn.visibility = View.VISIBLE
        gpsLoadingIndicator.visibility = View.GONE
        elevationInput.isEnabled = true
        return false
    }

    fun autofill() {
        if (!gpsBtn.isVisible) return
        gpsBtn.isVisible = false
        gpsLoadingIndicator.isVisible = true
        elevationInput.isEnabled = false
        autofillTimeout.once(SensorService.GPS_READ_TIMEOUT)
        altimeter.start(this::onAltimeterUpdate)
    }

    fun pause() {
        autofillTimeout.stop()
        altimeter.stop(this::onAltimeterUpdate)
        gpsBtn.isVisible = true
        gpsLoadingIndicator.isVisible = false
        elevationInput.isEnabled = true
    }

    fun setOnAutoElevationClickListener(listener: (() -> Unit)?) {
        autofillListener = listener
    }

    fun setOnElevationChangeListener(listener: ((elevation: Distance?) -> Unit)?) {
        changeListener = listener
    }


}
