package com.kylecorry.trail_sense.tools.ballistics.ui

import android.widget.TextView
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.sol.science.physics.Physics
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.EnergyUnits
import com.kylecorry.sol.units.WeightUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useSpeedPreference
import com.kylecorry.trail_sense.shared.extensions.useWeightPreference
import com.kylecorry.trail_sense.shared.views.BulletSpeedInputView
import com.kylecorry.trail_sense.shared.views.WeightInputView

class FragmentEnergyCalculator : TrailSenseReactiveFragment(R.layout.fragment_energy_calculator) {

    override fun update() {
        val bulletSpeedView = useView<BulletSpeedInputView>(R.id.bullet_speed)
        val bulletWeightView = useView<WeightInputView>(R.id.bullet_weight)
        val energyView = useView<TextView>(R.id.energy_amount)

        val formatter = useService<FormatService>()

        val (bulletSpeed, setBulletSpeed) = useSpeedPreference("cache-ballistics-bullet-speed")
        val (bulletWeight, setBulletWeight) = useWeightPreference("cache-ballistics-bullet-weight")

        useEffect(bulletSpeedView, bulletWeightView) {
            bulletSpeedView.units =
                formatter.sortDistanceUnits(listOf(DistanceUnits.Feet, DistanceUnits.Meters))
            bulletSpeedView.hint = getString(R.string.muzzle_velocity)
            if (bulletSpeed != null) {
                bulletSpeedView.value = bulletSpeed
            }
            bulletSpeedView.setOnValueChangeListener {
                setBulletSpeed(it)
            }

            bulletWeightView.units = formatter.sortWeightUnits(WeightUnits.entries)
            bulletWeightView.unit = WeightUnits.Grains
            bulletWeightView.hint = getString(R.string.bullet_weight)
            if (bulletWeight != null) {
                bulletWeightView.value = bulletWeight
            }
            bulletWeightView.setOnValueChangeListener {
                setBulletWeight(it)
            }
        }

        val energy = useMemo(bulletSpeed, bulletWeight) {
            Physics.getKineticEnergy(
                bulletWeight ?: return@useMemo null,
                bulletSpeed ?: return@useMemo null
            ).convertTo(
                if (bulletSpeed.distanceUnits.isMetric) {
                    EnergyUnits.Joules
                } else {
                    EnergyUnits.FootPounds
                }
            )
        }

        useEffect(energyView, energy) {
            if (energy == null) {
                energyView.text = null
                return@useEffect
            }

            val formatted = DecimalFormatter.format(
                energy.value,
                1
            )

            energyView.text = if (energy.units == EnergyUnits.Joules) {
                getString(R.string.format_joules, formatted)
            } else {
                getString(R.string.format_fpe, formatted)
            }
        }


    }
}