package com.kylecorry.trail_sense.astronomy.ui

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.fragments.BoundBottomSheetDialogFragment
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentAstronomyDaySeekerBottomSheetBinding
import com.kylecorry.trail_sense.shared.FormatService
import java.time.Duration
import java.time.LocalDateTime

class AstroDaySeekerBottomSheet :
    BoundBottomSheetDialogFragment<FragmentAstronomyDaySeekerBottomSheetBinding>() {

    private val formatService by lazy { FormatService(requireContext()) }
    private val markdownService by lazy { MarkdownService(requireContext()) }

    var startTime: LocalDateTime? = null
    var currentTime: LocalDateTime? = null
    var endTime: LocalDateTime? = null
    var positions: AstroPositions? = null
    var onTimeChangeListener: ((time: LocalDateTime?) -> Unit)? = null

    fun show(
        fragment: Fragment,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        currentTime: LocalDateTime = startTime,
        onTimeChangeListener: (time: LocalDateTime?) -> Unit
    ) {
        this.startTime = startTime
        this.endTime = endTime
        this.currentTime = currentTime
        this.onTimeChangeListener = onTimeChangeListener
        show(fragment)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onTimeChangeListener?.invoke(null)
    }

    fun updatePositions(positions: AstroPositions) {
        this.positions = positions
        if (!isBound) return
        binding.sunPositionText.text = markdownService.toMarkdown(
            getString(
                R.string.sun_moon_position_template,
                getString(R.string.sun),
                formatService.formatDegrees(positions.sunAltitude),
                formatService.formatDegrees(positions.sunAzimuth)
            )
        )

        binding.moonPositionText.text = markdownService.toMarkdown(
            getString(
                R.string.sun_moon_position_template,
                getString(R.string.moon),
                formatService.formatDegrees(positions.moonAltitude),
                formatService.formatDegrees(positions.moonAzimuth)
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (startTime != null && endTime != null && currentTime != null) {
            val totalDuration = Duration.between(startTime, endTime).seconds
            val currentDuration = Duration.between(startTime, currentTime).seconds
            val progress = 100 * currentDuration / totalDuration.toFloat()
            binding.timeSeeker.progress = progress.toInt()
            binding.time.text =
                formatService.formatTime(currentTime!!.toLocalTime(), includeSeconds = false)
        }

        positions?.let {
            updatePositions(it)
        }

        binding.timeSeeker.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val start = startTime ?: return
                val end = endTime ?: return
                val seconds = (Duration.between(start, end).seconds * progress / 100f).toLong()
                currentTime = start.plusSeconds(seconds)
                binding.time.text =
                    formatService.formatTime(currentTime!!.toLocalTime(), includeSeconds = false)
                onTimeChangeListener?.invoke(start.plusSeconds(seconds))
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAstronomyDaySeekerBottomSheetBinding {
        return FragmentAstronomyDaySeekerBottomSheetBinding.inflate(
            layoutInflater,
            container,
            false
        )
    }

    data class AstroPositions(
        val moonAltitude: Float,
        val sunAltitude: Float,
        val moonAzimuth: Float,
        val sunAzimuth: Float
    )
}