package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.kylecorry.trail_sense.R

class FloatingActionButtonMenu(context: Context, attrs: AttributeSet?) : FrameLayout(
    context,
    attrs
) {

    private var onMenuItemClick: MenuItem.OnMenuItemClickListener? = null
    private var overlay: View? = null

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
        if (menuId != -1){
            val p = PopupMenu(context, null)
            p.menuInflater.inflate(menuId, p.menu)
            val menu = p.menu
            for (i in 0 until menu.size()){
                val menuItem = menu.getItem(i)
                val text = menuItem.title
                val icon = menuItem.icon

                val fab = FloatingActionButtonMenuItem(context, null)
                fab.setText(text.toString())
                fab.setImageDrawable(icon)
                fab.setFabOnClickListener {
                    onMenuItemClick?.onMenuItemClick(menuItem)
                }
                fabMenu.addView(fab)
            }
            fabMenu.gravity = Gravity.END
        }
    }

    fun setOverlay(overlay: View){
        overlay.isVisible = isVisible
        this.overlay = overlay
    }

    fun setOnMenuItemClickListener(menuItemClickListener: MenuItem.OnMenuItemClickListener){
        this.onMenuItemClick = menuItemClickListener
    }

    fun hide(){
        isVisible = false
        overlay?.isVisible = false
    }

    fun show(){
        isVisible = true
        overlay?.isVisible = true
    }
}