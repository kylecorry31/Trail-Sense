package com.kylecorry.trail_sense.shared.sharing

import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.fragments.show

object Share {

    fun share(
        fragment: Fragment,
        title: String,
        actions: List<ShareAction>,
        onAction: (action: ShareAction?) -> Unit
    ) {
        var called = false

        val customOnAction = { action: ShareAction?, sheet: ShareSheet ->
            if (!called) {
                called = true
                if (action != null) {
                    sheet.dismiss()
                }
                onAction(action)
            }
        }
        val sheet = ShareSheet(title, actions, customOnAction)
        sheet.show(fragment)
    }

}

enum class ShareAction {
    Copy,
    QR,
    Maps,
    Send,
    File
}