package com.kylecorry.trail_sense.shared.andromeda_temporary

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.google.android.material.datepicker.DayViewDecorator
import com.kylecorry.andromeda.core.system.Resources
import java.time.LocalDate

open class AndromedaDayViewDecorator {
    open fun getBottomDrawable(context: Context, date: LocalDate, isSelected: Boolean): Drawable? {
        return null
    }

    open fun getTopDrawable(context: Context, date: LocalDate, isSelected: Boolean): Drawable? {
        return null
    }

    open fun getLeftDrawable(context: Context, date: LocalDate, isSelected: Boolean): Drawable? {
        return null
    }

    open fun getRightDrawable(context: Context, date: LocalDate, isSelected: Boolean): Drawable? {
        return null
    }

    open fun getBackgroundColor(
        context: Context,
        date: LocalDate,
        isSelected: Boolean
    ): ColorStateList? {
        return null
    }

    open fun getTextColor(context: Context, date: LocalDate, isSelected: Boolean): ColorStateList? {
        return null
    }

    protected fun createIndicatorDrawable(
        context: Context,
        @DrawableRes drawableId: Int,
        size: Int,
        @ColorInt color: Int? = null,
        ): Drawable? {
        val drawable = Resources.drawable(context, drawableId) ?: return null
        if (color != null) {
            drawable.setTint(color)
        }

        val insetDrawable = InsetDrawable(drawable, 0, 0, 0, 0)
        insetDrawable.setBounds(0, 0, size, size)
        return insetDrawable
    }

    protected fun createEmptyIndicatorDrawable(size: Int): Drawable {
        val spacer = ColorDrawable(Color.TRANSPARENT)
        spacer.setBounds(0, 0, size, size)
        return spacer
    }

    protected fun createIndicatorDrawableGrid(
        drawables: List<Drawable>,
        columns: Int,
        size: Int,
        gap: Int
    ): Drawable {
        val rows = (drawables.size + columns - 1) / columns
        val totalWidth = columns * size + (columns - 1) * gap
        val totalHeight = rows * size + (rows - 1) * gap

        val layerDrawable = LayerDrawable(drawables.toTypedArray())

        for (i in drawables.indices) {
            val x = i % columns
            val y = i / columns

            val left = x * (size + gap)
            val top = y * (size + gap)
            val right = totalWidth - (left + size)
            val bottom = totalHeight - (top + size)

            layerDrawable.setLayerInset(i, left, top, right, bottom)
        }

        // Center the last row if it has fewer than 'columns' items
        val lastRowItemCount = drawables.size % columns
        if (lastRowItemCount != 0) {
            val lastRowOffset = ((columns - lastRowItemCount) * (size + gap)) / 2
            for (i in (drawables.size - lastRowItemCount) until drawables.size) {
                val x = i % columns
                val y = i / columns

                val left = x * (size + gap) + lastRowOffset
                val top = y * (size + gap)
                val right = totalWidth - (left + size)
                val bottom = totalHeight - (top + size)

                layerDrawable.setLayerInset(i, left, top, right, bottom)
            }
        }

        val insetDrawable = InsetDrawable(layerDrawable, 0, 0, 0, 0)
        insetDrawable.setBounds(0, 0, totalWidth, totalHeight)
        return insetDrawable
    }

    fun toMaterialDayViewDecorator(): DayViewDecorator {
        return object : DayViewDecorator() {
            override fun describeContents(): Int {
                return 0
            }

            override fun writeToParcel(dest: Parcel, flags: Int) {
            }

            override fun getBackgroundColor(
                context: Context,
                year: Int,
                month: Int,
                day: Int,
                valid: Boolean,
                selected: Boolean
            ): ColorStateList? {
                if (!valid) {
                    return null
                }
                val date = LocalDate.of(year, month + 1, day)
                return getBackgroundColor(context, date, selected)
            }

            override fun getCompoundDrawableTop(
                context: Context,
                year: Int,
                month: Int,
                day: Int,
                valid: Boolean,
                selected: Boolean
            ): Drawable? {
                if (!valid) {
                    return null
                }
                val date = LocalDate.of(year, month + 1, day)
                return getTopDrawable(context, date, selected)
            }

            override fun getCompoundDrawableBottom(
                context: Context,
                year: Int,
                month: Int,
                day: Int,
                valid: Boolean,
                selected: Boolean
            ): Drawable? {
                if (!valid) {
                    return null
                }
                val date = LocalDate.of(year, month + 1, day)
                return getBottomDrawable(context, date, selected)
            }

            override fun getCompoundDrawableLeft(
                context: Context,
                year: Int,
                month: Int,
                day: Int,
                valid: Boolean,
                selected: Boolean
            ): Drawable? {
                if (!valid) {
                    return null
                }
                val date = LocalDate.of(year, month + 1, day)
                return getLeftDrawable(context, date, selected)
            }

            override fun getCompoundDrawableRight(
                context: Context,
                year: Int,
                month: Int,
                day: Int,
                valid: Boolean,
                selected: Boolean
            ): Drawable? {
                if (!valid) {
                    return null
                }
                val date = LocalDate.of(year, month + 1, day)
                return getRightDrawable(context, date, selected)
            }

            override fun getTextColor(
                context: Context,
                year: Int,
                month: Int,
                day: Int,
                valid: Boolean,
                selected: Boolean
            ): ColorStateList? {
                if (!valid) {
                    return null
                }
                val date = LocalDate.of(year, month + 1, day)
                return getTextColor(context, date, selected)
            }
        }
    }
}

open class CustomDayViewDecorator() : DayViewDecorator() {
    constructor(parcel: Parcel) : this() {
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
    }

    companion object CREATOR : Parcelable.Creator<CustomDayViewDecorator> {
        override fun createFromParcel(parcel: Parcel): CustomDayViewDecorator {
            return CustomDayViewDecorator(parcel)
        }

        override fun newArray(size: Int): Array<CustomDayViewDecorator?> {
            return arrayOfNulls(size)
        }
    }

}