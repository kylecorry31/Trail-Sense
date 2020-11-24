package com.kylecorry.trail_sense.tools.waterpurification.ui

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolWaterPurificationBinding
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.waterpurification.domain.WaterPurificationService
import com.kylecorry.trail_sense.tools.waterpurification.infrastructure.WaterPurificationTimerService
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

class WaterPurificationFragment : Fragment() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val altimeter by lazy { sensorService.getAltimeter(false) }
    private val cache by lazy { Cache(requireContext()) }
    private var timer: CountDownTimer? = null
    private var duration: Duration? = null
    private val waterPurificationService = WaterPurificationService()

    private var _binding: FragmentToolWaterPurificationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentToolWaterPurificationBinding.inflate(inflater, container, false)
        binding.boilButton.setOnClickListener {
            if (timer == null) {
                start()
            } else {
                stop()
            }
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()

        if (!altimeter.hasValidReading || duration == null) {
            binding.boilLoading.visibility = View.VISIBLE
            binding.timeLeft.visibility = View.INVISIBLE
            altimeter.start(this::updateAltitude)
        }

        val lastEndTime = cache.getLong(WATER_PURIFICATION_END_TIME_KEY)
        if (lastEndTime != null) {
            val oldDuration = Duration.between(Instant.now(), Instant.ofEpochMilli(lastEndTime))
            if (!oldDuration.isNegative && !oldDuration.isZero) {
                resume(oldDuration)
                binding.boilLoading.visibility = View.INVISIBLE
                binding.timeLeft.visibility = View.VISIBLE
            }
        }


    }

    override fun onPause() {
        super.onPause()
        timer?.cancel()
        timer = null
    }

    private fun resume(timeLeft: Duration) {
        binding.boilButton.text = getString(R.string.dialog_cancel)

        timer = object : CountDownTimer(timeLeft.toMillis(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000f).roundToInt()
                binding.timeLeft.text = seconds.toString()
            }

            override fun onFinish() {
                binding.timeLeft.text = duration?.seconds.toString()
                timer = null
                binding.boilButton.text = getString(R.string.boil_start)
            }

        }.start()
    }

    private fun updateAltitude(): Boolean {
        duration = waterPurificationService.getPurificationTime(altimeter.altitude)
        if (timer == null) {
            binding.timeLeft.text = duration?.seconds.toString()
        }
        binding.boilLoading.visibility = View.INVISIBLE
        binding.timeLeft.visibility = View.VISIBLE
        return false
    }

    private fun start() {
        val totalTime = duration ?: return
        cache.putLong(WATER_PURIFICATION_END_TIME_KEY, System.currentTimeMillis() + totalTime.toMillis())
        WaterPurificationTimerService.start(requireContext(), totalTime.seconds)
        resume(totalTime)
    }

    private fun stop() {
        timer?.cancel()
        timer = null
        WaterPurificationTimerService.stop(requireContext())
        binding.timeLeft.text = duration?.seconds.toString()
        binding.boilButton.text = getString(R.string.boil_start)
    }


    companion object {
        private const val WATER_PURIFICATION_END_TIME_KEY = "water_purification_start_time"
    }

}