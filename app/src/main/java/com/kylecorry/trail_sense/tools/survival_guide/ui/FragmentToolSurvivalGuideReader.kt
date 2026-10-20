package com.kylecorry.trail_sense.tools.survival_guide.ui

import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.widget.NestedScrollView
import com.kylecorry.andromeda.core.ui.ExpansionLayout
import com.kylecorry.andromeda.fragments.useArgument
import com.kylecorry.andromeda.fragments.useBackgroundEffect
import com.kylecorry.trail_sense.shared.views.Toolbar
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseReactiveFragment
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapter
import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapters

class FragmentToolSurvivalGuideReader :
    TrailSenseReactiveFragment(R.layout.fragment_survival_guide) {

    override fun update() {
        val context = useAndroidContext()

        // Views
        val titleView = useView<Toolbar>(R.id.guide_name)
        val scrollView = useView<NestedScrollView>(R.id.guide_scroll)

        // Arguments
        val chapter = useChapter()
        val content = useChapterContent()
        val headerIndex = useArgument<Int>("header_index") ?: 0

        // Render the title bar
        useEffect(titleView, chapter) {
            titleView.title.text = chapter.title
        }

        // Render the content
        useEffect(scrollView, content, headerIndex) {
            if (content == null) {
                return@useEffect
            }
            scrollView.removeAllViews()
            scrollView.addView(
                TextUtils.getMarkdownView(
                    context,
                    content,
                    shouldUppercaseSubheadings = true
                )
            )

            if (headerIndex > 0) {
                // Open the ExpansionLayout at the headerIndex - 1 and scroll to it

                val layout = scrollView.getChildAt(0) as? ViewGroup

                val header = layout?.children
                    ?.filterIsInstance<ExpansionLayout>()
                    ?.elementAtOrNull(headerIndex - 1)

                header?.expand()

                scrollView.post {
                    scrollView.scrollTo(0, header?.top ?: 0)
                }
            }
        }
    }

    private fun useChapter(): Chapter {
        val chapterResourceId = useArgument<Int>("chapter_resource_id")!!
        val context = useAndroidContext()
        return useMemo(context, chapterResourceId) {
            Chapters.getChapters(context).first { it.resource == chapterResourceId }
        }
    }

    private fun useChapterContent(): String? {
        val (content, setContent) = useState<String?>(null)
        val chapterResourceId = useArgument<Int>("chapter_resource_id")!!
        val context = useAndroidContext()
        useBackgroundEffect(context, chapterResourceId) {
            setContent(TextUtils.loadTextFromResources(context, chapterResourceId))
        }
        return content
    }
}
