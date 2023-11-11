package com.kylecorry.trail_sense.tools.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.setMargins
import androidx.gridlayout.widget.GridLayout
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.dialog
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.setCompoundDrawables
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentTools2Binding
import com.kylecorry.trail_sense.quickactions.ToolsQuickActionBinder
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.extensions.setOnQueryTextListener
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils
import com.kylecorry.trail_sense.tools.ui.sort.AlphabeticalToolSort
import com.kylecorry.trail_sense.tools.ui.sort.CategoricalToolSort
import com.kylecorry.trail_sense.tools.ui.sort.CategorizedTools

class ToolsFragment : BoundFragment<FragmentTools2Binding>() {

    private val tools by lazy { Tools.getTools(requireContext()) }

    private val pinnedToolManager = PinnedToolManager()

    init {
        // Navigation
        pinnedToolManager.pin(6L)

        // Weather
        pinnedToolManager.pin(20L)

        // Astronomy
        pinnedToolManager.pin(14L)
    }

    private val toolSorter by lazy { CategoricalToolSort(requireContext()) }
    private val pinnedSorter = AlphabeticalToolSort()

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

        binding.pinnedEditBtn.setOnClickListener {
            // Sort alphabetically, but if the tool is already pinned, put it first
            val sorted = tools.sortedBy { tool ->
                if (pinnedToolManager.isPinned(tool.id)) {
                    "0${tool.name}"
                } else {
                    tool.name
                }
            }
            val toolNames = sorted.map { it.name }
            val defaultSelected = sorted.mapIndexedNotNull { index, tool ->
                if (pinnedToolManager.isPinned(tool.id)) {
                    index
                } else {
                    null
                }
            }

            Pickers.items(
                requireContext(),
                getString(R.string.pinned),
                toolNames,
                defaultSelected
            ) { selected ->
                if (selected != null) {
                    pinnedToolManager.setPinnedToolIds(selected.map { sorted[it].id })
                }

                updatePinnedTools()
            }
        }
    }

    // TODO: Add a way to customize this
    private fun updateQuickActions() {
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
        }

        populateTools(toolSorter.sort(tools), binding.tools)
    }

    private fun updatePinnedTools() {
        val pinned = tools.filter {
            pinnedToolManager.isPinned(it.id)
        }

        binding.pinned.isVisible = pinned.isNotEmpty()

        populateTools(pinnedSorter.sort(pinned), binding.pinned)
    }

    private fun populateTools(categories: List<CategorizedTools>, grid: GridLayout) {
        grid.removeAllViews()

        if (categories.size == 1){
            categories.first().tools.forEach {
                grid.addView(createToolButton(it))
            }
            return
        }


        categories.forEach {
            grid.addView(createToolCategoryHeader(it.categoryName))
            it.tools.forEach {
                grid.addView(createToolButton(it))
            }
        }
    }

    private fun createToolCategoryHeader(name: String?): View {
        // TODO: Move this to the class level
        val headerMargins = Resources.dp(requireContext(), 8f).toInt()

        val gridColumnSpec = GridLayout.spec(GridLayout.UNDEFINED, 2, 1f)
        val gridRowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)

        val header = TextView(requireContext())
        header.text = name
        header.textSize = 14f
        header.setTextColor(AppColor.Orange.color)
        // Bold
        header.paint.isFakeBoldText = true
        header.layoutParams = GridLayout.LayoutParams().apply {
            width = 0
            height = GridLayout.LayoutParams.WRAP_CONTENT
            columnSpec = gridColumnSpec
            rowSpec = gridRowSpec
            setMargins(headerMargins, headerMargins * 2, headerMargins, headerMargins)
        }
        header.gravity = Gravity.CENTER_VERTICAL

        return header
    }

    private fun createToolButton(tool: Tool): View {
        // TODO: Move this to the class level
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

        val button = TextView(requireContext())
        button.text = tool.name
        button.setCompoundDrawables(iconSize, left = tool.icon)
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
        button.setOnClickListener { _ ->
            findNavController().navigate(tool.navAction)
        }

        button.setOnLongClickListener { view ->
            Pickers.menu(
                view, listOf(
                    if (tool.description != null) getString(R.string.pref_category_about) else null,
                    if (pinnedToolManager.isPinned(tool.id)) {
                        getString(R.string.unpin)
                    } else {
                        getString(R.string.pin)
                    },
                    if (tool.guideId != null ) getString(R.string.tool_user_guide_title) else null,
                )
            ) { selectedIdx ->
                when (selectedIdx) {
                    0 -> dialog(tool.name, tool.description, cancelText = null)
                    1 -> {
                        if (pinnedToolManager.isPinned(tool.id)) {
                            pinnedToolManager.unpin(tool.id)
                        } else {
                            pinnedToolManager.pin(tool.id)
                        }
                        updatePinnedTools()
                    }
                    2 -> {
                        UserGuideUtils.showGuide(this, tool.guideId!!)
                    }
                }
                true
            }
            true
        }

        return button
    }

}