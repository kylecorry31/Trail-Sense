package com.kylecorry.trail_sense.tools.tides.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.trail_sense.databinding.FragmentTideBinding
import com.kylecorry.trail_sense.shared.BoundFragment
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.tools.tides.domain.TideService
import java.time.*

class TidesFragment: BoundFragment<FragmentTideBinding>() {

    private val tideService = TideService()
    private val formatService by lazy { FormatServiceV2(requireContext()) }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTideBinding {
        return FragmentTideBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tides.text = "Today\n${getTides(LocalDate.now())}\n\nTomorrow\n${getTides(LocalDate.now().plusDays(1))}\n" +
                "\n" +
                "Two Days\n" +
                "${getTides(LocalDate.now().plusDays(2))}"
    }

    private fun getTides(date: LocalDate): String {
        return tideService.getTides(
            ZonedDateTime.of(LocalDateTime.of(2021, Month.MARCH, 13, 20, 18), ZoneId.systemDefault()),
            null,//ZonedDateTime.of(LocalDateTime.of(2021, Month.MARCH, 13, 13, 4), ZoneId.systemDefault()),
            date
        ).joinToString("\n") { "${ if(it.isHigh) "High" else "Low"} - ${formatService.formatTime(it.time.toLocalTime())}" }
    }

}