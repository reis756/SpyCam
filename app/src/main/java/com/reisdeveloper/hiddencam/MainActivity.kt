package com.reisdeveloper.hiddencam

import android.Manifest
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Size
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatButton
import androidx.camera.core.FlashMode
import com.reisdeveloper.hiddencam.lib.HiddenCam
import com.reisdeveloper.hiddencam.lib.HiddenVideo
import com.reisdeveloper.hiddencam.lib.OnImageCapturedListener
import com.reisdeveloper.hiddencam.lib.hasPermissions
import java.io.File

private const val PERMISSIONS_REQUEST_CODE = 7
private val PERMISSIONS_REQUIRED = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.READ_EXTERNAL_STORAGE,
    Manifest.permission.WRITE_EXTERNAL_STORAGE
)

class MainActivity : AppCompatActivity(), OnImageCapturedListener {

    private lateinit var baseStorageFolder: File
    private var hiddenCam: HiddenCam? = null
    private var hiddenVideo: HiddenVideo? = null

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        if (!hasPermissions()) {
            requestPermissions(PERMISSIONS_REQUIRED, PERMISSIONS_REQUEST_CODE)
        } else {
            setupHiddenCam()
        }

        findViewById<AppCompatButton>(R.id.btnPhoto).setOnClickListener {
            if (hiddenCam == null) {
                hiddenCam = HiddenCam(
                    this, baseStorageFolder, this,
                    targetResolution = Size(1080, 1920)
                )
                hiddenCam?.start()
            }
            hiddenCam?.captureImage()
        }

        findViewById<AppCompatButton>(R.id.btnVideo).setOnClickListener {
            if (hiddenVideo == null) {
                hiddenVideo = HiddenVideo(
                    this, baseStorageFolder, this,
                    targetResolution = Size(1080, 1920),
                    flashMode = FlashMode.ON
                )
                hiddenVideo?.start()
            }
            hiddenVideo?.captureVideo()
        }
    }

    private fun setupHiddenCam() {
        baseStorageFolder = File(getExternalFilesDir(null), Const.HIDDEN_PATH).apply {
            if (!exists())
                mkdir()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            toastMessage("Permission granted")
            setupHiddenCam()
        } else {
            toastMessage("Permission denied")
        }
    }

    private fun toastMessage(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    override fun onImageCaptured(image: File) {
        toastMessage("File saved on $image")
    }

    override fun onImageCaptureError(e: Throwable?) {
        toastMessage("Error: $e")
    }

    override fun onStop() {
        super.onStop()
        hiddenCam?.stop()
        hiddenVideo?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        hiddenCam?.destroy()
        hiddenVideo?.destroy()
    }

}