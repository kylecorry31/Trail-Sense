package com.kylecorry.trail_sense.tools.turn_back.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.databinding.FragmentToolTurnBackBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime

class TurnBackFragment : BoundFragment<FragmentToolTurnBackBinding>() {

    private val prefs by lazy { UserPreferences(requireContext()) }
    private val formatter by lazy { FormatService.getInstance(requireContext()) }

    private var returnTime by state<Instant?>(null)

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolTurnBackBinding {
        return FragmentToolTurnBackBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO: Add a sunset button

        binding.edittext.setOnClickListener {
            Pickers.time(
                requireContext(),
                prefs.use24HourTime,
                returnTime?.toZonedDateTime()?.toLocalTime() ?: LocalTime.now()
            ) {
                if (it != null) {
                    returnTime = if (it < LocalTime.now()) {
                        ZonedDateTime.now().plusDays(1).with(it)
                    } else {
                        ZonedDateTime.now().with(it)
                    }.toInstant()

                    // TODO: Schedule the notification
                }
            }
        }

        binding.cancelButton.setOnClickListener {
            // TODO: Cancel the notification
            returnTime = null
        }
    }

    override fun onUpdate() {
        super.onUpdate()
        effect("return_time", returnTime) {
            binding.edittext.setText(returnTime?.let { formatter.formatTime(it) })
        }

        // TODO: Display time until return (countdown)
    }
}