package com.example.docscanner

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import org.opencv.core.Point

class OverlayView(context: Context?, attrs: AttributeSet?) : SurfaceView(context, attrs),
    SurfaceHolder.Callback {

    var srcImageWidth: Int
    var srcImageHeight: Int
    var surfaceHolder: SurfaceHolder? = null
    var points: List<Point>? = null
    var stroke = Paint()

    init {
        isFocusable = true
        Log.d("DDN", "initialize overlay view")
        srcImageWidth = 0
        srcImageHeight = 0
        if (surfaceHolder == null) {
            // Get surfaceHolder object.
            surfaceHolder = holder
            // Add this as surfaceHolder callback object.
            surfaceHolder?.addCallback(this)
        }
        stroke.color = Color.GREEN
        // Set the parent view background color. This can not set surfaceview background color.
        setBackgroundColor(Color.TRANSPARENT)

        // Set current surfaceview at top of the view tree.
        setZOrderOnTop(true)
        this.holder.setFormat(PixelFormat.TRANSLUCENT)
    }

    override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
        Log.d("DDN", "surface created")
        if (points != null) {
            drawPolygon()
        }
    }

    override fun surfaceChanged(surfaceHolder: SurfaceHolder, i: Int, i1: Int, i2: Int) {}

    override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {}

    fun setPointsDraw(points: List<Point>) {
        this.points = points
        drawPolygon()
    }

    fun setPointsAndImageGeometry(points: List<Point>?, width: Int, height: Int) {
        srcImageWidth = width
        srcImageHeight = height
        this.points = points
        drawPolygon()
    }

    fun drawPolygon() {
        Log.d("DDN", "draw polygon")
        // Get and lock canvas object from surfaceHolder.
        val canvas = surfaceHolder!!.lockCanvas()
        if (canvas == null) {
            Log.d("DDN", "canvas is null")
            return
        }
        Log.d("DDN", "srcImageHeight: $srcImageHeight")
        val pts: List<Point>? = if (srcImageWidth != 0 && srcImageHeight != 0) {
            Log.d("DDN", "convert points")
            convertPoints(canvas.width, canvas.height)
        } else {
            points
        }
        // Clear canvas
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        for (index in 0..pts!!.size - 1) {
            if (index == pts.size - 1) {
                canvas.drawLine(
                    pts[index].x.toFloat(),
                    pts[index].y.toFloat(),
                    pts[0].x.toFloat(),
                    pts[0].y.toFloat(),
                    stroke
                )
            } else {
                canvas.drawLine(
                    pts[index].x.toFloat(),
                    pts[index].y.toFloat(),
                    pts[index + 1].x.toFloat(),
                    pts[index + 1].y.toFloat(),
                    stroke
                )
            }
        }

        // Unlock the canvas object and post the new draw.
        surfaceHolder!!.unlockCanvasAndPost(canvas)
    }

    fun convertPoints(canvasWidth: Int, canvasHeight: Int): List<Point> {
        val newPoints = ArrayList<Point>()
        val ratioX = canvasWidth.toDouble() / srcImageWidth
        val ratioY = canvasHeight.toDouble() / srcImageHeight
        for (index in 0..points!!.size.minus(1)) {
            val p = Point()
            p.x = ratioX * points!![index].x
            p.y = ratioY * points!![index].y
            newPoints.add(p)
        }
        return newPoints
    }
}