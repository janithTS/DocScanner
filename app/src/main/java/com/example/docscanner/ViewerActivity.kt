package com.example.docscanner

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.sdase.submission.documentscanner.DocumentDetector
import org.sdase.submission.documentscanner.extensions.saveToFile
import org.sdase.submission.documentscanner.extensions.toPoint
import org.sdase.submission.documentscanner.models.PointDouble
import org.sdase.submission.documentscanner.models.Quad
import org.sdase.submission.documentscanner.utils.FileUtil
import org.sdase.submission.documentscanner.utils.ImageUtil
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class ViewerActivity : AppCompatActivity() {
    private var normalizedImageView: ImageView? = null
    private lateinit var points: Array<PointDouble?>
    private var rawImage: Bitmap? = null
    private val normalized: Bitmap? = null
    private var ddn: DocumentDetector? = null
    private var rotation = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewer)
        val rotateButton = findViewById<Button>(R.id.rotateButton)
        val saveImageButton = findViewById<Button>(R.id.saveImageButton)
        rotateButton.setOnClickListener { v: View? ->
            rotation += 90
            if (rotation == 360) {
                rotation = 0
            }
            normalizedImageView!!.rotation = rotation.toFloat()
        }
        saveImageButton.setOnClickListener { v: View? ->
            if (hasStoragePermission()) {
                saveImage(normalized)
            } else {
                requestPermission()
            }
        }
        val filterRadioGroup = findViewById<RadioGroup>(R.id.filterRadioGroup)
        filterRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == R.id.binaryRadioButton) {
                updateSettings(R.raw.binary_template)
            } else if (checkedId == R.id.grayscaleRadioButton) {
                updateSettings(R.raw.gray_template)
            } else {
                updateSettings(R.raw.color_template)
            }
            //normalize();
        }
        normalizedImageView = findViewById(R.id.normalizedImageView)
        try {
            ddn = DocumentDetector()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        loadImageAndPoints()
        //normalize()
        cropDocumentAndFinishIntent()
    }

    private fun loadImageAndPoints() {
        val uri = Uri.parse(intent.getStringExtra(Extra.IMAGE_URI))
        rawImage = try {
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        } catch (e: IOException) {
            e.printStackTrace()
            return
        }
        val bitmapWidth = intent.getIntExtra(Extra.WIDTH, 720)
        val bitmapHeight = intent.getIntExtra(Extra.HEIGHT, 1280)
        val parcelables = intent.getParcelableArrayExtra(Extra.POINTS)
        points = arrayOfNulls(parcelables!!.size)
        for (i in parcelables.indices) {
            points[i] = parcelables[i] as PointDouble
            points[i]!!.x = points[i]!!.x * rawImage!!.width / bitmapWidth
            points[i]!!.y = points[i]!!.y * rawImage!!.height / bitmapHeight
        }
    }

    private fun normalize() {
        /*Quadrilateral quad = new Quadrilateral();
        quad.points = points;
        try {
            //NormalizedImageResult result = ddn.normalize(rawImage,quad);
            int bytes = rawImage.getByteCount();
            ByteBuffer buf = ByteBuffer.allocate(bytes);
            rawImage.copyPixelsToBuffer(buf);
            ImageData imageData = new ImageData();
            imageData.bytes = buf.array();
            imageData.width = rawImage.getWidth();
            imageData.height = rawImage.getHeight();
            imageData.stride = rawImage.getRowBytes();
            imageData.format = EnumImagePixelFormat.IPF_ABGR_8888;
            NormalizedImageResult result = ddn.normalize(imageData,quad);
            normalized = result.image.toBitmap();
            normalizedImageView.setImageBitmap(normalized);
        } catch (DocumentNormalizerException | CoreException e) {
            e.printStackTrace();
        }*/
    }

    private fun cropDocumentAndFinishIntent() {
        val uri = Uri.parse(intent.getStringExtra(Extra.IMAGE_URI))
        val croppedImageResults = arrayListOf<String>()
        // crop document photo by using corners
        val croppedImage: Bitmap =
            try {
                ImageUtil().crop(
                    uri.path!!,
                    Quad(
                        points[0]!!.toPoint(),
                        points[1]!!.toPoint(),
                        points[2]!!.toPoint(),
                        points[3]!!.toPoint()
                    )
                )
            } catch (exception: Exception) {
                //finishIntentWithError("unable to crop image: ${exception.message}")
                return
            }

        // delete original document photo
        File(uri.path).delete()

        // save cropped document photo
        try {
            val croppedImageFile = FileUtil().createImageFile(this, 0)
            croppedImage.saveToFile(croppedImageFile)
            croppedImageResults.add(croppedImageFile.absolutePath)
            normalizedImageView?.setImageBitmap(croppedImage)
        } catch (exception: Exception) {
            //finishIntentWithError("unable to save cropped image: ${exception.message}")
        }

        // return array of cropped document photo file paths
        //setResult(Activity.RESULT_OK, Intent().putExtra("croppedImageResults", croppedImageResults))
        //finish()
    }

    private fun updateSettings(id: Int) {
        /*try {
            ddn.initRuntimeSettingsFromString(readTemplate(id));
        } catch (DocumentNormalizerException e) {
            e.printStackTrace();
        }*/
    }

    private fun readTemplate(id: Int): String {
        val resources = this.resources
        val `is` = resources.openRawResource(id)
        val buffer: ByteArray
        try {
            buffer = ByteArray(`is`.available())
            `is`.read(buffer)
            return buffer.toString()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return ""
    }

    private fun convertBitmapToImageData() {
        /* ImageData data = new ImageData();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        rawImage.compress(Bitmap.CompressFormat.JPEG,80,stream);
        byte[] byteArray = stream.toByteArray();
        data.format = EnumImagePixelFormat.IPF_RGB_888;
        data.orientation = 0;
        data.width = rawImage.getWidth();
        data.height = rawImage.getHeight();
        data.bytes = byteArray;
        data.stride = 4 * ((rawImage.getWidth() * 3 + 31)/32);*/
    }

    fun saveImage(bmp: Bitmap?) {
        val appDir = File(Environment.getExternalStorageDirectory(), "ddn")
        if (!appDir.exists()) {
            appDir.mkdir()
        }
        val fileName = System.currentTimeMillis().toString() + ".jpg"
        val file = File(appDir, fileName)
        try {
            val fos = FileOutputStream(file)
            if (rotation != 0) {
                val matrix = Matrix()
                matrix.setRotate(rotation.toFloat())
                val rotated = Bitmap.createBitmap(bmp!!, 0, 0, bmp.width, bmp.height, matrix, false)
                rotated.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            } else {
                bmp!!.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            }
            fos.flush()
            fos.close()
            Toast.makeText(
                this@ViewerActivity,
                "File saved to " + file.absolutePath,
                Toast.LENGTH_LONG
            ).show()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun hasStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            WRITE_EXTERNAL_STORAGE_PERMISSION,
            WRITE_EXTERNAL_STORAGE_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            WRITE_EXTERNAL_STORAGE_REQUEST_CODE -> if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                saveImage(normalized)
            } else {
                Toast.makeText(
                    this,
                    "Please grant the permission to write external storage.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    companion object {
        private val WRITE_EXTERNAL_STORAGE_PERMISSION =
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        private const val WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 10
    }
}