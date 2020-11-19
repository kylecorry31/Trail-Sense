package com.kylecorry.trail_sense.tools.whistle.ui

import android.annotation.SuppressLint
import android.media.AudioTrack
import android.media.session.PlaybackState
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolWhistleBinding
import com.kylecorry.trail_sense.tools.whistle.infrastructure.ToneGenerator
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils

class ToolWhistleFragment : Fragment() {

    private var _binding: FragmentToolWhistleBinding? = null
    private val binding get() = _binding!!

    private val toneGenerator = ToneGenerator()
    private lateinit var tone: AudioTrack

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


        binding.whistleBtn.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN){
                play()
                UiUtils.setButtonState(
                    binding.whistleBtn,
                    true,
                    UiUtils.color(requireContext(), R.color.colorPrimary),
                    UiUtils.color(requireContext(), R.color.colorSecondary)
                )
            } else if (event.action == MotionEvent.ACTION_UP){
                pause()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        tone = toneGenerator.getTone(3150)
    }

    override fun onPause() {
        super.onPause()
        pause()
        tone.release()
    }

    private fun isPlaying(): Boolean {
        return tone.playState == AudioTrack.PLAYSTATE_PLAYING
    }

    private fun play() {
        if (isPlaying()){
            return
        }
        tone.play()
    }

    private fun pause() {
        if (!isPlaying()){
            return
        }
        tone.pause()
    }

}