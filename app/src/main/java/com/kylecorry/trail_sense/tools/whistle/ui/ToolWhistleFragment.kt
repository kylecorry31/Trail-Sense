package com.kylecorry.trail_sense.tools.whistle.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolWhistleBinding
import com.kylecorry.trailsensecore.infrastructure.audio.IWhistle
import com.kylecorry.trailsensecore.infrastructure.audio.Whistle
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import java.time.Duration

class ToolWhistleFragment : Fragment() {

    private var _binding: FragmentToolWhistleBinding? = null
    private val binding get() = _binding!!

    private lateinit var whistle: IWhistle

    private val emergencyWhistleDuration = Duration.ofMillis(500)
    private val emergencyWhistleStates = listOf(true, true, false, true, true, false, true, true, false, false, false, false)
    private var emergencyWhistleState = 0
    private var isEmergencyWhistleOn = false
    private val emergencyWhistleIntervalometer = Intervalometer {
        val isOn = emergencyWhistleStates[emergencyWhistleState]
        if (isOn) {
            whistle.on()
        } else {
            whistle.off()
        }

        emergencyWhistleState++
        emergencyWhistleState %= emergencyWhistleStates.size
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentToolWhistleBinding.inflate(inflater, container, false)
        UiUtils.setButtonState(
            binding.whistleBtn,
            false,
            UiUtils.color(requireContext(), R.color.colorPrimary),
            UiUtils.color(requireContext(), R.color.colorSecondary)
        )

        binding.whistleSosBtn.setOnClickListener {
            if (isEmergencyWhistleOn){
                stopEmergencyWhistle()
            } else {
                startEmergencyWhistle()
            }
        }


        binding.whistleBtn.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                stopEmergencyWhistle()
                whistle.on()
                UiUtils.setButtonState(
                    binding.whistleBtn,
                    true,
                    UiUtils.color(requireContext(), R.color.colorPrimary),
                    UiUtils.color(requireContext(), R.color.colorSecondary)
                )
            } else if (event.action == MotionEvent.ACTION_UP) {
                whistle.off()
                UiUtils.setButtonState(
                    binding.whistleBtn,
                    false,
                    UiUtils.color(requireContext(), R.color.colorPrimary),
                    UiUtils.color(requireContext(), R.color.colorSecondary)
                )
            }
            true
        }
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        whistle = Whistle()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        whistle.release()
    }

    override fun onPause() {
        super.onPause()
        stopEmergencyWhistle()
        whistle.off()
    }

    private fun startEmergencyWhistle() {
        emergencyWhistleState = 0
        isEmergencyWhistleOn = true
        whistle.off()
        UiUtils.setButtonState(
            binding.whistleBtn,
            false,
            UiUtils.color(requireContext(), R.color.colorPrimary),
            UiUtils.color(requireContext(), R.color.colorSecondary)
        )
        UiUtils.setButtonState(
            binding.whistleSosBtn,
            true,
            UiUtils.color(requireContext(), R.color.colorPrimary),
            UiUtils.color(requireContext(), R.color.colorSecondary)
        )
        emergencyWhistleIntervalometer.interval(emergencyWhistleDuration)
    }

    private fun stopEmergencyWhistle() {
        emergencyWhistleIntervalometer.stop()
        emergencyWhistleState = 0
        isEmergencyWhistleOn = false
        whistle.off()
        UiUtils.setButtonState(
            binding.whistleSosBtn,
            false,
            UiUtils.color(requireContext(), R.color.colorPrimary),
            UiUtils.color(requireContext(), R.color.colorSecondary)
        )
    }

}