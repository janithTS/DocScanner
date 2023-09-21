package com.example.docscanner

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class MainActivity : AppCompatActivity() {

    private lateinit var croppedImageView: ImageView
    private var startScanButton: Button? = null
    private val CAMERA_PERMISSION = arrayOf<String>(android.Manifest.permission.CAMERA)
    private val CAMERA_REQUEST_CODE = 10
   /* private val documentScanner = DocumentScanner(
        this,
        { croppedImageResults ->
            // display the first cropped image
            croppedImageView.setImageBitmap(
                BitmapFactory.decodeFile(croppedImageResults.first())
            )
        },
        {
            // an error happened
                errorMessage ->
            Log.v("documentscannerlogs", errorMessage)
        },
        {
            // user canceled document scan
            Log.v("documentscannerlogs", "User canceled document scan")
        }
    )*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //croppedImageView = findViewById(R.id.cropped_image_view)

        // start document scan
        //documentScanner.startScan()
        startScanButton = findViewById<Button>(R.id.startScanButton)
        startScanButton?.setOnClickListener { v ->
            if (hasCameraPermission()) {
                startScan()
            } else {
                requestPermission()
            }
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            CAMERA_PERMISSION,
            CAMERA_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_REQUEST_CODE -> if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                startScan()
            } else {
                Toast.makeText(this, "Please grant camera permission", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startScan() {
        val intent = Intent(this, CameraActivity::class.java)
        startActivity(intent)
    }
}