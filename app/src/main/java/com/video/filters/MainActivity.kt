package com.video.filters

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.daasuu.gpuv.camerarecorder.CameraRecordListener
import com.daasuu.gpuv.camerarecorder.GPUCameraRecorder
import com.daasuu.gpuv.camerarecorder.GPUCameraRecorderBuilder
import com.daasuu.gpuv.camerarecorder.LensFacing
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*

private const val CAMERA_PERMISSION_REQUEST_CODE = 111

class MainActivity : AppCompatActivity() {
    private var sampleGLView: GLSurfaceView? = null
    private var gpuCameraRecorder: GPUCameraRecorder? = null
    private var path = ""
    private var blueYellow = 0.toDouble()
    private var greenRed = 0.toDouble()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        checkPermission()
    }

    override fun onStop() {
        super.onStop()
        releaseCamera()
    }

    /**
     * set up record button logic
     */
    private fun buttonListeners() {
        record.setOnClickListener {
            if (record.text.toString() == getString(R.string.record)) {
                rgb.isVisible = false
                record.setText(R.string.stop)
                gpuCameraRecorder?.start(getVideoFilePath())
            } else {
                rgb.isVisible = true
                record.setText(R.string.record)
                gpuCameraRecorder?.stop()
            }
        }
    }

    /**
     * set up Blue to Yellow and Green to Red SeekBars change logic and accept new filter to the camera
     */
    private fun seekListeners() {
        by.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val filter = progress - 50.toDouble()
                blueYellow = 128 * filter / 50
                gpuCameraRecorder?.setFilter(getRGBFromLab(greenRed, blueYellow))
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        rg.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val filter = progress - 50.toDouble()
                greenRed = 128 * filter / 50
                gpuCameraRecorder?.setFilter(getRGBFromLab(greenRed, blueYellow))
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    /**
     * start showing record button and RGB SeekBars
     * Init GPUCameraRecorder
     */
    private fun setUpCamera() {
        rgb.isVisible = true
        record.isVisible = true
        seekListeners()
        buttonListeners()
        setUpCameraView()
        gpuCameraRecorder = GPUCameraRecorderBuilder(this, sampleGLView)
            .cameraRecordListener(object : CameraRecordListener {
                override fun onGetFlashSupport(flashSupport: Boolean) {}

                override fun onRecordComplete() {
                    exportMp4ToGallery(this@MainActivity, path)
                }

                override fun onRecordStart() {}

                override fun onError(exception: Exception) {}

                override fun onCameraThreadFinish() {}

                override fun onVideoFileReady() {}
            })
            .lensFacing(LensFacing.BACK)
            .build()
    }

    /**
     * setUp GLSurfaceView
     */
    private fun setUpCameraView() {
        cameraView.removeAllViews()
        sampleGLView = null
        sampleGLView = GLSurfaceView(applicationContext)
        cameraView.addView(sampleGLView)
    }

    /**
     * camera destroy logic
     */
    private fun releaseCamera() {
        if (sampleGLView != null) {
            sampleGLView?.onPause()
        }
        if (sampleGLView != null) {
            cameraView.removeView(sampleGLView)
            sampleGLView = null
        }
    }

    /**
     * checking camera, storage and audio permissions
     */
    private fun checkPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            setUpCameraView()
            return
        }
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            requestPermissions(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            setUpCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setUpCamera()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "[WARN] permission is not grunted.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * scan video file for showing in the gallery
     */
    private fun exportMp4ToGallery(context: Context, filePath: String) {
        val values = ContentValues(2)
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        values.put(MediaStore.Video.Media.DATA, filePath)
        context.contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            values
        )
        context.sendBroadcast(
            Intent(
                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.parse("file://$filePath")
            )
        )
    }

    /**
     * return new videoFile name based on datetime
     */
    private fun getVideoFilePath(): String {
        path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).absolutePath + "/" + SimpleDateFormat(
            "yyyyMM_dd-HHmmss",
            Locale.getDefault()
        ).format(Date()) + ".mp4"
        return path
    }
}