package com.kylecorry.trail_sense.shared.system

import android.view.View
import com.kylecorry.trail_sense.shared.math.cosDegrees
import com.kylecorry.trail_sense.shared.math.sinDegrees

fun alignToVector(to: View, viewToAlign: View, radius: Float, angle: Float){
    align(viewToAlign,
        VerticalConstraint(to, VerticalConstraintType.Top),
        HorizontalConstraint(to, HorizontalConstraintType.Left),
        VerticalConstraint(to, VerticalConstraintType.Bottom),
        HorizontalConstraint(to, HorizontalConstraintType.Right)
    )
    viewToAlign.x -= cosDegrees(angle.toDouble()).toFloat() * radius
    viewToAlign.y -= sinDegrees(angle.toDouble()).toFloat() * radius
}

enum class VerticalConstraintType {
    Top,
    Bottom
}

enum class HorizontalConstraintType {
    Left,
    Right
}

data class HorizontalConstraint(val view: View, val type: HorizontalConstraintType, val offset: Float = 0f)
data class VerticalConstraint(val view: View, val type: VerticalConstraintType, val offset: Float = 0f)

fun align(view: View, top: VerticalConstraint?, left: HorizontalConstraint?, bottom: VerticalConstraint?, right: HorizontalConstraint?, verticalPct: Float = 0.5f, horizontalPct: Float = 0.5f){

    if (top != null && bottom == null){
        view.y = (if (top.type == VerticalConstraintType.Top) top.view.y else (top.view.y + top.view.height)) + top.offset
    } else if (bottom != null && top == null){
        view.y = (if (bottom.type == VerticalConstraintType.Top) bottom.view.y  else (bottom.view.y + bottom.view.height)) + bottom.offset - view.height
    } else if (bottom != null && top != null){
        val t = (if (top.type == VerticalConstraintType.Top) top.view.y else (top.view.y + top.view.height)) + top.offset
        val b = (if (bottom.type == VerticalConstraintType.Top) bottom.view.y  else (bottom.view.y + bottom.view.height)) + bottom.offset
        val range = b - t
        view.y = t + range * verticalPct - view.height / 2f
    } else {
        // Do nothing
    }

    if (left != null && right == null){
        view.x = (if (left.type == HorizontalConstraintType.Left) left.view.x else (left.view.x + left.view.width)) + left.offset
    } else if (right != null && left == null){
        view.x = (if (right.type == HorizontalConstraintType.Left) right.view.x  else (right.view.x + right.view.width)) + right.offset - view.width
    } else if (left != null && right != null){
        val l = (if (left.type == HorizontalConstraintType.Left) left.view.x else (left.view.x + left.view.width)) + left.offset
        val r = (if (right.type == HorizontalConstraintType.Left) right.view.x  else (right.view.x + right.view.width)) + right.offset
        val range = r - l
        view.x = l + range * horizontalPct - view.width / 2f
    } else {
        // Do nothing
    }
}