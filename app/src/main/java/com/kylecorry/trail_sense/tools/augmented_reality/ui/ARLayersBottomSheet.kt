package com.kylecorry.trail_sense.tools.augmented_reality.ui

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.fragments.BoundBottomSheetDialogFragment
import com.kylecorry.trail_sense.databinding.FragmentArLayersBottomSheetBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils.replaceChildFragment
import java.time.LocalDate

class ARLayersBottomSheet : BoundBottomSheetDialogFragment<FragmentArLayersBottomSheetBinding>() {

    private var onDismissListener: (() -> Unit)? = null
    var astronomyOverrideDate: LocalDate? = null

    fun setOnDismissListener(listener: (() -> Unit)?) {
        onDismissListener = listener
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentArLayersBottomSheetBinding {
        return FragmentArLayersBottomSheetBinding.inflate(layoutInflater, container, false)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissListener?.invoke()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.title.rightButton.setOnClickListener {
            dismiss()
        }

        val preferences = ARLayersBottomSheetPreferenceFragment()
        preferences.astronomyOverrideDate = astronomyOverrideDate
        preferences.setOnAstronomyDateChangeListener {
            astronomyOverrideDate = it
        }

        replaceChildFragment(
            preferences,
            binding.preferencesFragment.id
        )
    }
}