package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.core.view.updateMargins
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R

class FloatingActionButtonMenu(context: Context, attrs: AttributeSet?) : FrameLayout(
    context,
    attrs
) {

    private var onMenuItemClick: MenuItem.OnMenuItemClickListener? = null
    private var onHideAction: (() -> Unit)? = null
    private var onShowAction: (() -> Unit)? = null
    private var overlay: View? = null

    var fab: FloatingActionButton? = null
        set(value) {
            if (value == null && field != null) {
                field?.setOnClickListener(null)
            } else {
                value?.setOnClickListener { toggle() }
            }
            field = value
        }

    var hideOnMenuOptionSelected = false

    init {
        inflate(context, R.layout.view_floating_action_button_menu, this)
        val fabMenu = findViewById<LinearLayout>(R.id.fab_menu)

        var menuId = -1
        parse(attrs, R.styleable.FloatingActionButtonMenu) {
            menuId = getResourceId(R.styleable.FloatingActionButtonMenu_menu, -1)
        }

        if (menuId != -1) {
            val items = Resources.menuItems(context, menuId)
            val itemSpacing = Resources.dp(context, 12f).toInt()
            for (menuItem in items) {
                val item = ExtendedFloatingActionButton(context).apply {
                    text = menuItem.title
                    icon = menuItem.icon
                    setOnClickListener {
                        if (hideOnMenuOptionSelected) {
                            this@FloatingActionButtonMenu.hide()
                        }
                        onMenuItemClick?.onMenuItemClick(menuItem)
                    }
                }
                val params = LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT
                ).apply {
                    updateMargins(bottom = itemSpacing)
                }
                fabMenu.addView(item, params)
            }
            fabMenu.gravity = Gravity.END
        }
    }

    fun setOverlay(overlay: View) {
        this.overlay?.setOnClickListener(null)
        overlay.isVisible = isVisible
        this.overlay = overlay
        this.overlay?.setOnClickListener {
            hide()
        }
    }

    fun setOnMenuItemClickListener(menuItemClickListener: MenuItem.OnMenuItemClickListener) {
        this.onMenuItemClick = menuItemClickListener
    }

    fun setOnHideListener(listener: () -> Unit) {
        this.onHideAction = listener
    }

    fun setOnShowListener(listener: () -> Unit) {
        this.onShowAction = listener
    }

    fun hide() {
        val wasVisible = isVisible
        isVisible = false
        overlay?.isVisible = false
        fab?.setImageResource(R.drawable.ic_add)
        if (wasVisible) {
            onHideAction?.invoke()
        }
    }

    fun show() {
        val wasHidden = !isVisible
        isVisible = true
        overlay?.isVisible = true
        fab?.setImageResource(R.drawable.ic_cancel)
        if (wasHidden) {
            onShowAction?.invoke()
        }
    }

    fun toggle() {
        if (isVisible) {
            hide()
        } else {
            show()
        }
    }
}
