package com.kylecorry.trail_sense.tools.whistle.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.sound.ISoundPlayer
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolWhistleBinding
import com.kylecorry.trail_sense.shared.asSignal
import com.kylecorry.trailsensecore.domain.morse.MorseService
import com.kylecorry.trailsensecore.domain.morse.Signal
import com.kylecorry.trailsensecore.infrastructure.audio.Whistle
import com.kylecorry.trailsensecore.infrastructure.morse.SignalPlayer
import java.time.Duration

class ToolWhistleFragment : BoundFragment<FragmentToolWhistleBinding>() {

    private lateinit var whistle: ISoundPlayer
    private val morseService = MorseService()

    private val morseDurationMs = 400L

    private var state = WhistleState.Off

    private val emergencySignal = listOf(
        Signal.on(Duration.ofSeconds(2)),
        Signal.off(Duration.ofSeconds(1)),
        Signal.on(Duration.ofSeconds(2)),
        Signal.off(Duration.ofSeconds(1)),
        Signal.on(Duration.ofSeconds(2)),
        Signal.off(Duration.ofSeconds(3))
    )

    private val whereAreYouAndAcknowledgedSignal = listOf(
        Signal.on(Duration.ofSeconds(2)),
    )

    private val comeHereSignal = listOf(
        Signal.on(Duration.ofSeconds(2)),
        Signal.off(Duration.ofSeconds(1)),
        Signal.on(Duration.ofSeconds(2)),
    )

    private val sosSignal = morseService.sosSignal(Duration.ofMillis(morseDurationMs)) + listOf(
        Signal.off(
            Duration.ofMillis(morseDurationMs * 7)
        )
    )

    private val signalWhistle by lazy { SignalPlayer(whistle.asSignal()) }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.whistleEmergencyBtn.setOnClickListener {
            state = if (state == WhistleState.Emergency) {
                signalWhistle.cancel()
                WhistleState.Off
            } else {
                whistle.off()
                signalWhistle.play(emergencySignal, true)
                WhistleState.Emergency
            }
            binding.whistleEmergencyBtn.setText(getText(R.string.help).toString())
            updateUI()
        }

        binding.whistleEmergencyBtn.setOnLongClickListener {
            val options = resources.getStringArray(R.array.whistle_signals_entries).toList()
            Pickers.item(
                requireContext(),
                getString(R.string.tool_whistle_title),
                options
            ){
                if (it != null){
                    whistle.off()
                    when (it) {
                        0 -> signalWhistle.play(
                            whereAreYouAndAcknowledgedSignal, false
                        ) { toggleOffInternationWhistleSignals() }
                        1 -> signalWhistle.play(
                            whereAreYouAndAcknowledgedSignal, false
                        ) { toggleOffInternationWhistleSignals() }
                        2 -> signalWhistle.play(
                            comeHereSignal,
                            false
                        ) { toggleOffInternationWhistleSignals() }
                        3 -> signalWhistle.play(emergencySignal, true)
                    }
                    binding.whistleEmergencyBtn.setText(options[it])
                    state = WhistleState.Emergency
                    updateUI()
                }
            }
            true
        }

        binding.whistleSosBtn.setOnClickListener {
            state = if (state == WhistleState.Sos) {
                signalWhistle.cancel()
                WhistleState.Off
            } else {
                whistle.off()
                signalWhistle.play(sosSignal, true)
                WhistleState.Sos
            }
            updateUI()
        }


        binding.whistleBtn.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                signalWhistle.cancel()
                whistle.on()
                state = WhistleState.On
            } else if (event.action == MotionEvent.ACTION_UP) {
                whistle.off()
                state = WhistleState.Off
            }
            updateUI()
            true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        whistle = Whistle()
    }

    override fun onDestroy() {
        super.onDestroy()
        whistle.release()
    }

    override fun onPause() {
        super.onPause()
        whistle.off()
        signalWhistle.cancel()
    }


    private fun updateUI() {
        binding.whistleEmergencyBtn.setState(state == WhistleState.Emergency)
        binding.whistleSosBtn.setState(state == WhistleState.Sos)
        binding.whistleBtn.setState(state == WhistleState.On)
    }

    private fun toggleOffInternationWhistleSignals() {
        state = WhistleState.Off
        signalWhistle.cancel()
        binding.whistleEmergencyBtn.setText(getText(R.string.help).toString())
        updateUI()
    }


    private enum class WhistleState {
        On,
        Off,
        Emergency,
        Sos
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolWhistleBinding {
        return FragmentToolWhistleBinding.inflate(layoutInflater, container, false)
    }

}