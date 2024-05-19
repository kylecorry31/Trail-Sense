package com.kylecorry.trail_sense.tools.tools.quickactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.core.view.setMargins
import com.google.android.flexbox.FlexboxLayout
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.fragments.BoundBottomSheetDialogFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentQuickActionsSheetBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.quickactions.QuickActionFactory
import com.kylecorry.trail_sense.shared.requireMainActivity

class QuickActionsBottomSheet : BoundBottomSheetDialogFragment<FragmentQuickActionsSheetBinding>() {

    private val prefs by lazy { UserPreferences(requireContext()) }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentQuickActionsSheetBinding {
        return FragmentQuickActionsSheetBinding.inflate(layoutInflater, container, false)
    }

    private fun createButton(): ImageButton {
        val size = Resources.dp(requireContext(), 40f).toInt()
        val margins = Resources.dp(requireContext(), 8f).toInt()
        val button = ImageButton(requireContext())
        button.layoutParams = FlexboxLayout.LayoutParams(size, size).apply {
            setMargins(margins)
        }
        button.background =
            Resources.drawable(requireContext(), R.drawable.rounded_rectangle)
        button.elevation = 2f

        binding.quickActions.addView(button)

        CustomUiUtils.setButtonState(button, false)

        return button
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fragment = requireMainActivity().getFragment() as? AndromedaFragment ?: return
        val selected = prefs.toolQuickActions.sorted()
        val factory = QuickActionFactory()
        selected.forEach {
            val action = factory.create(it, createButton(), fragment)
            action.bind(viewLifecycleOwner)
        }
    }
}