package com.kylecorry.trail_sense.tools.whistle.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.databinding.FragmentToolWhistleBinding
import com.kylecorry.trailsensecore.infrastructure.audio.ISoundPlayer
import com.kylecorry.trailsensecore.infrastructure.audio.Whistle
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import java.time.Duration

class ToolWhistleFragment : Fragment() {

    private var _binding: FragmentToolWhistleBinding? = null
    private val binding get() = _binding!!

    private lateinit var whistle: ISoundPlayer

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
    ): View {
        _binding = FragmentToolWhistleBinding.inflate(inflater, container, false)

        binding.whistleSosBtn.setOnClickListener {
            if (isEmergencyWhistleOn){
                stopEmergencyWhistle()
            } else {
                startEmergencyWhistle()
            }
        }


        binding.whistleBtn.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                stopEmergencyWhistle()
                whistle.on()
                binding.whistleBtn.setState(true)

            } else if (event.action == MotionEvent.ACTION_UP) {
                whistle.off()
                binding.whistleBtn.setState(false)
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
        binding.whistleBtn.setState(false)
        binding.whistleSosBtn.setState(true)
        emergencyWhistleIntervalometer.interval(emergencyWhistleDuration)
    }

    private fun stopEmergencyWhistle() {
        emergencyWhistleIntervalometer.stop()
        emergencyWhistleState = 0
        isEmergencyWhistleOn = false
        whistle.off()
        binding.whistleSosBtn.setState(false)
    }

}