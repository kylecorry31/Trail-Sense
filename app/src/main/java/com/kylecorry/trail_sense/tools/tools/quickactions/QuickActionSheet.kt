package com.kylecorry.trail_sense.tools.tools.quickactions

import android.content.Context
import android.util.AttributeSet
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.google.android.material.tabs.TabLayout
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ViewQuickActionSheetBinding
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetViewBinder

class QuickActionSheet @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    private val binding: ViewQuickActionSheetBinding
    private var quickActionBinder: MainActivityQuickActionBinder? = null
    private var widgetBinder: ToolWidgetViewBinder? = null
    private var backCallback: OnBackPressedCallback? = null

    init {
        inflate(context, R.layout.view_quick_action_sheet, this)
        binding = ViewQuickActionSheetBinding.bind(this)

        binding.closeButton.setOnClickListener {
            close()
        }

        binding.tabs.addTab(
            binding.tabs.newTab().also { it.text = context.getString(R.string.quick_actions) })
        binding.tabs.addTab(
            binding.tabs.newTab().also { it.text = context.getString(R.string.widgets) })

        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab == null) {
                    return
                }
                when (tab.position) {
                    0 -> {
                        showQuickActions()
                    }

                    1 -> {
                        showWidgets()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })
    }

    fun close() {
        binding.quickActions.removeAllViews()
        binding.quickActionsSheet.isVisible = false
        widgetBinder?.unbind()
        widgetBinder = null
        backCallback?.remove()
        backCallback = null
    }

    fun show(activity: MainActivity, tab: Int = 0) {
        if (tab == 0) {
            showQuickActions()
        } else {
            showWidgets()
        }

        if (isOpen()) {
            return
        }

        val fragment = activity.getFragment()

        if (fragment == null) {
            Alerts.toast(context, context.getString(R.string.quick_actions_are_unavailable))
            return
        }

        widgetBinder?.unbind()
        quickActionBinder = MainActivityQuickActionBinder(fragment, binding)
        widgetBinder = ToolWidgetViewBinder(fragment, binding)
        quickActionBinder?.bind()
        widgetBinder?.bind()
        binding.quickActionsSheet.isVisible = true

        backCallback?.remove()
        backCallback = activity.onBackPressedDispatcher.addCallback(activity, true) {
            close()
        }
    }

    fun isOpen(): Boolean {
        return binding.quickActionsSheet.isVisible
    }

    private fun showQuickActions() {
        if (binding.tabs.selectedTabPosition != 0) {
            binding.tabs.selectTab(binding.tabs.getTabAt(0))
        }
        binding.quickActionsContainer.isVisible = true
        binding.widgetsContainer.isVisible = false
    }

    private fun showWidgets() {
        if (binding.tabs.selectedTabPosition != 1) {
            binding.tabs.selectTab(binding.tabs.getTabAt(1))
        }
        binding.quickActionsContainer.isVisible = false
        binding.widgetsContainer.isVisible = true
    }

}