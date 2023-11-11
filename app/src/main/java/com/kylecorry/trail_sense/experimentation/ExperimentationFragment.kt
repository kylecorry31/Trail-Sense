package com.kylecorry.trail_sense.experimentation

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.setMargins
import androidx.gridlayout.widget.GridLayout
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.setCompoundDrawables
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentTools2Binding
import com.kylecorry.trail_sense.quickactions.ToolsQuickActionBinder
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.extensions.setOnQueryTextListener
import com.kylecorry.trail_sense.shared.views.TileButton
import com.kylecorry.trail_sense.tools.ui.Tool
import com.kylecorry.trail_sense.tools.ui.Tools

class ExperimentationFragment : BoundFragment<FragmentTools2Binding>() {

    private val tools by lazy { Tools.getTools(requireContext()).flatMap { it.tools } }

    override fun generateBinding(
        layoutInflater: LayoutInflater, container: ViewGroup?
    ): FragmentTools2Binding {
        return FragmentTools2Binding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.quickActions.children.forEach {
            if (it is ImageButton) {
                CustomUiUtils.setButtonState(it, false)
            }
        }

        updatePinnedTools()
        updateTools()

        updateQuickActions()

        binding.settingsBtn.setOnClickListener {
            findNavController().navigate(R.id.action_settings)
        }

        binding.searchbox.setOnQueryTextListener { _, _ ->
            updateTools()
            true
        }
    }

    private fun updateQuickActions(){
        ToolsQuickActionBinder(this, binding).bind()
    }

    private fun updateTools() {
        val filter = binding.searchbox.query

        val tools = if (filter.isNullOrBlank()) {
            this.tools
        } else {
            this.tools.filter {
                it.name.contains(filter, true) || it.description?.contains(filter, true) == true
            }
        }.sortedBy { it.name }

        populateTools(tools, binding.tools)
    }

    private fun updatePinnedTools() {
        val pinnedTools = listOf(
            R.id.action_navigation, R.id.action_weather, R.id.action_astronomy, R.id.action_settings
        )
        val pinned = tools.filter {
            it.navAction in pinnedTools
        }

        populateTools(pinned.sortedBy { it.name }, binding.pinned)
    }

    private fun populateTools(tools: List<Tool>, grid: GridLayout) {
        grid.removeAllViews()
        val iconSize = Resources.dp(requireContext(), 24f).toInt()
        val iconPadding = Resources.dp(requireContext(), 16f).toInt()
        val iconColor = Resources.androidTextColorPrimary(requireContext())
        val buttonHeight = Resources.dp(requireContext(), 64f).toInt()
        val buttonMargins = Resources.dp(requireContext(), 8f).toInt()
        val buttonPadding = Resources.dp(requireContext(), 16f).toInt()
        val buttonBackgroundColor = Resources.getAndroidColorAttr(
            requireContext(), android.R.attr.colorBackgroundFloating
        )

        val gridColumnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        val gridRowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)

        tools.forEach {
            val button = TextView(requireContext())
            button.text = it.name
            button.setCompoundDrawables(iconSize, left = it.icon)
            button.compoundDrawablePadding = iconPadding
            CustomUiUtils.setImageColor(button, iconColor)
            button.layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = buttonHeight
                columnSpec = gridColumnSpec
                rowSpec = gridRowSpec
                setMargins(buttonMargins)
            }
            button.gravity = Gravity.CENTER_VERTICAL
            button.setPadding(buttonPadding, 0, buttonPadding, 0)

            button.setBackgroundResource(R.drawable.rounded_rectangle)
            button.backgroundTintList = ColorStateList.valueOf(buttonBackgroundColor)
            button.isClickable = true
            button.setOnClickListener { _ ->
                findNavController().navigate(it.navAction)
            }

            grid.addView(button)
        }
    }

}