package com.kylecorry.trail_sense.tools.ballistics.ui

import android.widget.TextView
import com.google.android.material.button.MaterialButtonToggleGroup
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useDistancePreference
import com.kylecorry.trail_sense.shared.views.DistanceInputView
import com.kylecorry.trail_sense.shared.views.MaterialSpinnerView
import kotlin.math.roundToInt

class FragmentScopeAdjustment : TrailSenseReactiveFragment(R.layout.fragment_scope_adjustment) {

    private val referenceDistance = Distance(100f, DistanceUnits.Yards)

    override fun update() {
        val adjustmentTextView = useView<TextView>(R.id.adjustment_amount)
        val distanceToTargetView = useView<DistanceInputView>(R.id.distance_to_target)
        val missDistanceXView = useView<DistanceInputView>(R.id.miss_distance_x)
        val missDistanceYView = useView<DistanceInputView>(R.id.miss_distance_y)
        val missDirectionXView = useView<MaterialButtonToggleGroup>(R.id.miss_direction_x)
        val missDirectionYView = useView<MaterialButtonToggleGroup>(R.id.miss_direction_y)
        val clickAmountView = useView<MaterialSpinnerView>(R.id.click_amount)

        val formatter = useService<FormatService>()
        val prefs = useService<UserPreferences>()

        val (offsetX, setOffsetX) = useState(Distance(0f, DistanceUnits.Inches))
        val (offsetY, setOffsetY) = useState(Distance(0f, DistanceUnits.Inches))
        val (offsetDirectionX, setOffsetDirectionX) = useState(Direction.Left)
        val (offsetDirectionY, setOffsetDirectionY) = useState(Direction.Up)
        val (distanceToTarget, setDistanceToTarget) = useDistancePreference("cache-ballistics-sight-in-range")
        val (distancePerClick, setDistancePerClick) = useDistancePreference("cache-ballistics-scope-click-adjustment")

        useEffect(distancePerClick) {
            if (distancePerClick == null) {
                setDistancePerClick(
                    Distance(
                        0.25f,
                        DistanceUnits.Inches
                    )
                )
            }
        }

        useEffect(
            distanceToTargetView,
            missDistanceXView,
            missDistanceYView,
            missDirectionXView,
            missDirectionYView,
            clickAmountView
        ) {
            distanceToTargetView.units =
                formatter.sortDistanceUnits(DistanceUtils.hikingDistanceUnits)
            distanceToTargetView.hint = getString(R.string.distance_to_target)

            if (distanceToTarget != null) {
                distanceToTargetView.value = distanceToTarget
            }
            distanceToTargetView.setOnValueChangeListener {
                setDistanceToTarget(it ?: Distance(0f, DistanceUnits.Yards))
            }

            missDistanceXView.units = formatter.sortDistanceUnits(DistanceUtils.rulerDistanceUnits)
            missDistanceXView.setOnValueChangeListener {
                setOffsetX(it ?: Distance(0f, DistanceUnits.Inches))
            }

            missDistanceYView.units = formatter.sortDistanceUnits(DistanceUtils.rulerDistanceUnits)
            missDistanceYView.setOnValueChangeListener {
                setOffsetY(it ?: Distance(0f, DistanceUnits.Inches))
            }

            missDirectionXView.check(R.id.miss_left)
            missDirectionXView.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) {
                    setOffsetDirectionX(
                        when (checkedId) {
                            R.id.miss_left -> Direction.Left
                            R.id.miss_right -> Direction.Right
                            else -> Direction.Left
                        }
                    )
                }
            }

            missDirectionYView.check(R.id.miss_up)
            missDirectionYView.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) {
                    setOffsetDirectionY(
                        when (checkedId) {
                            R.id.miss_up -> Direction.Up
                            R.id.miss_down -> Direction.Down
                            else -> Direction.Up
                        }
                    )
                }
            }

            clickAmountView.setHint(getString(R.string.adjustment_per_click))
            val clickAmounts = listOf(
                "1/8 MOA" to Distance(0.125f, DistanceUnits.Inches),
                "1/4 MOA" to Distance(0.25f, DistanceUnits.Inches),
                "1/2 MOA" to Distance(0.5f, DistanceUnits.Inches),
                "1 MOA" to Distance(1f, DistanceUnits.Inches),
                "0.1 mil" to Distance(0.36f, DistanceUnits.Inches),
                "0.05 mil" to Distance(0.18f, DistanceUnits.Inches),
            )

            val selectedIdx = if (distancePerClick != null) {
                val selection = clickAmounts.indexOfFirst { it.second == distancePerClick }
                if (selection != -1) {
                    selection
                } else {
                    1
                }
            } else if (prefs.distanceUnits == UserPreferences.DistanceUnits.Feet) {
                1
            } else {
                4
            }
            setDistancePerClick(clickAmounts[selectedIdx].second)
            clickAmountView.setItems(clickAmounts.map { it.first })
            clickAmountView.setSelection(selectedIdx)
            clickAmountView.setOnItemSelectedListener {
                if (it != null) {
                    setDistancePerClick(clickAmounts[it].second)
                }
            }
        }


        val adjustmentX = useMemo(offsetX, offsetDirectionX, distanceToTarget, distancePerClick) {
            getAdjustment(
                distancePerClick ?: return@useMemo null,
                referenceDistance,
                offsetX,
                offsetDirectionX,
                distanceToTarget ?: return@useMemo null
            )
        }

        val adjustmentY = useMemo(offsetY, offsetDirectionY, distanceToTarget, distancePerClick) {
            getAdjustment(
                distancePerClick ?: return@useMemo null,
                referenceDistance,
                offsetY,
                offsetDirectionY,
                distanceToTarget ?: return@useMemo null
            )
        }

        useEffect(adjustmentTextView, adjustmentX, adjustmentY) {
            val adjustmentTexts = mutableListOf<String>()
            if (adjustmentX != null) {
                adjustmentTexts.add("${adjustmentX.direction.name} ${adjustmentX.clicks} clicks")
            }

            if (adjustmentY != null) {
                adjustmentTexts.add("${adjustmentY.direction.name} ${adjustmentY.clicks} clicks")
            }

            if (adjustmentTexts.isEmpty()) {
                adjustmentTextView.text = getString(R.string.none)
                return@useEffect
            }

            adjustmentTextView.text = adjustmentTexts.joinToString("\n")
        }

    }


    private fun getAdjustment(
        distancePerClick: Distance,
        referenceDistance: Distance,
        offset: Distance,
        offsetDirection: Direction,
        distanceToTarget: Distance
    ): Adjustment? {
        val offsetInches = offset.convertTo(DistanceUnits.Inches).distance
        val distanceToTargetYards = distanceToTarget.convertTo(DistanceUnits.Yards).distance
        val referenceYards = referenceDistance.convertTo(DistanceUnits.Yards).distance
        val inchesPerClick = distancePerClick.convertTo(DistanceUnits.Inches).distance

        if (SolMath.isZero(inchesPerClick) || SolMath.isZero(distanceToTargetYards)) {
            return null
        }

        val clicks = (offsetInches / inchesPerClick) * (referenceYards / distanceToTargetYards)
        val clickDirection = when (offsetDirection) {
            Direction.Left -> Direction.Right
            Direction.Right -> Direction.Left
            Direction.Up -> Direction.Down
            Direction.Down -> Direction.Up
        }
        return Adjustment(clicks.roundToInt(), clickDirection)
    }

    private data class Adjustment(val clicks: Int, val direction: Direction)

    private enum class Direction {
        Left, Right, Up, Down
    }
}