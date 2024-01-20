package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.google.android.material.materialswitch.MaterialSwitch
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.fragments.AndromedaActivity
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.sol.units.Bearing
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.camera.SightingCompassBottomSheetFragment
import com.kylecorry.trail_sense.shared.permissions.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.shared.sensors.SensorService

class BearingInputView(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {

    private val formatter by lazy { FormatService.getInstance(getContext()) }
    private val sensors by lazy { SensorService(getContext()) }
    private val compass by lazy { sensors.getCompass() }
    private val hasCompass by lazy { sensors.hasCompass() }

    var bearing: Float? = null
        set(value) {
            field = value
            if (value == null) {
                bearingTxt.text = context.getString(R.string.direction_not_set)
                clearBtn.isVisible = false
                northReferenceBadge.isVisible = false
            } else {
                bearingTxt.text = formatter.formatDegrees(value, replace360 = true)
                clearBtn.isVisible = true
                northReferenceBadge.isVisible = true
            }
            onChange()
        }

    var trueNorth: Boolean = false
        set(value) {
            field = value
            northReferenceBadge.useTrueNorth = value
            onChange()
        }

    private var changeListener: ((bearing: Float?, isTrueNorth: Boolean) -> Unit)? = null

    private val bearingTxt: TextView
    private val compassBtn: ImageButton
    private val cameraBtn: ImageButton
    private val compassText: TextView
    private val manualEntryBtn: TextView
    private val clearBtn: ImageButton
    private val northReferenceBadge: NorthReferenceBadge

    private var cameraSheet: SightingCompassBottomSheetFragment? = null


    init {
        inflate(context, R.layout.view_bearing_input, this)

        bearingTxt = findViewById(R.id.bearing)
        compassBtn = findViewById(R.id.compass_btn)
        cameraBtn = findViewById(R.id.camera_btn)
        compassText = findViewById(R.id.compass_bearing)
        manualEntryBtn = findViewById(R.id.manual_bearing)
        clearBtn = findViewById(R.id.clear_btn)
        northReferenceBadge = findViewById(R.id.north_reference_badge)

        CustomUiUtils.setButtonState(compassBtn, true)

        manualEntryBtn.setOnClickListener {
            pickManualBearing()
        }

        clearBtn.setOnClickListener {
            bearing = null
            trueNorth = false
            northReferenceBadge.useTrueNorth = false
        }

        compassBtn.setOnClickListener {
            bearing = compass.rawBearing
            trueNorth = false
            northReferenceBadge.useTrueNorth = false
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
                            northReferenceBadge.useTrueNorth = false
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
        // Note: This does not dismiss the sheet - if the sheet is visible, the camera is already listening to state changes
    }

    private fun onChange() {
        changeListener?.invoke(bearing, trueNorth)
    }

    fun setOnBearingChangeListener(listener: ((bearing: Float?, isTrueNorth: Boolean) -> Unit)?) {
        changeListener = listener
    }

    private fun pickManualBearing() {
        val view = View.inflate(context, R.layout.view_direction_manual_picker, null)
        var chosenBearing = bearing
        var chosenTrueNorth = trueNorth

        val bearingInputView = view.findViewById<EditText>(R.id.bearing)
        bearingInputView.addTextChangedListener {
            val text = bearingInputView.text?.toString()
            chosenBearing = text?.toFloatOrNull()?.let { Bearing.getBearing(it) }
        }
        bearingInputView.setText(chosenBearing?.let { DecimalFormatter.format(it, 1) })

        val trueNorthSwitch = view.findViewById<MaterialSwitch>(R.id.true_north)
        trueNorthSwitch.isChecked = chosenTrueNorth
        trueNorthSwitch.setOnCheckedChangeListener { _, isChecked ->
            chosenTrueNorth = isChecked
        }

        Alerts.dialog(
            context,
            context.getString(R.string.direction),
            contentView = view
        ) { cancelled ->
            if (!cancelled) {
                bearing = chosenBearing
                trueNorth = chosenTrueNorth
                northReferenceBadge.useTrueNorth = chosenTrueNorth
            }
        }
    }

}