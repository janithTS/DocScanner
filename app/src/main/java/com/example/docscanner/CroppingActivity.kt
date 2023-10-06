package com.example.docscanner

import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toPoint
import org.sdase.submission.documentscanner.extensions.toPoint
import org.sdase.submission.documentscanner.models.PointDouble

class CroppingActivity : AppCompatActivity() {
    private var okayButton: Button? = null
    private var reTakeButton: Button? = null
    private var background: Bitmap? = null
    private var imageView: ImageView? = null
    private var overlayView: OverlayView? = null
    private var corner1: ImageView? = null
    private var corner2: ImageView? = null
    private var corner3: ImageView? = null
    private var corner4: ImageView? = null
    private val corners = arrayOfNulls<ImageView>(4)
    private var mLastX = 0
    private var mLastY = 0
    private lateinit var points: Array<PointDouble?>
    private var screenWidth = 0
    private var screenHeight = 0
    private var bitmapWidth = 0
    private var bitmapHeight = 0
    private val cornerWidth = dp2px(15f).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cropping)

        imageView = findViewById(R.id.imageView)
        imageView?.scaleType = ImageView.ScaleType.FIT_XY
        overlayView = findViewById(R.id.cropOverlayView)
        reTakeButton = findViewById(R.id.reTakeButton)
        reTakeButton?.setOnClickListener { v: View? -> onBackPressed() }
        okayButton = findViewById(R.id.okayButton)
        okayButton?.setOnClickListener { v: View? ->
            val intent = Intent(this, ViewerActivity::class.java)
            intent.putExtra(Extra.IMAGE_URI, getIntent().getStringExtra(Extra.IMAGE_URI))
            intent.putExtra(Extra.POINTS, points)
            intent.putExtra(Extra.WIDTH, bitmapWidth)
            intent.putExtra(Extra.HEIGHT, bitmapHeight)
            startActivity(intent)
        }
        corner1 = findViewById(R.id.corner1)
        corner2 = findViewById(R.id.corner2)
        corner3 = findViewById(R.id.corner3)
        corner4 = findViewById(R.id.corner4)
        corners[0] = corner1
        corners[1] = corner2
        corners[2] = corner3
        corners[3] = corner4
        imageView?.viewTreeObserver?.addOnGlobalLayoutListener {
            //updateOverlayViewLayout()
        }
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        bitmapWidth = intent.getIntExtra(Extra.WIDTH, 720)
        bitmapHeight = intent.getIntExtra(Extra.HEIGHT, 1280)
        loadPoints()
        loadImage()
        setEvents()
    }

    private fun loadPoints() {
        val parcelables = intent.getParcelableArrayExtra(Extra.POINTS)
        points = arrayOfNulls(parcelables!!.size)
        for (i in parcelables.indices) {
            points[i] = parcelables[i] as PointDouble
        }
    }

    private fun loadImage() {
        try {
            val uri = Uri.parse(intent.getStringExtra(Extra.IMAGE_URI))
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            imageView!!.setImageBitmap(bitmap)
            background = bitmap
            drawOverlay()
            updateCornersPosition()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateCornersPosition() {
        for (i in 0..3) {
            val offsetX = getOffsetX(i)
            val offsetY = getOffsetY(i)
            corners[i]!!.x = points[i]!!.x.toFloat() * screenWidth / bitmapWidth + offsetX
            corners[i]!!.y = points[i]!!.y.toFloat() * screenHeight / bitmapHeight + offsetY
        }
    }

    private fun getOffsetX(index: Int): Int {
        return if (index == 0) {
            -cornerWidth
        } else if (index == 1) {
            0
        } else if (index == 2) {
            0
        } else {
            -cornerWidth
        }
    }

    private fun getOffsetY(index: Int): Int {
        return if (index == 0) {
            -cornerWidth
        } else if (index == 1) {
            -cornerWidth
        } else if (index == 2) {
            0
        } else {
            0
        }
    }

    private fun setEvents() {
        for (i in 0..3) {
            corners[i]?.setOnTouchListener { view: View, event: MotionEvent ->
                Log.d("DDN", event.toString())
                val x = event.x.toInt()
                val y = event.y.toInt()
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        mLastX = x
                        mLastY = y
                    }

                    MotionEvent.ACTION_MOVE -> {
                        view.x = view.x + x
                        view.y = view.y + y
                        updatePointsAndRedraw()
                    }

                    else -> {}
                }
                true
            }
        }
    }

    private fun updatePointsAndRedraw() {
        for (i in 0..3) {
            val offsetX = getOffsetX(i)
            val offsetY = getOffsetY(i)
            points[i]!!.x =
                ((corners[i]!!.x - offsetX) / screenWidth * bitmapWidth).toDouble()
            points[i]!!.y =
                ((corners[i]!!.y - offsetY) / screenHeight * bitmapHeight).toDouble()
        }
        drawOverlay()
    }

    private fun drawOverlay() {
        val points = points.map { it!!.toPoint() }
        overlayView?.setPointsAndImageGeometry(points, bitmapWidth, bitmapHeight);
    }

    private fun updateOverlayViewLayout() {
        val bm = background
        val ratioView = imageView!!.width.toDouble() / imageView!!.height
        val ratioImage = bm!!.width.toDouble() / bm.height
        val offsetX = (ratioImage * bm.width - bm.height) / 2
        overlayView!!.x = offsetX.toFloat()
    }

    fun dp2px(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            Resources.getSystem().displayMetrics
        )
    }
}