package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.core.view.isVisible
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.sol.math.SolMath.roundPlaces
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.BeaconPickers
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.sort.ClosestBeaconSort
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * A view that allows the user to enter an elevation
 */
class ElevationInputView(context: Context?, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {

    private val sensorService by lazy { SensorService(getContext()) }
    private val altimeter by lazy { sensorService.getAltimeter() }
    private val location by lazy { LocationSubsystem.getInstance(getContext()) }
    private val formatter by lazy { FormatService.getInstance(getContext()) }

    /**
     * The entered elevation
     */
    var elevation: Distance?
        get() = elevationInput.value
        set(value) {
            elevationInput.value = value
        }

    /**
     * The hint for the elevation input
     */
    var hint: CharSequence?
        get() = elevationInput.hint
        set(value) {
            elevationInput.hint = value
        }

    private var changeListener: ((elevation: Distance?) -> Unit)? = null
    private var autofillListener: (() -> Unit)? = null

    private lateinit var elevationInput: DistanceInputView
    private lateinit var gpsBtn: ImageButton

    private lateinit var gpsLoadingIndicator: ProgressBar

    init {
        context?.let {
            // Load view
            inflate(it, R.layout.view_elevation_input, this)
            elevationInput = findViewById(R.id.elevation_input)
            gpsLoadingIndicator = findViewById(R.id.gps_loading)
            gpsBtn = findViewById(R.id.gps_btn)

            // Set up elevation input
            elevationInput.defaultHint = it.getString(R.string.elevation)
            elevationInput.hint = it.getString(R.string.elevation)
            elevationInput.showFeetAndInches = false
            elevationInput.units = formatter.sortDistanceUnits(DistanceUtils.elevationDistanceUnits)
            elevationInput.setOnValueChangeListener { distance ->
                changeListener?.invoke(distance)
            }

            // Set up buttons
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
        val rounded = converted?.copy(
            distance = converted.distance.roundPlaces(Units.getDecimalPlaces(converted.units))
        )
        elevationInput.value = rounded
    }

    private fun onAltimeterUpdate(): Boolean {
        changeElevation(Distance.meters(altimeter.altitude))
        gpsBtn.visibility = View.VISIBLE
        gpsLoadingIndicator.visibility = View.GONE
        elevationInput.isEnabled = true
        return false
    }

    /**
     * Autofill the elevation from the altimeter
     */
    fun autofill() {
        if (!gpsBtn.isVisible) return
        gpsBtn.isVisible = false
        gpsLoadingIndicator.isVisible = true
        elevationInput.isEnabled = false
        altimeter.start(this::onAltimeterUpdate)
    }

    /**
     * Pause the altimeter if it is running
     */
    fun pause() {
        altimeter.stop(this::onAltimeterUpdate)
        gpsBtn.isVisible = true
        gpsLoadingIndicator.isVisible = false
        elevationInput.isEnabled = true
    }

    /**
     * Set the listener for when the user clicks the autofill button
     */
    fun setOnAutoElevationClickListener(listener: (() -> Unit)?) {
        autofillListener = listener
    }

    /**
     * Set the listener for when the elevation changes
     */
    fun setOnElevationChangeListener(listener: ((elevation: Distance?) -> Unit)?) {
        changeListener = listener
    }


}