package com.kylecorry.trail_sense.tools.dashboard.ui

import com.google.android.flexbox.FlexboxLayout
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.extensions.useDestroyEffect
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetViewBinder

class DashboardFragment : TrailSenseReactiveFragment(R.layout.fragment_tool_dashboard) {

    override fun update() {
        val widgetContainerView = useView<FlexboxLayout>(R.id.widgets)
        val binder = useMemo(widgetContainerView) {
            ToolWidgetViewBinder(this, widgetContainerView, showBackground = false)
        }

        useEffectWithCleanup(binder) {
            binder.bind()
            return@useEffectWithCleanup { binder.unbind() }
        }

        useDestroyEffect(binder) {
            binder.unbind()
        }

    }
}
