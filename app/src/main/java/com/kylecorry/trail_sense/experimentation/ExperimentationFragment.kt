package com.kylecorry.trail_sense.experimentation

import android.view.LayoutInflater
import android.view.ViewGroup
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.trail_sense.databinding.FragmentExperimentationBinding

class ExperimentationFragment : BoundFragment<FragmentExperimentationBinding>() {

    override fun generateBinding(
        layoutInflater: LayoutInflater, container: ViewGroup?
    ): FragmentExperimentationBinding {
        return FragmentExperimentationBinding.inflate(layoutInflater, container, false)
    }
}