package com.example.testndk

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import com.example.testndk.databinding.ActivityMainBinding
import org.opencv.android.*
import org.opencv.core.Mat

const val TAG = "MY_TAG"

class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private lateinit var binding: ActivityMainBinding
    private lateinit var javaCameraView: JavaCameraView

    private val mLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    javaCameraView.enableView()
                    Log.d(TAG, "callback: opencv loaded successfully")
                }
                else -> Log.d(TAG, "callback: could not load opencv")
            }
        }
    }

//    class LoaderCallbackImpl(private val context: Context) : BaseLoaderCallback(context) {
//        override fun onManagerConnected(status: Int) {
//            when(status) {
//                LoaderCallbackInterface.SUCCESS -> javaCamreaView
//            }
//        }
//    }

//    private val mLoaderCallback = LoaderCallbackImpl(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Example of a call to a native method
//        binding.sampleText.text = stringFromJNI()

        javaCameraView = findViewById(R.id.java_camera_view)
        javaCameraView.visibility = View.VISIBLE
        javaCameraView.setCvCameraViewListener(this)
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
    }

    override fun onCameraViewStopped() {
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
        return inputFrame.gray()
    }
}