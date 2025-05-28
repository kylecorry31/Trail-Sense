package com.kylecorry.trail_sense.shared.bitmaps

import android.graphics.Bitmap

class Conditional : BitmapOperation {

    private val predicate: (Bitmap) -> Boolean
    private val operation: BitmapOperation
    private val falseOperation: BitmapOperation

    constructor(
        shouldRun: Boolean,
        operation: BitmapOperation,
        falseOperation: BitmapOperation = NoOp()
    ) {
        predicate = { shouldRun }
        this.operation = operation
        this.falseOperation = falseOperation
    }

    constructor(
        operation: BitmapOperation,
        predicate: (Bitmap) -> Boolean,
        falseOperation: BitmapOperation = NoOp()
    ) {
        this.predicate = predicate
        this.operation = operation
        this.falseOperation = falseOperation
    }

    override fun execute(bitmap: Bitmap): Bitmap {
        return if (predicate(bitmap)) {
            operation.execute(bitmap)
        } else {
            falseOperation.execute(bitmap)
        }
    }
}