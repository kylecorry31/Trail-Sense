package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.kylecorry.andromeda.fragments.AndromedaActivity
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.sol.units.Bearing
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.camera.SightingCompassBottomSheetFragment
import com.kylecorry.trail_sense.shared.extensions.floatValue
import com.kylecorry.trail_sense.shared.permissions.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.shared.sensors.SensorService
import kotlin.math.roundToInt

class BearingInputView(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {

    private val formatter by lazy { FormatService.getInstance(getContext()) }
    private val sensors by lazy { SensorService(getContext()) }
    private val compass by lazy { sensors.getCompass() }
    private val hasCompass by lazy { sensors.hasCompass() }

    var bearing: Bearing?
        get() = _bearing
        set(value) {
            _bearing = value
            if (value == null) {
                bearingEdit.setText("")
            } else {
                val degrees = value.value.roundToInt() % 360
                bearingEdit.setText(degrees.toString())
            }
        }

    @Suppress("MemberVisibilityCanBePrivate")
    var trueNorth: Boolean
        get() = trueNorthSwitch.isChecked
        set(value) {
            trueNorthSwitch.isChecked = value
        }

    private var _bearing: Bearing? = null

    private var changeListener: ((bearing: Bearing?, isTrueNorth: Boolean) -> Unit)? = null

    private val bearingEdit: EditText
    private val compassBtn: ImageButton
    private val cameraBtn: ImageButton
    private val compassText: TextView
    private val trueNorthSwitch: SwitchCompat

    private var cameraSheet: SightingCompassBottomSheetFragment? = null


    init {
        inflate(context, R.layout.view_bearing_input, this)

        bearingEdit = findViewById(R.id.bearing)
        compassBtn = findViewById(R.id.compass_btn)
        cameraBtn = findViewById(R.id.camera_btn)
        compassText = findViewById(R.id.compass_bearing)
        trueNorthSwitch = findViewById(R.id.true_north)

        bearingEdit.addTextChangedListener {
            onChange()
        }

        trueNorthSwitch.setOnCheckedChangeListener { _, _ -> onChange() }

        compassBtn.setOnClickListener {
            bearing = compass.bearing
            trueNorth = false
        }

        cameraBtn.setOnClickListener {
            val activity = context as? AndromedaActivity ?: return@setOnClickListener
            val fragment = activity.getFragment() as? AndromedaFragment ?: return@setOnClickListener

            fragment.requestCamera { hasPermission ->
                if (hasPermission) {
                    if (cameraSheet == null) {
                        cameraSheet = SightingCompassBottomSheetFragment {
                            bearing = it
                            trueNorth = false
                        }
                    }

                    cameraSheet?.show(fragment)
                } else {
                    fragment.alertNoCameraPermission()
                }
            }
        }

        if (!hasCompass) {
            findViewById<View>(R.id.compass_autofill_holder).isVisible = false
            cameraBtn.isVisible = false
        }
    }

    fun start() {
        stop()
        compass.start {
            val bearing = compass.rawBearing
            compassText.text = formatter.formatDegrees(bearing, replace360 = true)
            cameraSheet?.bearing = bearing
            true
        }
    }

    fun stop() {
        compass.stop(null)
        cameraSheet?.dismiss()
    }

    private fun onChange() {
        val degrees = bearingEdit.floatValue()
        _bearing = degrees?.let { Bearing(it) }
        changeListener?.invoke(_bearing, trueNorthSwitch.isChecked)
    }

    fun setOnBearingChangeListener(listener: ((bearing: Bearing?, isTrueNorth: Boolean) -> Unit)?) {
        changeListener = listener
    }

}