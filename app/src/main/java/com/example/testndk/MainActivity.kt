package com.example.testndk

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.testndk.databinding.ActivityMainBinding
import com.example.testndk.demo.NativeClass
import org.opencv.android.*
import org.opencv.core.Mat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


const val TAG = "MY_TAG"
const val PERMISSION_REQUEST_CODE = 200

class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private lateinit var binding: ActivityMainBinding
    private var javaCameraView: JavaCameraView? = null

    private val mLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    loadFaceLib()
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

    override fun onCameraViewStarted(width: Int, height: Int) {
        Log.d(TAG, "onCameraViewStarted(width=$width, height=$height)")
    }

    override fun onCameraViewStopped() {
        Log.d(TAG, "onCameraViewStopped()")
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
        val mRgba = inputFrame.rgba()
        val screenRotation = this.resources.configuration.orientation * 90 + 90

        NativeClass.faceDetection(
            mRgba.nativeObjAddr,
            480,
            screenRotation,
            faceModel!!.absolutePath
        )
        return mRgba
    }

    fun loadFaceLib() {
        val modelInputStream =
            resources.openRawResource(R.raw.haarcascade_frontalface_alt2)
        try {


            // create a temp directory
            val faceDir = getDir(FACE_DIR, Context.MODE_PRIVATE)

            // create a model file
            faceModel = File(faceDir, FACE_MODEL)

            // copy model to new face library
            val modelOutputStream = FileOutputStream(faceModel)

            val buffer = ByteArray(byteSize)
            var byteRead = modelInputStream.read(buffer)
            while (byteRead != -1) {
                modelOutputStream.write(buffer, 0, byteRead)
                byteRead = modelInputStream.read(buffer)
            }

            modelInputStream.close()
            modelOutputStream.close()

//            faceDetector = CascadeClassifier(faceModel.absolutePath)
            Log.d(TAG, "Face lib loaded successfully")
        } catch (e: IOException) {
            Log.d(TAG, "Error loading cascade face model...$e")

        }
    }

    var faceModel: File? = null

    //    lateinit var faceDir: File
    var imageRatio = 0.0 // scale down ratio

    /**
     * A native method that is implemented by the 'testndk' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        private const val FACE_DIR = "facelib"
        private const val FACE_MODEL = "haarcascade_frontalface_alt2.xml"
        private const val byteSize = 4096 // buffer size

        init {
            System.loadLibrary("testndk")
        }
    }
}