package com.kylecorry.trail_sense.tools.tools.quickactions

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ViewQuickActionSheetBinding
import com.kylecorry.trail_sense.main.MainActivity

class QuickActionSheet @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val binding: ViewQuickActionSheetBinding

    init {
        inflate(context, R.layout.view_quick_action_sheet, this)
        binding = ViewQuickActionSheetBinding.bind(this)

        binding.quickActionsToolbar.rightButton.setOnClickListener {
            binding.quickActionsSheet.isVisible = false
        }
    }

    fun close() {
        binding.quickActions.removeAllViews()
        binding.quickActionsSheet.isVisible = false
    }

    fun show(activity: MainActivity) {
        val fragment = activity.getFragment()

        if (fragment == null) {
            Alerts.toast(context, context.getString(R.string.quick_actions_are_unavailable))
            return
        }

        MainActivityQuickActionBinder(fragment, binding).bind()
        binding.quickActionsSheet.isVisible = true
    }

    fun isOpen(): Boolean {
        return binding.quickActionsSheet.isVisible
    }

}