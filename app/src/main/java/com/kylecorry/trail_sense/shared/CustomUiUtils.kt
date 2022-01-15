package com.kylecorry.trail_sense.shared

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.views.*
import com.kylecorry.trail_sense.tools.qr.ui.ScanQRBottomSheet
import com.kylecorry.trail_sense.tools.qr.ui.ViewQRBottomSheet
import java.time.*
import java.util.*

object CustomUiUtils {

    fun setButtonState(button: ImageButton, state: Boolean) {
        setButtonState(
            button,
            state,
            Resources.getAndroidColorAttr(button.context, R.attr.colorPrimary),
            Resources.color(button.context, R.color.colorSecondary)
        )
    }

    private fun setButtonState(
        button: ImageButton,
        isOn: Boolean,
        @ColorInt primaryColor: Int,
        @ColorInt secondaryColor: Int
    ) {
        if (isOn) {
            setImageColor(button.drawable, secondaryColor)
            button.backgroundTintList = ColorStateList.valueOf(primaryColor)
        } else {
            setImageColor(button.drawable, Resources.androidTextColorSecondary(button.context))
            button.backgroundTintList =
                ColorStateList.valueOf(Resources.androidBackgroundColorSecondary(button.context))
        }
    }


    fun setButtonState(
        button: Button,
        isOn: Boolean
    ) {
        if (isOn) {
            button.setTextColor(
                Resources.color(button.context, R.color.colorSecondary)
            )
            button.backgroundTintList = ColorStateList.valueOf(
                Resources.getAndroidColorAttr(button.context, R.attr.colorPrimary)
            )
        } else {
            button.setTextColor(Resources.androidTextColorSecondary(button.context))
            button.backgroundTintList =
                ColorStateList.valueOf(Resources.androidBackgroundColorSecondary(button.context))
        }
    }

    @ColorInt
    fun getQualityColor(quality: Quality): Int {
        return when (quality) {
            Quality.Poor, Quality.Unknown -> AppColor.Red.color
            Quality.Moderate -> AppColor.Yellow.color
            Quality.Good -> AppColor.Green.color
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
                    Alerts.dialog(
                        activity,
                        activity.getString(R.string.unsaved_changes),
                        activity.getString(R.string.unsaved_changes_message),
                        okText = activity.getString(R.string.dialog_leave)
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
        showFeetAndInches: Boolean = false,
        onDistancePick: (distance: Distance?, cancelled: Boolean) -> Unit
    ) {
        val view = View.inflate(context, R.layout.view_distance_entry_prompt, null)
        var distance: Distance? = default
        val distanceInput = view.findViewById<DistanceInputView>(R.id.prompt_distance)
        distanceInput?.setOnValueChangeListener {
            distance = it
        }
        distanceInput?.units = units
        distanceInput?.value = default
        if (default == null) {
            distanceInput?.unit = units.firstOrNull()
        }

        distanceInput?.showFeetAndInches = showFeetAndInches

        Alerts.dialog(
            context,
            title,
            contentView = view
        ) { cancelled ->
            if (cancelled) {
                onDistancePick.invoke(null, true)
            } else {
                onDistancePick.invoke(distance, false)
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

        Alerts.dialog(
            context,
            title,
            contentView = view
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

        Alerts.dialog(
            context,
            title,
            contentView = view
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
            Alerts.dialog(context, title ?: "", contentView = view, okText = null) {
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
            Alerts.dialog(context, title ?: "", contentView = view, okText = null) {
                onBeaconGroupPick.invoke(beaconSelect.group)
            }
        beaconSelect?.setOnBeaconGroupChangeListener {
            onBeaconGroupPick.invoke(it)
            alert.dismiss()
        }
    }

    fun disclaimer(
        context: Context,
        title: String,
        message: String,
        shownKey: String,
        okText: String = context.getString(android.R.string.ok),
        cancelText: String = context.getString(android.R.string.cancel),
        considerShownIfCancelled: Boolean = true,
        shownValue: Boolean = true,
        onClose: (cancelled: Boolean) -> Unit = {}
    ) {
        val prefs = Preferences(context)
        if (prefs.getBoolean(shownKey) != shownValue) {
            if (considerShownIfCancelled) {
                Alerts.dialog(context, title, message, okText = okText, cancelText = null) {
                    prefs.putBoolean(shownKey, shownValue)
                    onClose(false)
                }
            } else {
                Alerts.dialog(
                    context,
                    title,
                    message,
                    okText = okText,
                    cancelText = cancelText
                ) { cancelled ->
                    if (!cancelled) {
                        prefs.putBoolean(shownKey, shownValue)
                    }
                    onClose(cancelled)
                }
            }
        } else {
            onClose(false)
        }
    }

    fun showImage(
        context: Context,
        title: String,
        @DrawableRes image: Int
    ) {
        val view = LinearLayout(context)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.CENTER
        view.layoutParams = params
        val imageView = ImageView(context)
        val imageParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )
        imageParams.gravity = Gravity.CENTER
        imageView.layoutParams = imageParams
        imageView.setImageResource(image)
        view.addView(imageView)

        Alerts.dialog(context, title, contentView = view, cancelText = null)
    }

    fun setImageColor(view: ImageView, @ColorInt color: Int?) {
        if (color == null) {
            view.clearColorFilter()
            return
        }
        view.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
    }

    fun setImageColor(drawable: Drawable, @ColorInt color: Int?) {
        if (color == null) {
            drawable.clearColorFilter()
            return
        }
        drawable.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
    }

    fun snackbar(
        fragment: Fragment,
        text: String,
        duration: Int = Snackbar.LENGTH_SHORT,
        action: String? = null,
        onAction: () -> Unit = {}
    ): Snackbar {
        return Snackbar.make(fragment.requireView(), text, duration).also {
            if (action != null) {
                it.setAction(action) {
                    onAction()
                }
            }
            it.setAnchorView(R.id.bottom_navigation)
            it.show()
        }
    }

    fun showQR(
        fragment: Fragment,
        title: String,
        qr: String
    ): BottomSheetDialogFragment {
        val sheet = ViewQRBottomSheet(title, qr)
        sheet.show(fragment)
        return sheet
    }

    fun scanQR(
        fragment: Fragment,
        title: String,
        onScan: (text: String?) -> Boolean
    ): BottomSheetDialogFragment {
        val sheet = ScanQRBottomSheet(title, onScan)
        sheet.show(fragment)
        return sheet
    }

    fun pickDate(
        fragment: Fragment,
        default: LocalDate = LocalDate.now(),
        onDatePick: (value: LocalDate?) -> Unit
    ) {
        val datePickerDialog = MaterialDatePicker.Builder.datePicker()
            .setSelection(default.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli())
            .build()

        datePickerDialog.addOnCancelListener {
            onDatePick.invoke(null)
        }
        datePickerDialog.addOnDismissListener {
            onDatePick.invoke(null)
        }
        datePickerDialog.addOnPositiveButtonClickListener {
            val millis = datePickerDialog.selection
            if (millis == null) {
                onDatePick.invoke(null)
                return@addOnPositiveButtonClickListener
            }
            val instant = Instant.ofEpochMilli(millis)
            onDatePick.invoke(LocalDateTime.ofInstant(instant, ZoneId.of("UTC")).toLocalDate())
        }

        datePickerDialog.show(
            fragment.requireActivity().supportFragmentManager,
            fragment.javaClass.name
        )
    }

    fun pickTime(
        fragment: Fragment,
        use24Hours: Boolean,
        default: LocalTime = LocalTime.now(),
        onTimePick: (value: LocalTime?) -> Unit
    ) {
        val timePicker = MaterialTimePicker.Builder()
            .setHour(default.hour)
            .setMinute(default.minute)
            .setTimeFormat(if (use24Hours) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
            .build()

        timePicker.addOnCancelListener {
            onTimePick.invoke(null)
        }
        timePicker.addOnDismissListener {
            onTimePick.invoke(null)
        }
        timePicker.addOnPositiveButtonClickListener {
           onTimePick.invoke(LocalTime.of(timePicker.hour, timePicker.minute))
        }

        timePicker.show(
            fragment.requireActivity().supportFragmentManager,
            fragment.javaClass.name
        )
    }

    fun pickDatetime(
        fragment: Fragment,
        use24Hours: Boolean,
        default: LocalDateTime = LocalDateTime.now(),
        onDatetimePick: (value: LocalDateTime?) -> Unit
    ) {
        pickDate(fragment, default.toLocalDate()) { date ->
            if (date != null) {
                pickTime(fragment, use24Hours, default.toLocalTime()) { time ->
                    if (time != null) {
                        onDatetimePick(LocalDateTime.of(date, time))
                    } else {
                        onDatetimePick(null)
                    }
                }
            } else {
                onDatetimePick(null)
            }
        }
    }

}