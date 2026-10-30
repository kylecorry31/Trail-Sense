package com.kylecorry.trail_sense.tools.navigation.ui

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.math.trigonometry.Trigonometry.deltaAngle
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.extensions.flatten
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.sensors.NavigationSensorValues
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationService
import com.kylecorry.trail_sense.tools.navigation.domain.guidance.Guidance
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.roundToInt

class NavigationSheetView(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {

    private val prefs = DependencyRegistry.get<UserPreferences>()
    private val formatter = DependencyRegistry.get<FormatService>()
    private val navigator = DependencyRegistry.get<Navigator>()
    private val hasCompass = DependencyRegistry.get<SensorService>().hasCompass()

    private val navigationService = NavigationService()

    private var destination: Destination? = null
    private var isNavigating: Boolean = false
    private var sensorValues: NavigationSensorValues? = null
    private var useTrueNorthOverride: Boolean? = null
    private var restoringJob: Job? = null

    // VIEWS
    private val textContainer: View
    private val titleView: TextView
    private val subtitleView: TextView
    private val cancelButton: MaterialButton
    private val arrowView: ImageView
    private val targetBearingView: TextView
    private val progressView: LinearProgressIndicator
    private val primaryStat: Stat
    private val secondaryStat: Stat
    private val tertiaryStat: Stat

    init {
        inflate(context, R.layout.view_navigation_sheet, this)
        textContainer = findViewById(R.id.navigation_text_container)
        titleView = findViewById(R.id.navigation_sheet_title)
        subtitleView = findViewById(R.id.navigation_subtitle)
        cancelButton = findViewById(R.id.navigation_cancel)
        arrowView = findViewById(R.id.navigation_arrow)
        targetBearingView = findViewById(R.id.navigation_target_bearing)
        progressView = findViewById(R.id.navigation_progress)
        primaryStat = Stat(
            findViewById(R.id.navigation_primary_stat),
            findViewById(R.id.navigation_primary_value),
            findViewById(R.id.navigation_primary_label)
        )
        secondaryStat = Stat(
            findViewById(R.id.navigation_secondary_stat),
            findViewById(R.id.navigation_secondary_value),
            findViewById(R.id.navigation_secondary_label)
        )
        tertiaryStat = Stat(
            findViewById(R.id.navigation_tertiary_stat),
            findViewById(R.id.navigation_tertiary_value),
            findViewById(R.id.navigation_tertiary_label)
        )
        cancelButton.flatten()
        cancelButton.setOnClickListener { cancelNavigation() }
        reserveTargetBearingWidth()
    }

    // The bearing sits next to the title, so it must not resize as the text changes
    private fun reserveTargetBearingWidth() {
        val paint = targetBearingView.paint
        val widest = CompassDirection.entries.maxOf {
            paint.measureText(
                "${formatter.formatDegrees(WIDEST_BEARING_DEGREES)} ${formatter.formatDirection(it)}"
            )
        }
        targetBearingView.minWidth = ceil(widest).toInt()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        restoringJob = CoroutineScope(Dispatchers.Main.immediate).launch {
            navigator.isRestoringNavigation.collect {
                updateNavigation()
            }
        }
    }

    override fun onDetachedFromWindow() {
        restoringJob?.cancel()
        restoringJob = null
        super.onDetachedFromWindow()
    }

    // TODO: Listen for navigation and automatically hide/show with override ability (for navigation tool)
    fun hide() {
        isVisible = false
        destination = null
        updateNavigation()
    }

    fun show(destination: Destination, isNavigating: Boolean = false) {
        this.destination = destination
        this.isNavigating = isNavigating
        updateNavigation()
    }

    fun setTrueNorthOverride(useTrueNorth: Boolean?) {
        useTrueNorthOverride = useTrueNorth
        updateNavigation()
    }

    // TODO: This should be tracked by a service (for background navigation)
    fun updateNavigationSensorValues(values: NavigationSensorValues) {
        sensorValues = values
        updateNavigation()
    }

    fun updateNavigationSensorValues(
        location: Coordinate,
        elevation: Float,
        speed: Float,
        declination: Float,
        bearing: Float
    ) {
        val values = NavigationSensorValues(
            location,
            null,
            Distance.meters(elevation),
            null,
            Bearing.from(bearing),
            declination,
            Speed.from(speed, DistanceUnits.Meters, TimeUnits.Seconds),
            Speed.from(speed, DistanceUnits.Meters, TimeUnits.Seconds)
        )
        updateNavigationSensorValues(values)
    }

    fun cancelNavigation() {
        if (!isNavigating) {
            return
        }
        val fragment = findFragment<Fragment>()
        fragment.lifecycleScope.launch {
            val restore = navigator.cancelAllNavigation()
            if (!fragment.isAdded) {
                return@launch
            }
            CustomUiUtils.snackbar(
                fragment,
                context.getString(R.string.navigation_cancelled),
                Snackbar.LENGTH_LONG,
                context.getString(R.string.undo)
            ) {
                CoroutineScope(Dispatchers.Default).launch { restore() }
            }
        }
    }

    private fun updateNavigation() {
        val values = sensorValues
        val destination = destination
        if (navigator.isRestoringNavigation.value) {
            showLoading()
            return
        }
        if (destination == null || values == null) {
            isVisible = false
            return
        }
        isVisible = true
        cancelButton.isVisible = isNavigating

        textContainer.setAction(null)
        subtitleView.isVisible = false
        progressView.isVisible = false

        val guidance = getGuidance(destination, values)
        updateDirection(guidance, values)

        when (destination) {
            is Destination.Beacon -> updateBeaconNavigation(destination, guidance, values)
            is Destination.Bearing -> updateBearingNavigation(guidance, values)
            is Destination.Path -> updatePathNavigation(destination, guidance, values)
        }
    }

    private fun showLoading() {
        isVisible = true
        titleView.text = context.getString(R.string.loading)
        titleView.contentDescription = null
        titleView.setStartIcon(null, TITLE_ICON_SIZE_RATIO)
        textContainer.setAction(null)
        subtitleView.isVisible = false
        progressView.isVisible = false
        cancelButton.isVisible = false
        arrowView.isVisible = false
        targetBearingView.isVisible = false
        primaryStat.hide()
        secondaryStat.hide()
        tertiaryStat.hide()
    }

    private fun getGuidance(destination: Destination, values: NavigationSensorValues): Guidance {
        return navigator.getGuidance(
            destination,
            values.location,
            values.elevation,
            values.declination,
            useTrueNorthOverride
        )
    }

    private fun getEta(guidance: Guidance, values: NavigationSensorValues): Duration? {
        val speed = values.speed.convertTo(
            DistanceUnits.Meters, TimeUnits.Seconds
        ).value
        val hikingSpeed = Speed.from(
            navigationService.getHikingSpeed(speed),
            DistanceUnits.Meters,
            TimeUnits.Seconds
        )
        val remainingDistance = guidance.remainingDistance ?: return null
        return HikingService.getHikingDuration(
            remainingDistance,
            guidance.remainingElevationGain ?: Distance.meters(0f),
            hikingSpeed
        )
    }

    private fun updateBeaconNavigation(
        destination: Destination.Beacon,
        guidance: Guidance,
        values: NavigationSensorValues
    ) {
        val beacon = destination.beacon
        setTitle(R.drawable.ic_location, R.string.beacon, beacon.name)
        textContainer.setAction { openBeacon(beacon.id) }
        subtitleView.isVisible = true
        subtitleView.text = formatter.join(
            formatter.formatLocation(beacon.coordinate),
            beacon.elevation?.let {
                // Keep the elevation together if the subtitle wraps
                context.getString(
                    R.string.navigation_elevation_format,
                    formatter.formatElevation(Distance.meters(it))
                ).replace(' ', NON_BREAKING_SPACE)
            },
            separator = FormatService.Separator.Space
        )

        updateRemainingStat(guidance)
        updateEtaStat(guidance, values)

        val gain = guidance.remainingElevationGain
        val loss = guidance.remainingElevationLoss
        when {
            gain != null -> tertiaryStat.show(
                formatElevationChange(gain),
                context.getString(R.string.navigation_remaining_gain),
                valueIcon = R.drawable.ic_triangle_up
            )

            loss != null -> tertiaryStat.show(
                formatElevationChange(loss),
                context.getString(R.string.navigation_remaining_loss),
                valueIcon = R.drawable.ic_triangle_down
            )

            else -> tertiaryStat.hide()
        }
    }

    private fun updateBearingNavigation(guidance: Guidance, values: NavigationSensorValues) {
        setTitle(
            R.drawable.ic_compass_icon,
            R.string.bearing,
            formatBearing(guidance.targetBearing.value)
        )

        primaryStat.show(
            guidance.traveledDistance?.let { formatRelativeDistance(it) },
            context.getString(R.string.navigation_traveled)
        )
        secondaryStat.hide()
        if (hasCompass) {
            tertiaryStat.show(
                formatBearing(values.bearing.value),
                context.getString(R.string.navigation_facing)
            )
        } else {
            tertiaryStat.hide()
        }
    }

    private fun updatePathNavigation(
        destination: Destination.Path,
        guidance: Guidance,
        values: NavigationSensorValues
    ) {
        setTitle(
            R.drawable.ic_tool_backtrack,
            R.string.path,
            destination.path.name ?: context.getString(R.string.path)
        )
        textContainer.setAction { openPath(destination.path.id) }
        progressView.isVisible = true
        progressView.progress = ((guidance.progress ?: 0f) * progressView.max).roundToInt()

        updateRemainingStat(guidance)
        updateEtaStat(guidance, values)

        val gain = guidance.remainingElevationGain ?: Distance.meters(0f)
        val loss = guidance.remainingElevationLoss ?: Distance.meters(0f)
        val gainText = formatElevationChange(gain)
        val lossText = formatElevationChange(loss)
        tertiaryStat.show(
            if (gain.value > 0 || loss.value > 0) gainText else null,
            lossText,
            valueIcon = R.drawable.ic_triangle_up,
            labelIcon = R.drawable.ic_triangle_down,
            description = "${context.getString(R.string.ascent)} $gainText, " +
                    "${context.getString(R.string.descent)} $lossText"
        )
    }

    private fun updateDirection(guidance: Guidance, values: NavigationSensorValues) {
        arrowView.isVisible = true
        targetBearingView.isVisible = true
        targetBearingView.text = formatBearing(guidance.targetBearing.value)

        if (!hasCompass) {
            arrowView.setImageResource(R.drawable.ic_compass_unavailable)
            arrowView.rotation = 0f
            arrowView.imageTintList =
                ColorStateList.valueOf(Resources.androidTextColorSecondary(context))
            return
        }

        arrowView.setImageResource(R.drawable.ic_navigation_arrow)
        val turn = deltaAngle(values.bearing.value, guidance.targetBearing.value)
        val color = getDirectionColor(turn.absoluteValue)
        arrowView.rotation = turn
        arrowView.imageTintList = ColorStateList.valueOf(color)
    }

    private fun getDirectionColor(offset: Float): Int {
        val fraction = ((offset - ON_TARGET_DEGREES) / (WRONG_DIRECTION_DEGREES - ON_TARGET_DEGREES))
            .coerceIn(0f, 1f)
        return ColorUtils.blendARGB(AppColor.Green.color, AppColor.Red.color, fraction)
    }

    private fun updateRemainingStat(guidance: Guidance) {
        primaryStat.show(
            guidance.remainingDistance?.let { formatRelativeDistance(it) },
            context.getString(R.string.navigation_remaining)
        )
    }

    private fun updateEtaStat(guidance: Guidance, values: NavigationSensorValues) {
        val duration = getEta(guidance, values)
        val arrival = duration?.let {
            formatter.formatTime(
                ZonedDateTime.now().plus(it).toLocalTime(),
                includeSeconds = false
            )
        }
        secondaryStat.show(
            duration?.let { formatter.formatDuration(it, it.toHours() >= LONG_DURATION_HOURS) },
            context.getString(R.string.navigation_arrive_format, arrival)
        )
    }

    private fun setTitle(@DrawableRes icon: Int, @StringRes typeRes: Int, text: String) {
        titleView.text = text
        titleView.contentDescription = "${context.getString(typeRes)}: $text"
        titleView.setStartIcon(icon, TITLE_ICON_SIZE_RATIO)
    }

    private fun formatBearing(degrees: Float): String {
        return formatter.formatDegrees(degrees, replace360 = true) + " " +
                formatter.formatDirection(CompassDirection.nearest(degrees))
    }

    private fun formatRelativeDistance(distance: Distance): String {
        val relative = distance.convertTo(prefs.baseDistanceUnits).toRelativeDistance()
        val decimalPlaces = if (relative.value >= LARGE_DISTANCE_THRESHOLD) {
            0
        } else {
            Units.getDecimalPlaces(relative.units)
        }
        return formatter.formatDistance(relative, decimalPlaces, false)
    }

    private fun formatElevationChange(distance: Distance): String {
        val converted = Distance.from(
            distance.convertTo(prefs.baseDistanceUnits).value.absoluteValue,
            prefs.baseDistanceUnits
        )
        return formatter.formatDistance(converted, Units.getDecimalPlaces(converted.units), false)
    }

    private fun openBeacon(id: Long) {
        findNavController().navigateWithAnimation(
            R.id.beaconDetailsFragment,
            Bundle().apply {
                putLong("beacon_id", id)
            }
        )
    }

    private fun openPath(id: Long) {
        findNavController().navigateWithAnimation(
            R.id.pathDetailsFragment,
            Bundle().apply {
                putLong("path_id", id)
            }
        )
    }

    private class Stat(
        private val container: View,
        private val value: TextView,
        private val label: TextView
    ) {
        fun show(
            valueText: String?,
            labelText: String,
            @DrawableRes valueIcon: Int? = null,
            @DrawableRes labelIcon: Int? = null,
            description: String? = null
        ) {
            container.isVisible = valueText != null
            container.contentDescription = description
            value.text = valueText
            label.text = labelText
            value.setStartIcon(valueIcon, STAT_ICON_SIZE_RATIO)
            label.setStartIcon(labelIcon, STAT_ICON_SIZE_RATIO)
        }

        fun hide() {
            container.isVisible = false
        }
    }

    companion object {
        // Within this many degrees of the target, the user is considered to be heading the right way
        private const val ON_TARGET_DEGREES = 5f

        // At or beyond this many degrees from the target, the direction is fully "wrong"
        private const val WRONG_DIRECTION_DEGREES = 45f

        // Three digits of the widest glyph
        private const val WIDEST_BEARING_DEGREES = 888f

        // Distances this large (in their displayed units) drop their decimals to keep the stat narrow
        private const val LARGE_DISTANCE_THRESHOLD = 100f

        // Durations this long drop their minutes to keep the stat narrow
        private const val LONG_DURATION_HOURS = 100L

        private const val NON_BREAKING_SPACE = ' '

        private const val STAT_ICON_SIZE_RATIO = 0.8f

        private const val TITLE_ICON_SIZE_RATIO = 1.1f
    }
}

private fun View.setAction(action: (() -> Unit)?) {
    setOnClickListener(action?.let { { it() } })
    isClickable = action != null
}

private fun TextView.setStartIcon(@DrawableRes icon: Int?, sizeRatio: Float) {
    val drawable = icon?.let { ContextCompat.getDrawable(context, it) }
    val size = (textSize * sizeRatio).roundToInt()
    drawable?.setBounds(0, 0, size, size)
    setCompoundDrawablesRelative(drawable, null, null, null)
    compoundDrawablePadding = size / 2
    TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(currentTextColor))
}
