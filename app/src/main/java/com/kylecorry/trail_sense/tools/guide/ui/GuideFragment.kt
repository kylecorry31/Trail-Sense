package com.kylecorry.trail_sense.tools.guide.ui

import androidx.core.widget.NestedScrollView
import com.kylecorry.andromeda.fragments.useArgument
import com.kylecorry.andromeda.fragments.useBackgroundEffect
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.shared.views.Toolbar

class GuideFragment : TrailSenseReactiveFragment(R.layout.fragment_guide) {

    override fun update() {
        val context = useAndroidContext()

        // Views
        val titleView = useView<Toolbar>(R.id.guide_name)
        val scrollView = useView<NestedScrollView>(R.id.guide_scroll)

        // Arguments
        val name = useArgument<String>("guide_name") ?: ""
        val resource = useArgument<Int>("guide_contents")

        // State
        val (content, setContent) = useState<String?>(null)

        useBackgroundEffect(context, resource) {
            resource?.let { setContent(TextUtils.loadTextFromResources(context, it)) }
        }

        useEffect(titleView, name) {
            titleView.title.text = name
        }

        useEffect(scrollView, content) {
            if (content == null) {
                return@useEffect
            }
            scrollView.removeAllViews()
            scrollView.addView(TextUtils.getMarkdownView(context, content))
        }
    }
}
