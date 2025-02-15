package com.kylecorry.trail_sense.tools.tools.ui.widgets

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.flexbox.FlexboxLayout
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.luna.coroutines.onDefault
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.trail_sense.databinding.ViewQuickActionSheetBinding
import com.kylecorry.trail_sense.settings.SettingsToolRegistration
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolSummarySize
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolWidget
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class ToolWidgetViewBinder(
    private val fragment: Fragment,
    private val binding: ViewQuickActionSheetBinding
) {

    private val widgets = mutableListOf<WidgetInstance>()
    private val prefs by lazy { UserPreferences(fragment.requireContext()) }
    private val context by lazy { fragment.requireContext() }

    private val lifecycleEventObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> onResume()
            Lifecycle.Event.ON_PAUSE -> onPause()
            Lifecycle.Event.ON_DESTROY -> onDestroy()
            else -> {}
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun bind() {
        val selectedWidgets = prefs.toolWidgets
        val allWidgets = Tools.getTools(context)
            .flatMap { it.widgets }
            .filter { it.isEnabled(context) && it.canPlaceInApp }
            .sortedByDescending { it.size.ordinal }

        // Only show the selected widgets
        val widgets = allWidgets.filter { selectedWidgets.contains(it.id) }

        binding.widgets.removeAllViews()

        // 2 cells on a Pixel phone = 102dp
        val halfSummaryHeight = Resources.dp(context, 102f).toInt()
        val fullSummaryHeight = halfSummaryHeight * 2
        val summaryGap = Resources.dp(context, 4f).toInt()

        widgets.forEachIndexed { index, widget ->
            val layout = AppWidgetHostView(context)
            layout.setAppWidget(
                index,
                AppWidgetManager.getInstance(context).installedProviders?.find {
                    it.provider == ComponentName(
                        context,
                        widget.widgetClass
                    )
                }
            )

            layout.layoutParams = FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.MATCH_PARENT,
                if (widget.size == ToolSummarySize.Full) fullSummaryHeight else halfSummaryHeight
            ).apply {
                flexBasisPercent = when (widget.size) {
                    ToolSummarySize.Full -> 1f
                    ToolSummarySize.Half -> 0.5f
                }
            }
            layout.setPadding(summaryGap)

            binding.widgets.addView(layout)

            val updateFunction = {
                fragment.inBackground {
                    val views = onDefault {
                        widget.widgetView.getPopulatedView(context)
                    }
                    onMain {
                        tryOrLog {
                            layout.updateAppWidget(views)
                            layout.getChildAt(0)?.backgroundTintList =
                                ColorStateList.valueOf(
                                    Resources.androidBackgroundColorSecondary(
                                        context
                                    )
                                )
                        }
                    }
                }
                Unit
            }

            this.widgets.add(WidgetInstance(widget, updateFunction))
            val widgetView =
                widget.widgetView.getView(context)
            layout.updateAppWidget(widgetView)
            layout.getChildAt(0)?.backgroundTintList =
                ColorStateList.valueOf(Resources.androidBackgroundColorSecondary(context))
        }

        this.widgets.forEach {
            it.widget.widgetView.onInAppEvent(
                context,
                Lifecycle.Event.ON_CREATE,
                it.updateFunction
            )
        }

        fragment.lifecycle.addObserver(lifecycleEventObserver)
    }

    fun unbind() {
        fragment.lifecycle.removeObserver(lifecycleEventObserver)
        onPause()
        onDestroy()
    }

    private fun onResume() {
        Tools.subscribe(SettingsToolRegistration.BROADCAST_UPDATE_IN_APP_WIDGET, this::onUpdate)

        // TODO: Get a GPS / elevation reading

        // Update all widgets
        widgets.forEach { it.updateFunction() }
        this.widgets.forEach {
            it.widget.widgetView.onInAppEvent(
                context,
                Lifecycle.Event.ON_RESUME,
                it.updateFunction
            )
        }
    }

    private fun onPause() {
        Tools.unsubscribe(SettingsToolRegistration.BROADCAST_UPDATE_IN_APP_WIDGET, this::onUpdate)
        this.widgets.forEach {
            it.widget.widgetView.onInAppEvent(
                context,
                Lifecycle.Event.ON_PAUSE,
                it.updateFunction
            )
        }
    }

    private fun onDestroy() {
        this.widgets.forEach {
            it.widget.widgetView.onInAppEvent(
                context,
                Lifecycle.Event.ON_DESTROY,
                it.updateFunction
            )
        }
        this.widgets.clear()
    }

    private fun onUpdate(data: Bundle): Boolean {
        val widgetId = data.getString("widgetId") ?: return true
        val widget = widgets.find { it.widget.id == widgetId } ?: return true
        widget.updateFunction()
        return true
    }

    private data class WidgetInstance(
        val widget: ToolWidget,
        val updateFunction: () -> Unit = {}
    )
}