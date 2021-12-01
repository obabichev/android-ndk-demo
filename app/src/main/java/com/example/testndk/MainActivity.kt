package com.example.testndk

import android.Manifest
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import com.example.testndk.databinding.ActivityMainBinding
import org.opencv.android.*
import org.opencv.core.Mat
import androidx.core.app.ActivityCompat

import android.content.pm.PackageManager

import androidx.core.content.ContextCompat


const val TAG = "MY_TAG"
const val PERMISSION_REQUEST_CODE = 200

class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private lateinit var binding: ActivityMainBinding
    private var javaCameraView: JavaCameraView? = null

    private val mLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    javaCameraView?.setCameraPermissionGranted()
                    javaCameraView?.enableView()
                    Log.d(TAG, "callback: opencv loaded successfully")
                }
                else -> {
                    super.onManagerConnected(status)
                    Log.d(TAG, "callback: could not load opencv")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Example of a call to a native method
//        binding.sampleText.text = stringFromJNI()

        if (permission()) {
            Log.d(TAG, "Permissions granted")
            javaCameraView = findViewById(R.id.java_camera_view)
            javaCameraView?.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT)
            javaCameraView?.visibility = View.VISIBLE
            javaCameraView?.setCvCameraViewListener(this)

        } else {
            Log.d(TAG, "Request permission")
            requestPermission()
        }
    }

    private fun permission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.CAMERA),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onResume() {
        Log.d(TAG, "in onResume()")
        super.onResume()

        if (!OpenCVLoader.initDebug()) {
            val success = OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback)

            if (success) {
                Log.d(TAG, "asynchronous init succeeded!")
            } else {
                Log.d(TAG, "asynchronous init failed!")
            }
        } else {
            Log.d(TAG, "OPENCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    /**
     * A native method that is implemented by the 'testndk' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'testndk' library on application startup.
        init {
            System.loadLibrary("testndk")
        }
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        Log.d(TAG, "onCameraViewStarted(width=$width, height=$height)")
    }

    override fun onCameraViewStopped() {
        Log.d(TAG, "onCameraViewStopped()")
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
//        Log.d(TAG, "onCameraFrame()")
        return inputFrame.gray()
    }
}