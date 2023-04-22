package com.kylecorry.trail_sense.shared

import android.content.Context
import android.net.Uri
import android.util.Size
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.core.ui.setState
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.ceres.chart.Chart
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconIcon
import com.kylecorry.trail_sense.shared.camera.PhotoImportBottomSheetFragment
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.BeaconIconPickerView
import com.kylecorry.trail_sense.shared.views.ColorPickerView
import com.kylecorry.trail_sense.shared.views.DistanceInputView
import com.kylecorry.trail_sense.shared.views.DurationInputView
import com.kylecorry.trail_sense.tools.qr.ui.ScanQRBottomSheet
import com.kylecorry.trail_sense.tools.qr.ui.ViewQRBottomSheet
import java.time.Duration
import java.time.LocalDateTime
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object CustomUiUtils {

    fun setButtonState(button: ImageButton, state: Boolean) {
        button.setState(
            state,
            Resources.getAndroidColorAttr(button.context, R.attr.colorPrimary),
            Resources.color(button.context, R.color.colorSecondary)
        )
    }


    fun setButtonState(
        button: Button,
        isOn: Boolean
    ) {
        button.setState(
            isOn,
            Resources.getAndroidColorAttr(button.context, R.attr.colorPrimary),
            Resources.color(button.context, R.color.colorSecondary)
        )
    }

    @ColorInt
    fun getQualityColor(quality: Quality): Int {
        return when (quality) {
            Quality.Poor, Quality.Unknown -> AppColor.Red.color
            Quality.Moderate -> AppColor.Yellow.color
            Quality.Good -> AppColor.Green.color
        }
    }

    fun pickDistance(
        context: Context,
        units: List<DistanceUnits>,
        default: Distance? = null,
        title: String,
        showFeetAndInches: Boolean = false,
        hint: String = context.getString(R.string.distance),
        onDistancePick: (distance: Distance?, cancelled: Boolean) -> Unit
    ) {
        val view = View.inflate(context, R.layout.view_distance_entry_prompt, null)
        var distance: Distance? = default
        val distanceInput = view.findViewById<DistanceInputView>(R.id.prompt_distance)
        distanceInput?.defaultHint = hint
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

    // TODO: Make this generic - pick icon, takes in a list of resouces, returns selected (and allows user to pick none)
    fun pickBeaconIcon(
        context: Context,
        default: BeaconIcon? = null,
        title: String,
        onIconPick: (icon: BeaconIcon?) -> Unit
    ) {
        val view = View.inflate(context, R.layout.view_beacon_icon_picker_prompt, null)
        val iconPicker = view.findViewById<BeaconIconPickerView>(R.id.prompt_icon_picker)
        var icon = default
        iconPicker?.setOnIconChangeListener {
            icon = it
        }
        iconPicker?.icon = icon

        Alerts.dialog(
            context,
            title,
            contentView = view
        ) { cancelled ->
            if (cancelled) {
                onIconPick.invoke(null)
            } else {
                onIconPick.invoke(icon)
            }
        }
    }

    fun pickDuration(
        context: Context,
        default: Duration? = null,
        title: String,
        message: String? = null,
        showSeconds: Boolean = false,
        onDurationPick: (duration: Duration?) -> Unit
    ) {
        val view = View.inflate(context, R.layout.view_duration_entry_prompt, null)
        var duration: Duration? = default
        val durationMessage = view.findViewById<TextView>(R.id.prompt_duration_message)
        val durationInput = view.findViewById<DurationInputView>(R.id.prompt_duration)

        durationMessage.isVisible = !message.isNullOrBlank()
        durationMessage.text = message

        durationInput.showSeconds = showSeconds

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

    fun disclaimer(
        context: Context,
        title: String,
        message: CharSequence,
        shownKey: String,
        okText: String = context.getString(android.R.string.ok),
        cancelText: String? = context.getString(android.R.string.cancel),
        considerShownIfCancelled: Boolean = true,
        shownValue: Boolean = true,
        onClose: (cancelled: Boolean, agreed: Boolean) -> Unit = { _, _ -> }
    ) {
        val prefs = PreferencesSubsystem.getInstance(context).preferences
        if (prefs.getBoolean(shownKey) != shownValue) {
            if (considerShownIfCancelled && cancelText == null) {
                Alerts.dialog(context, title, message, okText = okText, cancelText = null) {
                    prefs.putBoolean(shownKey, shownValue)
                    onClose(it, true)
                }
            } else {
                Alerts.dialog(
                    context,
                    title,
                    message,
                    okText = okText,
                    cancelText = cancelText
                ) { cancelled ->
                    val agreed = !cancelled || considerShownIfCancelled
                    if (agreed) {
                        prefs.putBoolean(shownKey, shownValue)
                    }
                    onClose(cancelled, agreed)
                }
            }
        } else {
            onClose(false, true)
        }
    }

    fun setImageColor(textView: TextView, @ColorInt color: Int?) {
        textView.compoundDrawables.forEach {
            it?.let { Colors.setImageColor(it, color) }
        }
    }

    fun snackbar(
        fragment: Fragment,
        text: String,
        duration: Int = Snackbar.LENGTH_SHORT,
        action: String? = null,
        onAction: () -> Unit = {}
    ): Snackbar {
        return Alerts.snackbar(
            fragment,
            R.id.bottom_navigation,
            text,
            duration,
            action,
            onAction
        )
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

    suspend fun takePhoto(fragment: AndromedaFragment, size: Size? = null): Uri? =
        suspendCoroutine { cont ->
            takePhoto(fragment, size) {
                cont.resume(it)
            }
        }

    private fun takePhoto(
        fragment: AndromedaFragment,
        size: Size? = null,
        onCapture: (uri: Uri?) -> Unit
    ) {
        fragment.requestCamera {
            if (!it) {
                onCapture(null)
                return@requestCamera
            }

            val sheet = PhotoImportBottomSheetFragment(size) { uri ->
                onCapture(uri)
            }

            sheet.show(fragment)
        }
    }

    fun pickDatetime(
        context: Context,
        use24Hours: Boolean,
        default: LocalDateTime = LocalDateTime.now(),
        onDatetimePick: (value: LocalDateTime?) -> Unit
    ) {
        Pickers.date(context, default.toLocalDate()) { date ->
            if (date != null) {
                Pickers.time(context, use24Hours, default.toLocalTime()) { time ->
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

    fun showChart(
        fragment: Fragment,
        title: String,
        content: CharSequence? = null,
        populateFn: (Chart) -> Unit
    ) {
        val chartView = View.inflate(fragment.requireContext(), R.layout.view_chart_prompt, null)

        if (content != null) {
            chartView.findViewById<TextView>(R.id.content).text = content
        } else {
            chartView.findViewById<TextView>(R.id.content).isVisible = false
        }

        populateFn(chartView.findViewById(R.id.chart))
        Alerts.dialog(
            fragment.requireContext(),
            title,
            contentView = chartView,
            cancelText = null
        )
    }

}