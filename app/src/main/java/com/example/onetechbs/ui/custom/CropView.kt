package com.example.onetechbs.ui.custom

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.max
import kotlin.math.min

class CropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs) {

    private var bitmap: Bitmap? = null
    private val imageMatrix = Matrix()

    // Paint for the semi-transparent overlay
    private val overlayPaint = Paint().apply {
        color = Color.parseColor("#BB000000") // Semi-transparent black
        style = Paint.Style.FILL
    }

    // Paint for the crop window border
    private val borderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val cropRect = RectF()
    private var cropCirclePath = Path()

    // Gesture detectors
    private val scaleDetector: ScaleGestureDetector
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var activePointerId = MotionEvent.INVALID_POINTER_ID

    private var scaleFactor = 1.0f
    private val minScale = 0.5f
    private val maxScale = 5.0f

    init {
        scaleDetector = ScaleGestureDetector(context, ScaleListener())
    }

    fun setImageUri(uri: Uri) {
        val inputStream = context.contentResolver.openInputStream(uri)
        bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        resetImageMatrix()
        invalidate()
    }

    private fun resetImageMatrix() {
        bitmap?.let { b ->
            imageMatrix.reset()
            val viewWidth = width.toFloat()
            val viewHeight = height.toFloat()
            val bitmapWidth = b.width.toFloat()
            val bitmapHeight = b.height.toFloat()

            val scale: Float
            var dx = 0f
            var dy = 0f

            if (bitmapWidth * viewHeight > viewWidth * bitmapHeight) {
                scale = viewHeight / bitmapHeight
                dx = (viewWidth - bitmapWidth * scale) * 0.5f
            } else {
                scale = viewWidth / bitmapWidth
                dy = (viewHeight - bitmapHeight * scale) * 0.5f
            }
            
            imageMatrix.setScale(scale, scale)
            imageMatrix.postTranslate(dx, dy)
            scaleFactor = scale

            invalidate()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Define the crop window (a centered square)
        val size = min(w, h) * 0.9f
        val left = (w - size) / 2f
        val top = (h - size) / 2f
        val right = left + size
        val bottom = top + size
        cropRect.set(left, top, right, bottom)

        // Create a circular path for the overlay
        cropCirclePath.reset()
        cropCirclePath.addOval(cropRect, Path.Direction.CW)
        cropCirclePath.fillType = Path.FillType.INVERSE_WINDING

        resetImageMatrix()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bitmap?.let { canvas.drawBitmap(it, imageMatrix, null) }

        // Draw the semi-transparent overlay and the border
        canvas.drawPath(cropCirclePath, overlayPaint)
        canvas.drawOval(cropRect, borderPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                activePointerId = event.getPointerId(0)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress) {
                    val pointerIndex = event.findPointerIndex(activePointerId)
                    if (pointerIndex != -1) {
                        val x = event.getX(pointerIndex)
                        val y = event.getY(pointerIndex)
                        val dx = x - lastTouchX
                        val dy = y - lastTouchY
                        imageMatrix.postTranslate(dx, dy)
                        invalidate()
                        lastTouchX = x
                        lastTouchY = y
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePointerId = MotionEvent.INVALID_POINTER_ID
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = (event.action and MotionEvent.ACTION_POINTER_INDEX_MASK) shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
                if (event.getPointerId(pointerIndex) == activePointerId) {
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    lastTouchX = event.getX(newPointerIndex)
                    lastTouchY = event.getY(newPointerIndex)
                    activePointerId = event.getPointerId(newPointerIndex)
                }
            }
        }
        return true
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            var newScale = scaleFactor * detector.scaleFactor
            newScale = max(minScale, min(newScale, maxScale))
            val scale = newScale / scaleFactor
            scaleFactor = newScale

            imageMatrix.postScale(scale, scale, detector.focusX, detector.focusY)
            invalidate()
            return true
        }
    }

    fun getCroppedImage(): Bitmap {
        val invertedMatrix = Matrix()
        imageMatrix.invert(invertedMatrix)

        val mappedRect = RectF()
        invertedMatrix.mapRect(mappedRect, cropRect)

        val sourceBitmap = bitmap ?: throw IllegalStateException("Bitmap is not set.")

        val croppedBitmap = Bitmap.createBitmap(
            sourceBitmap,
            max(0, mappedRect.left.toInt()),
            max(0, mappedRect.top.toInt()),
            min(sourceBitmap.width - max(0, mappedRect.left.toInt()), mappedRect.width().toInt()),
            min(sourceBitmap.height - max(0, mappedRect.top.toInt()), mappedRect.height().toInt())
        )

        // Scale the cropped bitmap to a fixed size if needed, e.g., 512x512
        val output = Bitmap.createBitmap(cropRect.width().toInt(), cropRect.height().toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawBitmap(croppedBitmap, null, RectF(0f, 0f, output.width.toFloat(), output.height.toFloat()), null)

        return output
    }
}
