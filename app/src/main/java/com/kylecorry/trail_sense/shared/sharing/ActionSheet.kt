package com.kylecorry.trail_sense.shared.sharing

import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.view.setMargins
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.button.MaterialButton
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.BoundBottomSheetDialogFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentActionSheetBinding

class ActionSheet(
    private val title: String,
    private val subtitle: String?,
    private val actions: List<ActionItem>,
    private val onAction: (action: ActionItem?, sheet: ActionSheet) -> Unit
) : BoundBottomSheetDialogFragment<FragmentActionSheetBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.shareSheetTitle.title.text = title
        binding.shareSheetTitle.subtitle.isVisible = subtitle != null
        binding.shareSheetTitle.subtitle.text = subtitle
        actions.forEach { action ->
            val tile = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }
            val button = MaterialButton(
                requireContext(),
                null,
                R.attr.quickActionButtonStyle
            ).apply {
                setIconResource(action.icon)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also {
                    it.gravity = Gravity.CENTER_HORIZONTAL
                }
            }
            val label = TextView(requireContext()).apply {
                text = action.name
                isClickable = false
                isFocusable = false
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setTextColor(Resources.androidTextColorSecondary(requireContext()))
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also {
                    it.topMargin = Resources.dp(requireContext(), 2f).toInt()
                }
            }
            tile.addView(button)
            tile.addView(label)
            tile.layoutParams = FlexboxLayout.LayoutParams(
                Resources.dp(requireContext(), 90f).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).also {
                it.setMargins(Resources.dp(requireContext(), 8f).toInt())
            }
            val clickListener = View.OnClickListener {
                onAction(action, this)
            }
            tile.setOnClickListener(clickListener)
            button.setOnClickListener(clickListener)
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
