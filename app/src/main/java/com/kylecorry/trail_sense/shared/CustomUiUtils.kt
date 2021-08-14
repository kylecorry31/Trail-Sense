package com.kylecorry.trail_sense.shared

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.*
import com.kylecorry.andromeda.core.units.Coordinate
import com.kylecorry.andromeda.core.math.toDoubleCompat
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.domain.navigation.BeaconGroup
import com.kylecorry.andromeda.core.units.Distance
import com.kylecorry.andromeda.core.units.DistanceUnits
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import java.time.Duration

object CustomUiUtils {

    fun setButtonState(button: ImageButton, state: Boolean) {
        setButtonState(
            button,
            state,
            UiUtils.color(button.context, R.color.colorPrimary),
            UiUtils.color(button.context, R.color.colorSecondary)
        )
    }

    private fun setButtonState(
        button: ImageButton,
        isOn: Boolean,
        @ColorInt primaryColor: Int,
        @ColorInt secondaryColor: Int
    ){
        if (isOn) {
            button.drawable.colorFilter = PorterDuffColorFilter(secondaryColor, PorterDuff.Mode.SRC_IN)
            button.backgroundTintList = ColorStateList.valueOf(primaryColor)
        } else {
            button.drawable.colorFilter = PorterDuffColorFilter(UiUtils.androidTextColorSecondary(button.context), PorterDuff.Mode.SRC_IN)
            button.backgroundTintList =
                ColorStateList.valueOf(UiUtils.androidBackgroundColorSecondary(button.context))
        }
    }


    fun setButtonState(
        button: Button,
        isOn: Boolean
    ) {
        if (isOn) {
            button.setTextColor(UiUtils.color(button.context, R.color.colorSecondary))
            button.backgroundTintList =
                ColorStateList.valueOf(UiUtils.color(button.context, R.color.colorPrimary))
        } else {
            button.setTextColor(UiUtils.androidTextColorSecondary(button.context))
            button.backgroundTintList =
                ColorStateList.valueOf(UiUtils.androidBackgroundColorSecondary(button.context))
        }
    }

    @ColorInt
    fun getQualityColor(context: Context, quality: Quality): Int {
        return when (quality) {
            Quality.Poor, Quality.Unknown -> UiUtils.color(context, R.color.red)
            Quality.Moderate -> UiUtils.color(context, R.color.yellow)
            Quality.Good -> UiUtils.color(context, R.color.green)
        }
    }

    fun promptIfUnsavedChanges(
        activity: FragmentActivity,
        owner: LifecycleOwner,
        hasChanges: () -> Boolean
    ): OnBackPressedCallback {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (hasChanges()) {
                    UiUtils.alertWithCancel(
                        activity,
                        activity.getString(R.string.unsaved_changes),
                        activity.getString(R.string.unsaved_changes_message),
                        activity.getString(R.string.dialog_leave),
                        activity.getString(R.string.dialog_cancel)
                    ) { cancelled ->
                        if (!cancelled) {
                            remove()
                            activity.onBackPressed()
                        }
                    }
                } else {
                    remove()
                    activity.onBackPressed()
                }
            }
        }

        activity.onBackPressedDispatcher.addCallback(owner, callback)

        return callback
    }

    fun pickDistance(
        context: Context,
        units: List<DistanceUnits>,
        default: Distance? = null,
        title: String,
        onDistancePick: (distance: Distance?) -> Unit
    ) {
        val view = View.inflate(context, R.layout.view_distance_entry_prompt, null)
        var distance: Distance? = default
        val distanceInput = view.findViewById<DistanceInputView>(R.id.prompt_distance)
        distanceInput?.setOnDistanceChangeListener {
            distance = it
        }
        distanceInput?.units = units
        distanceInput?.updateDistance(default)

        UiUtils.alertViewWithCancel(
            context,
            title,
            view,
            context.getString(R.string.dialog_ok),
            context.getString(R.string.dialog_cancel)
        ) { cancelled ->
            if (cancelled) {
                onDistancePick.invoke(null)
            } else {
                onDistancePick.invoke(distance)
            }
        }
    }

    fun <T> pickItem(
        context: Context,
        items: List<T>,
        labels: List<String>,
        default: T? = null,
        title: String,
        onItemPick: (item: T?) -> Unit
    ) {
        val view = View.inflate(context, R.layout.view_dropdown_entry_prompt, null)
        val spinner = view.findViewById<Spinner>(R.id.prompt_spinner)
        val adapter = ArrayAdapter(
            context,
            R.layout.spinner_item_plain,
            R.id.item_name,
            labels)
        spinner.prompt = title
        spinner.adapter = adapter
        if (default != null){
            val idx = items.indexOf(default)
            if (idx != -1) {
                spinner.setSelection(idx)
            }
        }
        UiUtils.alertViewWithCancel(
            context,
            title,
            view,
            context.getString(R.string.dialog_ok),
            context.getString(R.string.dialog_cancel)
        ) { cancelled ->
            if (cancelled) {
                onItemPick.invoke(null)
            } else {
                val idx = spinner.selectedItemPosition
                if (idx == -1){
                    onItemPick.invoke(null)
                } else {
                    onItemPick.invoke(items[idx])
                }
            }
        }
    }

    fun pickColor(
        context: Context,
        default: AppColor? = null,
        title: String,
        onColorPick: (color: AppColor?) -> Unit
    ) {
        val view = View.inflate(context, R.layout.view_color_picker_prompt, null)
        val colorPicker = view.findViewById<ColorPickerView>(R.id.prompt_color_picker)
        var color = default
        colorPicker?.setOnColorChangeListener {
            color = it
        }
        colorPicker?.color = color

        UiUtils.alertViewWithCancel(
            context,
            title,
            view,
            context.getString(R.string.dialog_ok),
            context.getString(R.string.dialog_cancel)
        ) { cancelled ->
            if (cancelled) {
                onColorPick.invoke(null)
            } else {
                onColorPick.invoke(color)
            }
        }
    }

    fun pickDuration(
        context: Context,
        default: Duration? = null,
        title: String,
        message: String? = null,
        onDurationPick: (duration: Duration?) -> Unit
    ) {
        val view = View.inflate(context, R.layout.view_duration_entry_prompt, null)
        var duration: Duration? = default
        val durationMessage = view.findViewById<TextView>(R.id.prompt_duration_message)
        val durationInput = view.findViewById<DurationInputView>(R.id.prompt_duration)

        durationMessage.isVisible = !message.isNullOrBlank()
        durationMessage.text = message

        durationInput?.setOnDurationChangeListener {
            duration = it
        }
        durationInput?.updateDuration(default)

        UiUtils.alertViewWithCancel(
            context,
            title,
            view,
            context.getString(R.string.dialog_ok),
            context.getString(R.string.dialog_cancel)
        ) { cancelled ->
            if (cancelled) {
                onDurationPick.invoke(null)
            } else {
                onDurationPick.invoke(duration)
            }
        }
    }

    fun pickBeacon(
        context: Context,
        title: String?,
        location: Coordinate,
        onBeaconPick: (beacon: Beacon?) -> Unit
    ) {
        val view = View.inflate(context, R.layout.view_beacon_select_prompt, null)
        val beaconSelect = view.findViewById<BeaconSelectView>(R.id.prompt_beacons)
        beaconSelect.location = location
        val alert =
            UiUtils.alertView(context, title, view, context.getString(R.string.dialog_cancel)) {
                onBeaconPick.invoke(beaconSelect.beacon)
            }
        beaconSelect?.setOnBeaconChangeListener {
            onBeaconPick.invoke(it)
            alert.dismiss()
        }
    }

    fun pickBeaconGroup(
        context: Context,
        title: String?,
        onBeaconGroupPick: (group: BeaconGroup?) -> Unit
    ) {
        val view = View.inflate(context, R.layout.view_beacon_group_select_prompt, null)
        val beaconSelect = view.findViewById<BeaconGroupSelectView>(R.id.prompt_beacon_groups)
        val alert =
            UiUtils.alertView(context, title, view, context.getString(R.string.dialog_cancel)) {
                onBeaconGroupPick.invoke(beaconSelect.group)
            }
        beaconSelect?.setOnBeaconGroupChangeListener {
            onBeaconGroupPick.invoke(it)
            alert.dismiss()
        }
    }

    fun pickText(
        context: Context,
        title: String?,
        description: String?,
        default: String?,
        hint: String? = null,
        onTextEnter: (text: String?) -> Unit
    ) {
        val layout = FrameLayout(context)
        val editTextView = EditText(context)
        editTextView.setText(default)
        editTextView.hint = hint
        layout.setPadding(64, 0, 64, 0)
        layout.addView(editTextView)

        val builder = AlertDialog.Builder(context)
        builder.apply {
            setTitle(title)
            if (description != null) {
                setMessage(description)
            }
            setView(layout)
            setPositiveButton(context.getString(R.string.dialog_ok)) { dialog, _ ->
                onTextEnter.invoke(editTextView.text.toString())
                dialog.dismiss()
            }
            setNegativeButton(context.getString(R.string.dialog_cancel)) { dialog, _ ->
                onTextEnter.invoke(null)
                dialog.dismiss()
            }
        }

        val dialog = builder.create()
        dialog.show()
    }

    fun pickNumber(
        context: Context,
        title: String?,
        description: String?,
        default: Number?,
        allowDecimals: Boolean = true,
        allowNegative: Boolean = false,
        hint: String? = null,
        onNumberEnter: (number: Number?) -> Unit
    ) {
        val layout = FrameLayout(context)
        val editTextView = EditText(context)
        if (default != null) {
            editTextView.setText(default.toString())
        }
        editTextView.inputType = InputType.TYPE_CLASS_NUMBER or
                (if (allowDecimals) InputType.TYPE_NUMBER_FLAG_DECIMAL else 0) or
                (if (allowNegative) InputType.TYPE_NUMBER_FLAG_SIGNED else 0)
        editTextView.hint = hint
        layout.setPadding(64, 0, 64, 0)
        layout.addView(editTextView)

        val builder = AlertDialog.Builder(context)
        builder.apply {
            setTitle(title)
            if (description != null) {
                setMessage(description)
            }
            setView(layout)
            setPositiveButton(context.getString(R.string.dialog_ok)) { dialog, _ ->
                onNumberEnter.invoke(editTextView.text.toString().toDoubleCompat())
                dialog.dismiss()
            }
            setNegativeButton(context.getString(R.string.dialog_cancel)) { dialog, _ ->
                onNumberEnter.invoke(null)
                dialog.dismiss()
            }
        }

        val dialog = builder.create()
        dialog.show()
    }


    fun disclaimer(
        context: Context,
        title: String,
        message: String,
        shownKey: String,
        okText: String = context.getString(R.string.dialog_ok),
        cancelText: String = context.getString(R.string.dialog_cancel),
        considerShownIfCancelled: Boolean = true,
        onClose: (cancelled: Boolean) -> Unit = {}
    ) {
        val cache = Preferences(context)
        if (cache.getBoolean(shownKey) != true) {
            if (considerShownIfCancelled) {
                UiUtils.alert(context, title, message, okText) {
                    cache.putBoolean(shownKey, true)
                    onClose(false)
                }
            } else {
                UiUtils.alertWithCancel(context, title, message, okText, cancelText) { cancelled ->
                    if (!cancelled) {
                        cache.putBoolean(shownKey, true)
                    }
                    onClose(cancelled)
                }
            }
        } else {
            onClose(false)
        }
    }

}