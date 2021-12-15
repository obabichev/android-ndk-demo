package com.example.testndk

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.testndk.databinding.ActivityMainBinding
import com.example.testndk.demo.NativeClass
import org.opencv.android.*
import org.opencv.core.Mat
import org.opencv.core.CvType
import org.opencv.core.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.view.WindowManager
import android.view.Surface
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.core.Core


const val TAG = "MY_TAG"
const val PERMISSION_REQUEST_CODE = 200

class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private lateinit var binding: ActivityMainBinding
    private var javaCameraView: JavaCameraView? = null

    private var deviceRotation = 0

    private val mLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    tensorflowModel =
                        loadModel(R.raw.selfie_segmentstion_no_custom_op, FACE_DIR, "selfie.tflite")
                    val background = loadModel(R.raw.image, FACE_DIR, FACE_MODEL)
                    backgroundDefault = Imgcodecs.imread(background!!.absolutePath)
                    NativeClass.loadTensorflowModel(tensorflowModel!!.absolutePath)
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
//            javaCameraView?.rotation = 90.0
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

    private fun updateRotation() {
        val display: Display = (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay
        val rotation: Int = display.getRotation()

        when (rotation) {
            Surface.ROTATION_0 -> deviceRotation = 0
            Surface.ROTATION_90 -> deviceRotation = 90
            Surface.ROTATION_180 -> deviceRotation = 180
            Surface.ROTATION_270 -> deviceRotation = 270
        }
        Log.d(TAG, "Current orientation: $deviceRotation")
    }

    override fun onResume() {
        Log.d(TAG, "in onResume()")
        super.onResume()
        updateRotation()

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

    private fun getPersonMask(img: Mat): Mat {
        val mask = Mat.zeros(img.size(), CvType.CV_32F);
        NativeClass.testTensorflowLite(img.nativeObjAddr, mask.nativeObjAddr);
        val binaryMask = Mat();
        Core.compare(mask, Scalar(0.1), binaryMask, Core.CMP_LE)
        Core.multiply(mask, Scalar(255.0), mask)
        return binaryMask
    }

    private fun processFrame(frame: Mat): Mat {
        var mask = getPersonMask(frame)
        if (backgroundDefault!!.size() != frame.size()) {
            Imgproc.resize(backgroundDefault, backgroundDefault, frame.size())
        }
        var background = backgroundDefault!!.clone()

        frame.setTo(Scalar(0.0), mask)
        Core.bitwise_not(mask, mask)
        background.setTo(Scalar(0.0), mask)
        Core.add(frame, background, frame)
        return frame
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
        var mRgba = inputFrame.rgba()

        if (deviceRotation == 0) {
            val mRgbaT: Mat = mRgba.t()
            Core.flip(mRgba.t(), mRgbaT, 0)
            Imgproc.resize(mRgbaT, mRgbaT, mRgba.size(), 0.0, 0.0)
            mRgba = mRgbaT
        }

        if (deviceRotation == 270) {
            Core.flip(mRgba, mRgba, 0)
        }

        return processFrame(mRgba)
    }

    fun loadModel(resourceId: Int, dir: String, model: String): File? {
        val modelInputStream =
            resources.openRawResource(resourceId)
        try {


            // create a temp directory
            val faceDir = getDir(dir, Context.MODE_PRIVATE)

            // create a model file
            val modelFile = File(faceDir, model)

            // copy model to new face library
            val modelOutputStream = FileOutputStream(modelFile)

            val buffer = ByteArray(byteSize)
            var byteRead = modelInputStream.read(buffer)
            while (byteRead != -1) {
                modelOutputStream.write(buffer, 0, byteRead)
                byteRead = modelInputStream.read(buffer)
            }

            modelInputStream.close()
            modelOutputStream.close()

            Log.d(TAG, "Face lib loaded successfully")
            return modelFile
        } catch (e: IOException) {
            Log.d(TAG, "Error loading cascade face model...$e")

        }
        return null
    }

    var tensorflowModel: File? = null
    var backgroundDefault: Mat? = null

    companion object {
        private const val FACE_DIR = "facelib"
        private const val FACE_MODEL = "haarcascade_frontalface_alt2.xml"

        private const val TENSORFLOW_DIR = "tensorflowlib"
        private const val TENSORFLOW_MODEL = "selfie_segmentation_no_custom_op.tflite"

        private const val byteSize = 4096 // buffer size

        init {
            System.loadLibrary("testndk")
        }
    }
}