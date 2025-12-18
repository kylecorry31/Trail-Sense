package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.PathEffect
import android.graphics.Shader
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.kylecorry.andromeda.canvas.ArcMode
import com.kylecorry.andromeda.canvas.CanvasDrawer
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.ImageMode
import com.kylecorry.andromeda.canvas.StrokeCap
import com.kylecorry.andromeda.canvas.StrokeJoin
import com.kylecorry.andromeda.canvas.TextAlign
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.canvas.TextStyle
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.core.tryOrLog

abstract class SurfaceCanvasView : SurfaceView, ICanvasDrawer, SurfaceHolder.Callback {

    override var canvas: Canvas
        get() = drawer.canvas
        set(value) {
            drawer.canvas = value
        }

    protected lateinit var drawer: ICanvasDrawer
    protected var runEveryCycle: Boolean = true
        set(value) {
            field = value
            if (value) {
                thread?.requestDraw()
            }
        }

    private var isSetup = false

    protected var setupAfterVisible = false
    protected var useSoftwareCanvas = false

    private var thread: RenderingThread? = null

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        thread = RenderingThread()
        thread?.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        invalidate()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        thread?.stopRendering()
        thread = null
    }

    override fun invalidate() {
        super.invalidate()
        thread?.requestDraw()
    }

    private inner class RenderingThread : Thread() {
        private var running = true
        private val lock = Object()
        private var needsDraw = true

        fun stopRendering() {
            running = false
            synchronized(lock) {
                lock.notifyAll()
            }
        }

        fun requestDraw() {
            synchronized(lock) {
                needsDraw = true
                lock.notifyAll()
            }
        }

        override fun run() {
            while (running) {
                var shouldDraw = false
                synchronized(lock) {
                    if (running && (runEveryCycle || needsDraw)) {
                        shouldDraw = true
                        needsDraw = false
                    } else if (running) {
                        try {
                            lock.wait()
                        } catch (_: InterruptedException) {
                            // Do nothing
                        }
                        if (running && (runEveryCycle || needsDraw)) {
                            shouldDraw = true
                            needsDraw = false
                        }
                    }
                }

                if (shouldDraw) {
                    val canvas = tryOrDefault(null) {
                        if (!useSoftwareCanvas) {
                            holder.surface.lockHardwareCanvas()
                        } else {
                            holder.lockCanvas()
                        }
                    } ?: continue
                    try {
                        if (!isSetup) {
                            if (setupAfterVisible && !isVisible) {
                                continue
                            }
                            drawer = CanvasDrawer(context, canvas)
                            setup()
                            isSetup = true
                        }

                        drawer.canvas = canvas
                        draw()
                    } finally {
                        tryOrLog {
                            if (!useSoftwareCanvas) {
                                holder.surface.unlockCanvasAndPost(canvas)
                            } else {
                                holder.unlockCanvasAndPost(canvas)
                            }
                        }
                    }
                }
            }
        }
    }

    abstract fun setup()

    abstract fun draw()


    // COLOR HELPERS
    override fun background(@ColorInt color: Int) {
        drawer.background(color)
    }

    override fun clear() {
        drawer.clear()
    }

    @ColorInt
    override fun color(r: Int, g: Int, b: Int, a: Int?): Int {
        return drawer.color(r, g, b, a)
    }

    override fun fill(@ColorInt color: Int) {
        drawer.fill(color)
    }

    override fun tint(@ColorInt color: Int) {
        drawer.tint(color)
    }

    override fun noTint() {
        drawer.noTint()
    }

    override fun stroke(@ColorInt color: Int) {
        drawer.stroke(color)
    }

    override fun pathEffect(effect: PathEffect) {
        drawer.pathEffect(effect)
    }

    override fun noPathEffect() {
        drawer.noPathEffect()
    }

    override fun noStroke() {
        drawer.noStroke()
    }

    override fun noFill() {
        drawer.noFill()
    }

    override fun strokeWeight(pixels: Float) {
        drawer.strokeWeight(pixels)
    }

    override fun strokeCap(cap: StrokeCap) {
        drawer.strokeCap(cap)
    }

    override fun strokeJoin(join: StrokeJoin) {
        drawer.strokeJoin(join)
    }

    override fun opacity(value: Int) {
        drawer.opacity(value)
    }

    override fun erase() {
        drawer.erase()
    }

    override fun noErase() {
        drawer.noErase()
    }

    override fun smooth() {
        drawer.smooth()
    }

    override fun noSmooth() {
        drawer.noSmooth()
    }

    override fun shader(shader: Shader?) {
        drawer.shader(shader)
    }

    // TEXT HELPERS
    override fun textAlign(align: TextAlign) {
        drawer.textAlign(align)
    }

    override fun textSize(pixels: Float) {
        drawer.textSize(pixels)
    }

    override fun textStyle(style: TextStyle) {
        drawer.textStyle(style)
    }

    override fun textWidth(text: String): Float {
        return drawer.textWidth(text)
    }

    override fun textHeight(text: String): Float {
        return drawer.textHeight(text)
    }

    override fun textDimensions(text: String): Pair<Float, Float> {
        return drawer.textDimensions(text)
    }

    override fun pathWidth(path: Path): Float {
        return drawer.pathWidth(path)
    }

    override fun pathHeight(path: Path): Float {
        return drawer.pathHeight(path)
    }

    override fun pathDimensions(path: Path): Pair<Float, Float> {
        return drawer.pathDimensions(path)
    }

    override fun textAscent(): Float {
        return drawer.textAscent()
    }

    override fun textDescent(): Float {
        return drawer.textDescent()
    }

    override fun text(str: String, x: Float, y: Float) {
        drawer.text(str, x, y)
    }


    // SHAPE HELPERS
    override fun arc(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        start: Float,
        stop: Float,
        mode: ArcMode
    ) {
        drawer.arc(x, y, w, h, start, stop, mode)
    }

    override fun ellipse(x: Float, y: Float, w: Float, h: Float) {
        drawer.ellipse(x, y, w, h)
    }

    override fun circle(x: Float, y: Float, diameter: Float) {
        drawer.circle(x, y, diameter)
    }

    override fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
        drawer.line(x1, y1, x2, y2)
    }

    override fun lines(points: FloatArray) {
        drawer.lines(points)
    }

    override fun grid(
        spacing: Float,
        width: Float,
        height: Float
    ) {
        drawer.grid(spacing, width, height)
    }

    override fun point(x: Float, y: Float) {
        drawer.point(x, y)
    }

    // TODO: Support different radius for each corner
    override fun rect(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        radius: Float
    ) {
        drawer.rect(x, y, w, h, radius)
    }

    override fun square(
        x: Float,
        y: Float,
        size: Float,
        radius: Float
    ) {
        drawer.square(x, y, size, radius)
    }

    override fun path(value: Path) {
        drawer.path(value)
    }

    override fun triangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        drawer.triangle(x1, y1, x2, y2, x3, y3)
    }

    // Transforms
    override fun push() {
        drawer.push()
    }

    override fun pop() {
        drawer.pop()
    }

    override fun rotate(
        degrees: Float,
        originX: Float,
        originY: Float
    ) {
        drawer.rotate(degrees, originX, originY)
    }

    override fun scale(x: Float, y: Float) {
        drawer.scale(x, y)
    }

    override fun scale(x: Float, y: Float, pivotX: Float, pivotY: Float) {
        drawer.scale(x, y, pivotX, pivotY)
    }

    override fun translate(x: Float, y: Float) {
        drawer.translate(x, y)
    }

    // Images

    override fun loadImage(@DrawableRes id: Int, w: Int?, h: Int?): Bitmap {
        return drawer.loadImage(id, w, h)
    }

    override fun image(
        img: Bitmap,
        x: Float,
        y: Float,
        w: Float,
        h: Float
    ) {
        drawer.image(img, x, y, w, h)
    }

    override fun image(
        img: Bitmap,
        dx: Float,
        dy: Float,
        dw: Float,
        dh: Float,
        sx: Float,
        sy: Float,
        sw: Float,
        sh: Float
    ) {
        drawer.image(img, dx, dy, dw, dh, sx, sy, sw, sh)
    }

    override fun imageMode(imageMode: ImageMode) {
        drawer.imageMode(imageMode)
    }

    override fun textMode(textMode: TextMode) {
        drawer.textMode(textMode)
    }

    // Masks

    override fun clip(path: Path) {
        drawer.clip(path)
    }

    override fun clipInverse(path: Path) {
        drawer.clipInverse(path)
    }

    override fun mask(
        mask: Bitmap,
        tempBitmap: Bitmap,
        block: () -> Unit
    ): Bitmap {
        return drawer.mask(mask, tempBitmap, block)
    }

    // System

    override fun dp(size: Float): Float {
        return drawer.dp(size)
    }

    override fun sp(size: Float): Float {
        return drawer.sp(size)
    }

}