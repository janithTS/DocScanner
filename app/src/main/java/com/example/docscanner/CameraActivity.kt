package com.example.docscanner

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.core.ImageCapture.OutputFileResults
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.lifecycle.LifecycleOwner
import com.example.docscanner.Bitmap.rotationDegrees
import com.google.common.util.concurrent.ListenableFuture
import org.opencv.android.OpenCVLoader
import org.opencv.core.Point
import org.sdase.submission.documentscanner.DocumentDetector
import org.sdase.submission.documentscanner.extensions.move
import org.sdase.submission.documentscanner.extensions.saveToFile
import org.sdase.submission.documentscanner.extensions.toPointD
import org.sdase.submission.documentscanner.models.Quad
import java.io.File
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraActivity : AppCompatActivity() {

    private var bitmap: Bitmap? = null
    private var previewView: PreviewView? = null
    private var overlayView: OverlayView? = null
    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    private var exec: ExecutorService? = null
    private var camera: Camera? = null
    private var ddn: DocumentDetector? = null
    private var imageCapture: ImageCapture? = null
    private var taken = false
    private val previousResults = arrayListOf<Point>()
    lateinit var results: List<Point>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        /*supportActionBar!!.hide()
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )*/

        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)
        exec = Executors.newSingleThreadExecutor()
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture?.addListener({
            try {
                val cameraProvider = cameraProviderFuture?.get()
                bindUseCases(cameraProvider!!)
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
        initDDN()

        val capture: ImageButton = findViewById(R.id.btnCamera)
        capture.setOnClickListener {
            if (!taken) {
                //if (previousResults.size == 3) {
                //if (steady() == true) {
                Log.d("DDN", "take photo")
                bitmap?.let { it1 -> takePhoto(results, it1.width, it1.height) }
                taken = true
                //} else {
                //previousResults.remove(0)
                //previousResults.add(result)
                //}
                //} else {
                //previousResults.add(result)
                //}
            }
        }
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun bindUseCases(cameraProvider: ProcessCameraProvider) {
        val orientation = applicationContext.resources.configuration.orientation
        val resolution: Size
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            resolution = Size(720, 1280)
        } else {
            resolution = Size(1280, 720)
        }
        val previewBuilder = Preview.Builder()
        previewBuilder.setTargetAspectRatio(AspectRatio.RATIO_16_9)
        val preview = previewBuilder.build()
        val imageAnalysisBuilder = ImageAnalysis.Builder()
        imageAnalysisBuilder.setTargetResolution(resolution)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        val imageAnalysis = imageAnalysisBuilder.build()
        imageAnalysis.setAnalyzer(exec!!) { image ->
            rotationDegrees =
                image.imageInfo.rotationDegrees
            @SuppressLint("UnsafeOptInUsageError")
            bitmap = BitmapUtils.getBitmap(image)
            overlayView!!.srcImageWidth = bitmap!!.width
            overlayView!!.srcImageHeight = bitmap!!.height
            try {
                try {
                    val (topLeft, topRight, bottomLeft, bottomRight) = getDocumentCorners(bitmap!!)
                    Quad(topLeft, topRight, bottomRight, bottomLeft)
                    results = listOf(topLeft, topRight, bottomRight, bottomLeft)
                    //results = getDocumentCorners(bitmap)
                    //Quad(topLeft, topRight, bottomRight, bottomLeft)
                } catch (exception: Exception) {
                    finishIntentWithError("unable to get document corners: ${exception.message}")
                    return@setAnalyzer
                }

                if (results.isNotEmpty()) {
                    overlayView!!.setPointsDraw(results)

                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            image.close()
        }
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        preview.setSurfaceProvider(previewView!!.surfaceProvider)
        imageCapture = ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setTargetRotation(rotationDegrees)
            .build()
        val useCaseGroup = UseCaseGroup.Builder()
            .addUseCase(preview)
            .addUseCase(imageAnalysis)
            .addUseCase(imageCapture!!)
            .build()
        camera =
            cameraProvider.bindToLifecycle((this as LifecycleOwner), cameraSelector, useCaseGroup)
    }

    private val cropperOffsetWhenCornersNotFound = 100.0

    private fun getDocumentCorners(photo: Bitmap): List<Point> {
        val cornerPoints: List<Point>? = DocumentDetector().findDocumentCorners(photo)

        // if cornerPoints is null then default the corners to the photo bounds with a margin
        return cornerPoints
            ?: listOf(
                Point(0.0, 0.0).move(
                    cropperOffsetWhenCornersNotFound,
                    cropperOffsetWhenCornersNotFound
                ),
                Point(photo.width.toDouble(), 0.0)
                    .move(-cropperOffsetWhenCornersNotFound, cropperOffsetWhenCornersNotFound),
                Point(0.0, photo.height.toDouble())
                    .move(cropperOffsetWhenCornersNotFound, -cropperOffsetWhenCornersNotFound),
                Point(photo.width.toDouble(), photo.height.toDouble())
                    .move(-cropperOffsetWhenCornersNotFound, -cropperOffsetWhenCornersNotFound)
            )
    }

    override fun onResume() {
        super.onResume()
        try {
            // load OpenCV
            OpenCVLoader.initDebug()
        } catch (exception: Exception) {
            finishIntentWithError("error starting OpenCV: ${exception.message}")
        }
    }

    /*private fun steady(): Boolean? {
        val iou1: Float = Utils.intersectionOverUnion(
            previousResults.get(0).location.points,
            previousResults.get(1).location.points
        )
        val iou2: Float = Utils.intersectionOverUnion(
            previousResults.get(1).location.points,
            previousResults.get(2).location.points
        )
        val iou3: Float = Utils.intersectionOverUnion(
            previousResults.get(0).location.points,
            previousResults.get(2).location.points
        )
        Log.d("DDN", "iou1: $iou1")
        Log.d("DDN", "iou2: $iou2")
        Log.d("DDN", "iou3: $iou3")
        return if (iou1 > 0.9 && iou2 > 0.9 && iou3 > 0.9) {
            true
        } else {
            false
        }
    }*/

    private fun takePhoto(
        result: List<Point>,
        bitmapWidth: Int,
        bitmapHeight: Int
    ) {
        val pointFs = result.map { it.toPointD() }
        val dir = externalCacheDir
        val file = File(dir, "photo.jpg")
        val outputFileOptions = OutputFileOptions.Builder(file).build()
        imageCapture!!.takePicture(outputFileOptions, exec!!,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: OutputFileResults) {
                    Log.d("DDN", "saved")
                    Log.d("DDN", outputFileResults.savedUri.toString())
                    val uri = outputFileResults.savedUri
                    //val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    val bitmap =
                        com.example.docscanner.Bitmap.handleSamplingAndRotationBitmap(
                            this@CameraActivity,
                            uri
                        )
                    val matrix = Matrix().apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            postRotate(rotationDegrees.toFloat())
                        }
                    }
                    val bitmap1 = bitmap?.let {
                        com.example.docscanner.Bitmap.getResizedBitmap(
                            it,
                            matrix
                        )
                    }
                    val file = uri?.toFile()
                    file?.let { bitmap1?.saveToFile(it) }

                    val intent = Intent(
                        this@CameraActivity,
                        CroppingActivity::class.java
                    )
                    intent.putExtra("imageUri", outputFileResults.savedUri.toString())
                    intent.putExtra("points", pointFs.toTypedArray())
                    intent.putExtra("bitmapWidth", bitmapWidth)
                    intent.putExtra("bitmapHeight", bitmapHeight)
                    startActivity(intent)
                }

                override fun onError(exception: ImageCaptureException) {}
            }
        )
    }

    private fun initDDN() {
        ddn = DocumentDetector()
    }

    /**
     * This ends the document scanner activity, and returns an error message that can be used to debug
     * error
     *
     * @param errorMessage an error message
     */
    private fun finishIntentWithError(errorMessage: String) {
        setResult(Activity.RESULT_OK, Intent().putExtra("error", errorMessage))
        finish()
    }
}