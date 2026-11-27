package com.kylecorry.trail_sense.tools.lightning.ui

import android.widget.TextView
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.setCompoundDrawables
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.useBackgroundEffect
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useTimer
import com.kylecorry.trail_sense.shared.views.TileButton
import com.kylecorry.trail_sense.shared.views.Toolbar
import com.kylecorry.trail_sense.tools.lightning.domain.LightningStrike
import com.kylecorry.trail_sense.tools.lightning.infrastructure.persistence.LightningRepo
import java.time.Instant

class FragmentToolLightning : TrailSenseReactiveFragment(R.layout.fragment_tool_lightning) {

    override fun update() {
        val titleView = useView<Toolbar>(R.id.lightning_title)
        val startButton = useView<TileButton>(R.id.start_btn)
        val previousStrikeView = useView<TextView>(R.id.previous_strike)

        val context = useAndroidContext()
        val formatter = useService<FormatService>()
        val prefs = useService<UserPreferences>()
        val repo = useMemo(context) { LightningRepo.getInstance(context) }
        val units = useMemo(prefs, resetOnResume) { prefs.baseDistanceUnits }

        val (lightningTime, setLightningTime) = useState<Instant?>(null)
        val (now, setNow) = useState(Instant.now())
        val (strike, setStrike) = useState<Reading<LightningStrike>?>(null)
        val (lastStrike, setLastStrike) = useState<Reading<LightningStrike>?>(null)

        useEffect(resetOnResume) {
            setLightningTime(null)
        }

        useBackgroundEffect(repo, resetOnResume) {
            setLastStrike(repo.getLast())
        }

        useTimer(20) {
            if (lightningTime != null) {
                setNow(Instant.now())
            }
        }

        useEffect(titleView) {
            titleView.subtitle.setCompoundDrawables(
                Resources.dp(context, 24f).toInt(),
                left = R.drawable.ic_alert
            )
            CustomUiUtils.setImageColor(titleView.subtitle, AppColor.Red.color)
        }

        val distance = (lightningTime?.let { getDistance(it, maxOf(it, now)) }
            ?: strike?.value?.distance)
            ?.convertTo(units)
            ?.toRelativeDistance()

        useEffect(titleView, distance, formatter) {
            titleView.title.text = distance?.let { formatDistance(formatter, it) } ?: ""
            titleView.subtitle.isVisible =
                distance != null && Meteorology.isLightningStrikeDangerous(distance)
        }

        useEffect(previousStrikeView, lastStrike, units, formatter) {
            previousStrikeView.text = lastStrike?.let {
                val lastTime = getString(
                    R.string.last_lightning_strike_at,
                    formatter.formatTime(it.time, includeSeconds = false)
                )
                val lastDistance = formatDistance(
                    formatter,
                    it.value.distance.convertTo(units).toRelativeDistance()
                )
                getString(R.string.dash_separated_pair, lastTime, lastDistance)
            } ?: ""
        }

        useEffect(startButton, lightningTime != null) {
            if (lightningTime != null) {
                startButton.setImageResource(R.drawable.ic_thunder)
                startButton.setText(getString(R.string.thunder))
            } else {
                startButton.setImageResource(R.drawable.ic_torch_on)
                startButton.setText(getString(R.string.lightning))
            }
            startButton.setState(lightningTime != null)
        }

        useEffect(startButton, lightningTime, strike, repo) {
            startButton.setOnClickListener {
                if (lightningTime == null) {
                    val time = Instant.now()
                    setNow(time)
                    setLightningTime(time)
                } else {
                    val thunderTime = Instant.now()
                    val newStrike = Reading(
                        LightningStrike(0, getDistance(lightningTime, thunderTime)),
                        thunderTime
                    )
                    strike?.let { setLastStrike(it) }
                    setStrike(newStrike)
                    setLightningTime(null)
                    inBackground {
                        repo.add(newStrike)
                    }
                }
            }
        }
    }

    private fun formatDistance(formatter: FormatService, distance: Distance): String {
        return formatter.formatDistance(distance, Units.getDecimalPlaces(distance.units), false)
    }

    private fun getDistance(lightning: Instant, thunder: Instant): Distance {
        return Distance.meters(Meteorology.getLightningStrikeDistance(lightning, thunder))
    }
}
