package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.graphics.drawable.toBitmap
import com.kylecorry.trailsensecore.infrastructure.canvas.getMaskedBitmap
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils

abstract class CanvasView : View {

    protected lateinit var canvas: Canvas
    protected lateinit var fillPaint: Paint
    protected lateinit var strokePaint: Paint
    protected var paintStyle = PaintStyle.Fill
    private var imageMode = ImageMode.Corner
    private var textMode = TextMode.Corner
    private val measurementRect = Rect()
    protected var runEveryCycle: Boolean = true

    private var isSetup = false

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas ?: return
        this.canvas = canvas
        if (!isSetup) {
            fillPaint = Paint()
            fillPaint.style = Paint.Style.FILL
            strokePaint = Paint()
            strokePaint.style = Paint.Style.STROKE
            smooth()
            strokeCap(StrokeCap.Round)
            strokeJoin(StrokeJoin.Miter)
            setup()
            isSetup = true
        }

        draw()
        if (runEveryCycle) {
            invalidate()
        }
    }

    abstract fun setup()

    abstract fun draw()


    // COLOR HELPERS
    // TODO: Handle stroke and fill

    protected fun background(@ColorInt color: Int) {
        canvas.drawColor(color)
    }

    protected fun clear() {
        background(Color.TRANSPARENT)
    }

    @ColorInt
    protected fun color(r: Int, g: Int = r, b: Int = g, a: Int? = null): Int {
        return if (a != null) {
            Color.argb(a, r, g, b)
        } else {
            Color.rgb(r, g, b)
        }
    }

    protected fun fill(@ColorInt color: Int) {
        paintStyle = if (shouldStroke()) {
            PaintStyle.FillAndStroke
        } else {
            PaintStyle.Fill
        }
        fillPaint.color = color
    }

    protected fun tint(@ColorInt color: Int) {
        fillPaint.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        strokePaint.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
    }

    protected fun noTint() {
        fillPaint.colorFilter = null
        strokePaint.colorFilter = null
    }

    protected fun stroke(@ColorInt color: Int) {
        paintStyle = if (shouldFill()) {
            PaintStyle.FillAndStroke
        } else {
            PaintStyle.Stroke
        }
        strokePaint.color = color
    }

    protected fun noStroke() {
        paintStyle = if (shouldFill()) {
            PaintStyle.Fill
        } else {
            PaintStyle.None
        }
    }

    protected fun noFill() {
        paintStyle = if (shouldStroke()) {
            PaintStyle.Stroke
        } else {
            PaintStyle.None
        }
    }

    protected fun strokeWeight(pixels: Float) {
        strokePaint.strokeWidth = pixels
    }

    protected fun strokeCap(cap: StrokeCap) {
        strokePaint.strokeCap = when (cap) {
            StrokeCap.Round -> Paint.Cap.ROUND
            StrokeCap.Square -> Paint.Cap.SQUARE
            StrokeCap.Project -> Paint.Cap.BUTT
        }
    }

    protected fun strokeJoin(join: StrokeJoin) {
        strokePaint.strokeJoin = when (join) {
            StrokeJoin.Miter -> Paint.Join.MITER
            StrokeJoin.Bevel -> Paint.Join.BEVEL
            StrokeJoin.Round -> Paint.Join.ROUND
        }
    }

    protected fun erase() {
        // This may need the following to be called in setup: setLayerType(LAYER_TYPE_HARDWARE, null)
        fillPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        strokePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    protected fun noErase() {
        fillPaint.xfermode = null
        strokePaint.xfermode = null
    }

    protected fun smooth() {
        fillPaint.isAntiAlias = true
        strokePaint.isAntiAlias = true
    }

    protected fun noSmooth() {
        fillPaint.isAntiAlias = false
        strokePaint.isAntiAlias = false
    }

    // TEXT HELPERS
    protected fun textAlign(align: TextAlign) {
        val alignment = when (align) {
            TextAlign.Right -> Paint.Align.RIGHT
            TextAlign.Center -> Paint.Align.CENTER
            TextAlign.Left -> Paint.Align.LEFT
        }
        fillPaint.textAlign = alignment
        strokePaint.textAlign = alignment
    }

    protected fun textSize(pixels: Float) {
        fillPaint.textSize = pixels
        strokePaint.textSize = pixels
    }

    protected fun textStyle(style: TextStyle) {
        val typeface = when (style) {
            TextStyle.Normal -> fillPaint.setTypeface(
                Typeface.create(
                    Typeface.DEFAULT,
                    Typeface.NORMAL
                )
            )
            TextStyle.Italic -> fillPaint.setTypeface(
                Typeface.create(
                    Typeface.DEFAULT,
                    Typeface.ITALIC
                )
            )
            TextStyle.Bold -> fillPaint.setTypeface(
                Typeface.create(
                    Typeface.DEFAULT,
                    Typeface.BOLD
                )
            )
            TextStyle.BoldItalic -> fillPaint.setTypeface(
                Typeface.create(
                    Typeface.DEFAULT,
                    Typeface.BOLD_ITALIC
                )
            )
        }

        fillPaint.typeface = typeface
        strokePaint.typeface = typeface
    }

    protected fun textWidth(text: String): Float {
        return textDimensions(text).first
    }

    protected fun textHeight(text: String): Float {
        return textDimensions(text).second
    }

    protected fun textDimensions(text: String): Pair<Float, Float> {
        // TODO: Factor in stroke
        fillPaint.getTextBounds(text, 0, text.length, measurementRect)
        return measurementRect.width().toFloat() to measurementRect.height().toFloat()
    }

    protected fun textAscent(): Float {
        // TODO: Factor in stroke
        return fillPaint.ascent()
    }

    protected fun textDescent(): Float {
        // TODO: Factor in stroke
        return fillPaint.descent()
    }

    protected fun text(str: String, x: Float, y: Float) {
        if (!shouldDraw()) {
            return
        }

        val realX = if (textMode == TextMode.Center) {
            x - textWidth(str) / 2f
        } else {
            x
        }

        val realY = if (textMode == TextMode.Center) {
            y + textHeight(str) / 2f
        } else {
            y
        }

        if (shouldStroke()) {
            canvas.drawText(str, realX, realY, strokePaint)
        }

        if (shouldFill()) {
            canvas.drawText(str, realX, realY, fillPaint)
        }
    }


    // SHAPE HELPERS
    protected fun arc(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        start: Float,
        stop: Float,
        mode: ArcMode = ArcMode.Pie
    ) {
        if (!shouldDraw()) {
            return
        }
        if (shouldFill()) {
            canvas.drawArc(x, y, x + w, y + h, start, stop - start, mode == ArcMode.Pie, fillPaint)
        }

        if (shouldStroke()) {
            canvas.drawArc(
                x,
                y,
                x + w,
                y + h,
                start,
                stop - start,
                mode == ArcMode.Pie,
                strokePaint
            )
        }
    }

    protected fun ellipse(x: Float, y: Float, w: Float, h: Float = w) {
        if (!shouldDraw()) {
            return
        }

        if (shouldFill()) {
            canvas.drawOval(x, y, x + w, y + h, fillPaint)
        }

        if (shouldStroke()) {
            canvas.drawOval(x, y, x + w, y + h, strokePaint)
        }

    }

    protected fun circle(x: Float, y: Float, diameter: Float) {
        ellipse(x - diameter / 2f, y - diameter / 2f, diameter, diameter)
    }

    protected fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
        if (!shouldDraw()) {
            return
        }

        if (shouldFill()) {
            canvas.drawLine(x1, y1, x2, y2, fillPaint)
        }

        if (shouldStroke()) {
            canvas.drawLine(x1, y1, x2, y2, strokePaint)
        }
    }

    protected fun point(x: Float, y: Float) {
        if (!shouldDraw()) {
            return
        }
        if (shouldFill()) {
            canvas.drawPoint(x, y, fillPaint)
        }

        if (shouldStroke()) {
            canvas.drawPoint(x, y, strokePaint)
        }
    }

    // TODO: Support different radius for each corner
    protected fun rect(
        x: Float,
        y: Float,
        w: Float,
        h: Float = w,
        radius: Float = 0f
    ) {
        if (!shouldDraw()) {
            return
        }
        if (shouldFill()) {
            if (radius == 0f) {
                canvas.drawRect(x, y, x + w, y + h, fillPaint)
            } else {
                canvas.drawRoundRect(x, y, x + w, y + h, radius, radius, fillPaint)
            }
        }

        if (shouldStroke()) {
            if (radius == 0f) {
                canvas.drawRect(x, y, x + w, y + h, strokePaint)
            } else {
                canvas.drawRoundRect(x, y, x + w, y + h, radius, radius, strokePaint)
            }
        }
    }

    protected fun square(
        x: Float,
        y: Float,
        size: Float,
        radius: Float = 0f
    ) {
        rect(x, y, size, size, radius)
    }

    protected fun triangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        if (!shouldDraw()) {
            return
        }

        val path = Path()
        path.moveTo(x1, y1)
        path.lineTo(x2, y2)
        path.lineTo(x3, y3)
        path.lineTo(x1, y1)
        path.close()

        if (shouldFill()) {
            canvas.drawPath(path, fillPaint)
        }

        if (shouldStroke()) {
            canvas.drawPath(path, strokePaint)
        }
    }

    // Transforms
    // TODO: Add the transforms
    protected fun push() {
        canvas.save()
    }

    protected fun pop() {
        canvas.restore()
    }

    protected fun rotate(
        degrees: Float,
        originX: Float = width / 2f,
        originY: Float = height / 2f
    ) {
        canvas.rotate(degrees, originX, originY)
    }

    protected fun scale(x: Float, y: Float = x) {
        canvas.scale(x, y)
    }

    protected fun scale(x: Float, y: Float = x, pivotX: Float, pivotY: Float) {
        canvas.scale(x, y, pivotX, pivotY)
    }

    protected fun translate(x: Float, y: Float) {
        canvas.translate(x, y)
    }

    // Images

    protected fun loadImage(@DrawableRes id: Int, w: Int? = null, h: Int? = null): Bitmap {
        val drawable = UiUtils.drawable(context, id)!!
        return drawable.toBitmap(w ?: drawable.intrinsicWidth, h ?: drawable.intrinsicHeight)
    }

    protected fun image(
        img: Bitmap,
        x: Float,
        y: Float,
        w: Float = img.width.toFloat(),
        h: Float = img.height.toFloat()
    ) {
        if (imageMode == ImageMode.Corner) {
            image(img, x, y, w, h, 0f, 0f)
        } else {
            image(img, x - w / 2f, y - h / 2f, w, h, 0f, 0f)
        }
    }

    protected fun image(
        img: Bitmap,
        dx: Float,
        dy: Float,
        dw: Float,
        dh: Float,
        sx: Float,
        sy: Float,
        sw: Float = img.width.toFloat(),
        sh: Float = img.height.toFloat()
    ) {
        canvas.drawBitmap(
            img,
            Rect(sx.toInt(), sy.toInt(), sw.toInt(), sh.toInt()),
            Rect(dx.toInt(), dy.toInt(), (dx + dw).toInt(), (dy + dh).toInt()),
            fillPaint
        )
    }

    protected fun imageMode(imageMode: ImageMode) {
        this.imageMode = imageMode
    }

    protected fun textMode(textMode: TextMode) {
        this.textMode = textMode
    }

    private fun shouldDraw(): Boolean {
        return paintStyle != PaintStyle.None
    }

    private fun shouldFill(): Boolean {
        return paintStyle == PaintStyle.Fill || paintStyle == PaintStyle.FillAndStroke
    }

    private fun shouldStroke(): Boolean {
        return paintStyle == PaintStyle.Stroke || paintStyle == PaintStyle.FillAndStroke
    }

    // Masks

    protected fun mask(
        mask: Bitmap,
        tempBitmap: Bitmap = Bitmap.createBitmap(
            mask.width,
            mask.height,
            Bitmap.Config.ARGB_8888
        ),
        block: () -> Unit
    ): Bitmap {
        return canvas.getMaskedBitmap(
            mask,
            tempBitmap
        ) {
            val oldCanvas = canvas
            canvas = it
            block()
            canvas = oldCanvas
        }
    }


    protected enum class ArcMode {
        Pie,
        Open
    }

    protected enum class StrokeCap {
        Round,
        Square,
        Project
    }

    protected enum class StrokeJoin {
        Miter,
        Bevel,
        Round
    }

    protected enum class TextAlign {
        Right, Center, Left
    }

    protected enum class TextStyle {
        Normal,
        Italic,
        Bold,
        BoldItalic
    }

    protected enum class PaintStyle {
        Fill,
        Stroke,
        FillAndStroke,
        None
    }

    protected enum class ImageMode {
        Corner,
        Center
    }

    protected enum class TextMode {
        Corner,
        Center
    }

}