package com.kylecorry.trail_sense.tools.tides.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import com.kylecorry.trail_sense.databinding.FragmentCreateTideBinding
import com.kylecorry.trail_sense.shared.BoundFragment

class CreateTideFragment: BoundFragment<FragmentCreateTideBinding>() {
    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCreateTideBinding {
        return FragmentCreateTideBinding.inflate(layoutInflater, container, false)
    }
}