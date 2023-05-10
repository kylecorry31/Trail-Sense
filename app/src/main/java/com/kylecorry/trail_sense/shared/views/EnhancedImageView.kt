package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.kylecorry.andromeda.canvas.CanvasDrawer
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.normalizeAngle
import com.kylecorry.sol.math.SolMath.roundNearest
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import kotlin.math.max

// TODO: Fix panning and zooming while rotated
open class EnhancedImageView : SubsamplingScaleImageView {

    protected lateinit var drawer: ICanvasDrawer
    private var isSetup = false

    protected var imageRotation = 0f
        set(value) {
            field = value
            refreshRequiredTiles(true)
            invalidate()
        }

    private val imageClipPath = Path()
    private val lookupMatrix = Matrix()
    private val files = FileSubsystem.getInstance(context)
    private var lastImage: String? = null
    private var lastScale = 1f
    private var lastTranslateX = 0f
    private var lastTranslateY = 0f
    private var rotationOffset = 0f

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)


    override fun onDraw(canvas: Canvas?) {
        if (isSetup && canvas != null) {
            drawer.canvas = canvas
            drawer.push()
            drawer.rotate(-(imageRotation - rotationOffset))
        }

        super.onDraw(canvas)
        if (!isReady || canvas == null) {
            return
        }

        if (!isSetup) {
            drawer = CanvasDrawer(context, canvas)
            mySetup()
            isSetup = true
        }

        myDraw()
        // TODO: Use a flag instead
        tryOrNothing {
            drawer.pop()
        }

        drawOverlay()
    }

    private fun mySetup() {
        setPanLimit(PAN_LIMIT_OUTSIDE)
        maxScale = 6f
        alwaysZoomDoubleTap = true
        alwaysZoomDoubleTapZoomScale = 2f
        setup()
    }

    private fun myDraw() {
        imageClipPath.apply {
            rewind()
            val topLeft = toView(0f, 0f) ?: return@apply
            val bottomRight = toView(imageWidth.toFloat(), imageHeight.toFloat()) ?: return@apply
            addRect(
                topLeft.x,
                topLeft.y,
                bottomRight.x,
                bottomRight.y,
                Path.Direction.CW
            )
        }

        if (scale != lastScale) {
            onScaleChanged(lastScale, scale)
            lastScale = scale
        }

        vTranslate?.let {
            if (it.x != lastTranslateX || it.y != lastTranslateY) {
                onTranslateChanged(lastTranslateX, lastTranslateY, it.x, it.y)
                lastTranslateX = it.x
                lastTranslateY = it.y
            }
        }

        preDraw()

        drawer.push()
        drawer.clip(imageClipPath)
        draw()
        drawer.pop()

        postDraw()
    }

    protected open fun postDraw() {
        // Do nothing
    }

    protected open fun drawOverlay() {
        // Do nothing
    }

    protected open fun preDraw() {
        // Do nothing
    }

    protected open fun draw() {
        // Do nothing
    }

    protected open fun setup() {

    }

    fun setImage(filename: String, rotation: Int = 0) {
        val baseRotation = getBaseRotation(rotation)
        if (orientation != baseRotation) {
            orientation = when (baseRotation) {
                90 -> ORIENTATION_90
                180 -> ORIENTATION_180
                270 -> ORIENTATION_270
                else -> ORIENTATION_0
            }
        }
        rotationOffset = SolMath.deltaAngle(
            baseRotation.toFloat(),
            rotation.toFloat()
        )
        if (lastImage != filename) {
            val uri = files.uri(filename)
            setImage(ImageSource.uri(uri))
            lastImage = filename
        }
        invalidate()
    }

    private fun getBaseRotation(rotation: Int): Int {
        return normalizeAngle(rotation.toFloat()).toInt().roundNearest(90)
    }

    override fun tileVisible(tile: Tile?): Boolean {
        // No need to check if the image is not rotated
        if (imageRotation == 0f) {
            return super.tileVisible(tile)
        }

        val rect = tile?.sRect ?: return false

        // Calculate the bounds of the visible source area (factoring in rotation)
        val topLeftSource = toSource(0f, 0f, true)
        val topRightSource = toSource(width.toFloat(), 0f, true)
        val bottomRightSource = toSource(width.toFloat(), height.toFloat(), true)
        val bottomLeftSource = toSource(0f, height.toFloat(), true)

        val sVisLeft = minOf(
            topLeftSource?.x ?: 0f,
            topRightSource?.x ?: 0f,
            bottomRightSource?.x ?: 0f,
            bottomLeftSource?.x ?: 0f
        )
        val sVisRight = maxOf(
            topLeftSource?.x ?: 0f,
            topRightSource?.x ?: 0f,
            bottomRightSource?.x ?: 0f,
            bottomLeftSource?.x ?: 0f
        )
        val sVisTop = minOf(
            topLeftSource?.y ?: 0f,
            topRightSource?.y ?: 0f,
            bottomRightSource?.y ?: 0f,
            bottomLeftSource?.y ?: 0f
        )
        val sVisBottom = maxOf(
            topLeftSource?.y ?: 0f,
            topRightSource?.y ?: 0f,
            bottomRightSource?.y ?: 0f,
            bottomLeftSource?.y ?: 0f
        )

        // Check to see if the tile is within the visible source area
        return sVisLeft > rect.right || rect.left > sVisRight || sVisTop > rect.bottom || rect.top <= sVisBottom

    }

    override fun onImageLoaded() {
        super.onImageLoaded()
        invalidate()
    }

    protected open fun onScaleChanged(oldScale: Float, newScale: Float) {
        // Do nothing
    }

    protected open fun onTranslateChanged(
        oldTranslateX: Float,
        oldTranslateY: Float,
        newTranslateX: Float,
        newTranslateY: Float
    ) {
        // Do nothing
    }

    fun recenter() {
        resetScaleAndCenter()
    }

    fun moveTo(x: Float, y: Float) {
        setScaleAndCenter(scale, PointF(x, y))
    }

    fun zoomBy(multiple: Float) {
        requestScale((scale * multiple).coerceIn(minScale, max(2 * minScale, maxScale)))
    }

    protected fun toView(sourceX: Float, sourceY: Float, withRotation: Boolean = false): PointF? {
        val view = sourceToViewCoord(sourceX, sourceY)
        if (!withRotation) {
            return view
        }
        val point = floatArrayOf(view?.x ?: 0f, view?.y ?: 0f)
        lookupMatrix.reset()
        lookupMatrix.postRotate(-imageRotation, width / 2f, height / 2f)
        lookupMatrix.mapPoints(point)
        view?.x = point[0]
        view?.y = point[1]
        return view
    }

    protected fun toSource(viewX: Float, viewY: Float, withRotation: Boolean = false): PointF? {
        if (!withRotation) {
            return viewToSourceCoord(viewX, viewY)
        }
        val point = floatArrayOf(viewX, viewY)
        lookupMatrix.reset()
        lookupMatrix.postRotate(-imageRotation, width / 2f, height / 2f)
        lookupMatrix.invert(lookupMatrix)
        lookupMatrix.mapPoints(point)
        return viewToSourceCoord(point[0], point[1])
    }

    val imageWidth: Int
        get() {
            return if (orientation == 90 || orientation == 270) {
                sHeight
            } else {
                sWidth
            }
        }

    val imageHeight: Int
        get() {
            return if (orientation == 90 || orientation == 270) {
                sWidth
            } else {
                sHeight
            }
        }

    protected open fun onLongPress(e: MotionEvent) {
        // Do nothing
    }

    protected open fun onSinglePress(e: MotionEvent) {
        // Do nothing
    }

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)

            // Don't invoke if it is currently scaling
            if (isZooming || isQuickScaling) return

            this@EnhancedImageView.onLongPress(e)
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            this@EnhancedImageView.onSinglePress(e)
            return super.onSingleTapConfirmed(e)
        }
    }

    private val gestureDetector = GestureDetector(context, gestureListener)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val consumed = gestureDetector.onTouchEvent(event)
        return consumed || super.onTouchEvent(event)
    }

}