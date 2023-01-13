package com.kylecorry.trail_sense.shared.sharing

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.setMargins
import com.google.android.flexbox.FlexboxLayout
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.BoundBottomSheetDialogFragment
import com.kylecorry.trail_sense.databinding.FragmentActionSheetBinding
import com.kylecorry.trail_sense.shared.views.TileButton

class ActionSheet(
    private val title: String,
    private val actions: List<ActionItem>,
    private val onAction: (action: ActionItem?, sheet: ActionSheet) -> Unit
) : BoundBottomSheetDialogFragment<FragmentActionSheetBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.shareSheetTitle.text = title
        actions.forEach { action ->
            val tile = TileButton(requireContext(), null)
            tile.setText(action.name)
            tile.setImageResource(action.icon)
            tile.setTilePadding(Resources.dp(requireContext(), 32f).toInt())
            tile.layoutParams = FlexboxLayout.LayoutParams(
                Resources.dp(requireContext(), 90f).toInt(),
                Resources.dp(requireContext(), 90f).toInt()
            ).also {
                it.setMargins(Resources.dp(requireContext(), 8f).toInt())
            }
            tile.setOnClickListener {
                onAction(action, this)
            }
            binding.shareSheetItems.addView(tile)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onAction(null, this)
    }


    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentActionSheetBinding {
        return FragmentActionSheetBinding.inflate(layoutInflater, container, false)
    }

}