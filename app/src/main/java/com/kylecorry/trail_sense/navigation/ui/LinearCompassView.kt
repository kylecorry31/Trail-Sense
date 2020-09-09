package com.kylecorry.trail_sense.navigation.ui

/**
 * Adapted from https://github.com/RedInput/CompassView
 *
 * View original license: https://github.com/RedInput/CompassView/blob/master/LICENSE
 */

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.compass.CompassDirection
import com.kylecorry.trail_sense.shared.FormatService
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

class LinearCompassView(
    context: Context,
    attrs: AttributeSet?
) : View(context, attrs) {
    private var mTextPaint: Paint? = null
    private var mMainLinePaint: Paint? = null
    private var mSecondaryLinePaint: Paint? = null
    private var mTerciaryLinePaint: Paint? = null
    private var mCardinalLinePaint: Paint? = null
    private var mMarkerPaint: Paint? = null
    private var pathMarker: Path? = null
    private var mTextColor: Int
    private var mBackgroundColor: Int
    private var mLineColor: Int
    private var mCardinalLineColor: Int
    private var mMarkerColor: Int
    private var mDegrees: Float
    private var mTextSize: Float
    private var mRangeDegrees: Float
    private var mShowMarker: Boolean
    private val formatService = FormatService(context)


    private fun init() {
        mTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mTextPaint!!.textAlign = Paint.Align.CENTER
        mMainLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mMainLinePaint!!.strokeWidth = 8f
        mCardinalLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mCardinalLinePaint!!.strokeWidth = 8f
        mSecondaryLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mSecondaryLinePaint!!.strokeWidth = 6f
        mTerciaryLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mTerciaryLinePaint!!.strokeWidth = 3f
        mMarkerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mMarkerPaint!!.style = Paint.Style.FILL
        pathMarker = Path()
    }

    override fun onSaveInstanceState(): Parcelable? {
        val b = Bundle()
        b.putParcelable("instanceState", super.onSaveInstanceState())
        b.putFloat("degrees", mDegrees)
        return b
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        var state: Parcelable? = state
        if (state is Bundle) {
            val b = state
            mDegrees = b.getFloat("degrees", 0f)
            state = b.getParcelable("instanceState")
        }
        super.onRestoreInstanceState(state)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec))
    }

    private fun measureWidth(measureSpec: Int): Int {
        var result = 0
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        val minWidth =
            floor(50 * resources.displayMetrics.density.toDouble()).toInt()
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize
        } else {
            result = minWidth + paddingLeft + paddingRight
            if (specMode == MeasureSpec.AT_MOST) {
                result = min(result, specSize)
            }
        }
        return result
    }

    private fun measureHeight(measureSpec: Int): Int {
        var result = 0
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        val minHeight =
            floor(30 * resources.displayMetrics.density.toDouble()).toInt()
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize
        } else {
            result = minHeight + paddingTop + paddingBottom
            if (specMode == MeasureSpec.AT_MOST) {
                result = min(result, specSize)
            }
        }
        return result
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mTextPaint!!.color = mTextColor
        mTextPaint!!.textSize = mTextSize
        mMainLinePaint!!.color = mCardinalLineColor
        mSecondaryLinePaint!!.color = mLineColor
        mTerciaryLinePaint!!.color = mLineColor
        mMarkerPaint!!.color = mMarkerColor
        canvas.drawColor(mBackgroundColor)
        val width = measuredWidth
        val height = measuredHeight
        val paddingTop = paddingTop
        val paddingBottom = paddingBottom
        val paddingLeft = paddingLeft
        val paddingRight = paddingRight
        val unitHeight = (height - paddingTop - paddingBottom) / 12
        val pixDeg = (width - paddingLeft - paddingRight) / mRangeDegrees
        val minDegrees = (mDegrees - mRangeDegrees / 2).roundToInt()
        val maxDegrees = (mDegrees + mRangeDegrees / 2).roundToInt()
        var i = -180
        while (i < 540) {
            if (i in minDegrees..maxDegrees) {
                val paint = when {
                    i % 45 == 0 -> mMainLinePaint!!
                    else -> mSecondaryLinePaint!!
                }
                when {
                    i % 90 == 0 -> {
                        canvas.drawLine(
                            paddingLeft + pixDeg * (i - minDegrees),
                            height - paddingBottom.toFloat(),
                            paddingLeft + pixDeg * (i - minDegrees),
                            6 * unitHeight + paddingTop.toFloat(),
                            paint
                        )
                        val coord = when (i) {
                            -90, 270 -> formatService.formatDirection(CompassDirection.West)
                            0, 360 -> formatService.formatDirection(CompassDirection.North)
                            90, 450 -> formatService.formatDirection(CompassDirection.East)
                            -180, 180 -> formatService.formatDirection(CompassDirection.South)
                            else -> ""
                        }
                        canvas.drawText(
                            coord, paddingLeft + pixDeg * (i - minDegrees), (5 * unitHeight
                                    + paddingTop).toFloat(), mTextPaint!!
                        )
                    }
                    i % 15 == 0 -> {
                        canvas.drawLine(
                            paddingLeft + pixDeg * (i - minDegrees),
                            height - paddingBottom.toFloat(),
                            paddingLeft + pixDeg * (i - minDegrees),
                            8 * unitHeight + paddingTop.toFloat(),
                            paint
                        )
                    }
                    else -> {
                        canvas.drawLine(
                            paddingLeft + pixDeg * (i - minDegrees),
                            height - paddingBottom.toFloat(),
                            paddingLeft + pixDeg * (i - minDegrees),
                            10 * unitHeight + paddingTop.toFloat(),
                            paint
                        )
                    }
                }
            }
            i += 5
        }
        if (mShowMarker) {
            pathMarker!!.moveTo(width / 2.toFloat(), 1 * unitHeight + paddingTop.toFloat())
            pathMarker!!.lineTo(width / 2 + 20.toFloat(), paddingTop.toFloat())
            pathMarker!!.lineTo(width / 2 - 20.toFloat(), paddingTop.toFloat())
            pathMarker!!.close()
            canvas.drawPath(pathMarker!!, mMarkerPaint!!)
        }
    }

    fun setDegrees(degrees: Float) {
        mDegrees = degrees
        invalidate()
        requestLayout()
    }

    override fun setBackgroundColor(color: Int) {
        mBackgroundColor = color
        invalidate()
        requestLayout()
    }

    init {
        val a =
            context.theme.obtainStyledAttributes(attrs, R.styleable.LinearCompassView, 0, 0)
        mBackgroundColor =
            a.getColor(R.styleable.LinearCompassView_backgroundColor, Color.BLACK)
        mMarkerColor = a.getColor(R.styleable.LinearCompassView_markerColor, Color.RED)
        mShowMarker = a.getBoolean(R.styleable.LinearCompassView_showMarker, true)
        mLineColor =
            a.getColor(R.styleable.LinearCompassView_lineColor, Color.WHITE)
        mCardinalLineColor =
            a.getColor(R.styleable.LinearCompassView_cardinalLineColor, Color.WHITE)
        mTextColor =
            a.getColor(R.styleable.LinearCompassView_textColor, Color.WHITE)
        mTextSize = a.getDimension(
            R.styleable.LinearCompassView_textSize,
            15 * resources.displayMetrics.scaledDensity
        )
        mDegrees = a.getFloat(R.styleable.LinearCompassView_degrees, 0f)
        mRangeDegrees = a.getFloat(R.styleable.LinearCompassView_rangeDegrees, 180f)
        a.recycle()
        init()
    }
}