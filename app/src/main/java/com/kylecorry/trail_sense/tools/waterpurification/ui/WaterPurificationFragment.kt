package com.kylecorry.trail_sense.tools.waterpurification.ui

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.core.units.Distance
import com.kylecorry.andromeda.core.units.DistanceUnits
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolWaterPurificationBinding
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.waterpurification.infrastructure.WaterPurificationTimerService
import com.kylecorry.trailsensecore.domain.water.WaterService
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

class WaterPurificationFragment : BoundFragment<FragmentToolWaterPurificationBinding>() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val altimeter by lazy { sensorService.getAltimeter(false) }
    private val cache by lazy { Preferences(requireContext()) }
    private var timer: CountDownTimer? = null
    private var duration: Duration? = null
    private val waterService = WaterService()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.boilButton.setOnClickListener {
            if (timer == null) {
                start()
            } else {
                stop()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (!altimeter.hasValidReading) {
            binding.boilLoading.visibility = View.VISIBLE
            binding.timeLeft.visibility = View.INVISIBLE
            binding.boilButton.visibility = View.INVISIBLE
            altimeter.start(this::updateAltitude)
        } else {
            updateAltitude()
        }

        val lastEndTime = cache.getLong(WATER_PURIFICATION_END_TIME_KEY)
        if (lastEndTime != null) {
            val oldDuration = Duration.between(Instant.now(), Instant.ofEpochMilli(lastEndTime))
            if (!oldDuration.isNegative && !oldDuration.isZero) {
                resume(oldDuration)
                binding.boilLoading.visibility = View.INVISIBLE
                binding.timeLeft.visibility = View.VISIBLE
                binding.boilButton.visibility = View.VISIBLE
            }
        }


    }

    override fun onPause() {
        super.onPause()
        altimeter.stop(this::updateAltitude)
        timer?.cancel()
        timer = null
    }

    private fun resume(timeLeft: Duration) {
        binding.boilButton.text = getString(android.R.string.cancel)

        timer = object : CountDownTimer(timeLeft.toMillis(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000f).roundToInt()
                binding.timeLeft.text = seconds.toString()
                if (!cache.contains(WATER_PURIFICATION_END_TIME_KEY)) {
                    stop()
                }
            }

            override fun onFinish() {
                binding.timeLeft.text = duration?.seconds.toString()
                timer = null
                binding.boilButton.text = getString(R.string.boil_start)
            }

        }.start()
    }

    private fun updateAltitude(): Boolean {
        duration = waterService.getPurificationTime(
            Distance(altimeter.altitude, DistanceUnits.Meters)
        )
        if (timer == null) {
            binding.timeLeft.text = duration?.seconds.toString()
        }
        binding.boilLoading.visibility = View.INVISIBLE
        binding.timeLeft.visibility = View.VISIBLE
        binding.boilButton.visibility = View.VISIBLE
        return false
    }

    private fun start() {
        val totalTime = duration ?: return
        cache.putLong(
            WATER_PURIFICATION_END_TIME_KEY,
            System.currentTimeMillis() + totalTime.toMillis()
        )
        WaterPurificationTimerService.start(requireContext(), totalTime.seconds)
        resume(totalTime)
    }

    private fun stop() {
        timer?.cancel()
        timer = null
        cache.remove(WATER_PURIFICATION_END_TIME_KEY)
        WaterPurificationTimerService.stop(requireContext())
        binding.timeLeft.text = duration?.seconds.toString()
        binding.boilButton.text = getString(R.string.boil_start)
    }


    companion object {
        const val WATER_PURIFICATION_END_TIME_KEY = "water_purification_start_time"
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolWaterPurificationBinding {
        return FragmentToolWaterPurificationBinding.inflate(layoutInflater, container, false)
    }

}