package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.core.view.isVisible
import com.google.android.material.floatingactionbutton.FloatingActionButton
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
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.FloatingActionButtonMenu,
            0,
            0
        )
        val menuId = a.getResourceId(R.styleable.FloatingActionButtonMenu_menu, -1)
        a.recycle()
        if (menuId != -1) {
            val p = PopupMenu(context, null)
            p.menuInflater.inflate(menuId, p.menu)
            val menu = p.menu
            for (i in 0 until menu.size()) {
                val menuItem = menu.getItem(i)
                val text = menuItem.title
                val icon = menuItem.icon

                val fab = FloatingActionButtonMenuItem(context, null)
                fab.setText(text.toString())
                fab.setImageDrawable(icon)
                fab.setItemOnClickListener {
                    if (hideOnMenuOptionSelected) {
                        hide()
                    }
                    onMenuItemClick?.onMenuItemClick(menuItem)
                }
                fabMenu.addView(fab)
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