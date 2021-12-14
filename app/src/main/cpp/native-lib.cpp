#include <jni.h>
#include <string>
#include <tensorflow/lite/c/c_api.h>
#include <tensorflow/lite/c/common.h>
#include "opencv2/objdetect.hpp"
#include "opencv2/highgui.hpp"
#include "opencv2/imgproc.hpp"
#include "opencv2/videoio.hpp"
#include "opencv2/core.hpp"
#include "iostream"

cv::CascadeClassifier faceDetector;


// Ratio between true size and grayscale size
double getRatio(cv::Size src, int newSize) {
    short int w = static_cast<short>(src.width);
    short int h = static_cast<short>(src.height);
    short int heightMin = 320;

    if (newSize < heightMin) {
//        lgd("Input size is too small! Set to 320 px.");
    } else {
        heightMin = static_cast<short>(newSize);
    }

    float ratio;
    if (w > h) {
        if (w < heightMin) return 0.0;
        ratio = (float) heightMin / w;
    } else {
        if (h < heightMin) return 0.0;
        ratio = (float) heightMin / h;
    }

    return ratio;
}

// Rotate the grayscale image by rotation
void rotateGray(cv::Mat &src, int rotation) {
    switch (rotation) {
        case 0:
            rotate(src, src, cv::ROTATE_90_COUNTERCLOCKWISE);
            break;
        case 90:
            break;
        case 180:
            rotate(src, src, cv::ROTATE_90_COUNTERCLOCKWISE);
            break;
        case 270:
            flip(src, src, 0);
            break;
        default:
//            string msg = "Error: wrong rotation data -- " +
//                         to_string(rotation);
//            lge(msg.c_str());
            break;
    }
}

void drawFaceRectangle(
        cv::Mat &rgba,
        cv::Mat &gray,
        cv::String path,
        double ratio,
        int rotation) {

    // width and height of frame
    float scrW = (float) rgba.size().width;
    float scrH = (float) rgba.size().height;

    std::vector<cv::Rect> faces;
    double scale = 1.0 + ratio;
    int neighbor = 3;

// fix orientation so it can be detected
    rotateGray(gray, rotation);

    faceDetector.detectMultiScale(
            gray, // image
            faces, // array
            scale, // scale
            neighbor, // min neighbors
            0, // flags,
            cv::Size(30, 30), // min size: 30x30
            cv::Size((int) scrW, (int) scrH) // max size: screen size
    );

    cv::Scalar color(0, 105, 205);
    int frame_thickness = 4;


    for (int i = 0; i < faces.size(); i++) {
        cv::Rect rect = faces[i];

        float x = (float) rect.x;
        float y = (float) rect.y;
        float rw = (float) rect.width;
        float rh = (float) rect.height;

        switch (rotation) {
            case 0:
                cv::rectangle(rgba, cv::Rect(scrW - y - rh, x, rh, rw), color, frame_thickness);
                break;

            case 90:
                cv::rectangle(rgba, cv::Rect(x, y, rw, rh), color,
                              frame_thickness);
                break;

            case 180: // TODO was not tested due to smartphone limitations
                cv::rectangle(rgba, cv::Rect(x, y, rw, rh), color,
                              frame_thickness); // TODO fix according to orientation
                break;

            case 270:
                cv::rectangle(rgba, cv::Rect(x, scrH - y - rh, rw, rh), color, frame_thickness);
                break;

            default:
//                string msg = "Error: wrong rotation data -- " +
//                             to_string(rotation);
//                lge(msg.c_str());
                break;
        }
    }
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_testndk_demo_NativeClass_00024Companion_faceDetection(JNIEnv *env, jobject thiz,
                                                                       jlong mat_addr_rgba,
                                                                       jint height,
                                                                       jint rotation,
                                                                       jstring model_path) {
    return (jboolean) true;
    cv::Mat &mRgb = *(cv::Mat *) mat_addr_rgba;
    cv::Mat mGr = cv::Mat();
    int mHeight = (int) height;
    int mRotation = (int) rotation;

    cv::cvtColor(mRgb, mGr, cv::COLOR_RGB2GRAY);

    cv::Size src = cv::Size(mGr.size().width, mGr.size().height);
    double ratio = getRatio(src, mHeight);
    double scale = 1.0 + ratio;

    const char *value = env->GetStringUTFChars(model_path, 0);
    cv::String modelPath = (cv::String) value;

    faceDetector = cv::CascadeClassifier();
    if (!faceDetector.load(modelPath)) {
        cv::String error = "Error loading the model: " + modelPath;
//        lge(error.c_str());
        return (jboolean) false;
    }

    // draw faces
    drawFaceRectangle(
            mRgb,
            mGr,
            modelPath,
            ratio,
            mRotation);

    return (jboolean) true;
}

TfLiteInterpreter *interpreter;

void reporter(void* user_data, const char* format, va_list args)
{
    std::cout << "Error" << std::endl;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_testndk_demo_NativeClass_00024Companion_loadTensorflowModel(JNIEnv *env,
                                                                             jobject thiz,
                                                                             jstring model_path) {
    const char *MODEL_PATH = env->GetStringUTFChars(model_path, 0);
    TfLiteModel *model = TfLiteModelCreateFromFile(MODEL_PATH);
    TfLiteInterpreterOptions *options = TfLiteInterpreterOptionsCreate();
    TfLiteInterpreterOptionsSetNumThreads(options, 2);
    TfLiteInterpreterOptionsSetErrorReporter(options, reporter, NULL);
    // Create the interpreter.
    interpreter = TfLiteInterpreterCreate(model, options);
    auto allocateStatus = TfLiteInterpreterAllocateTensors(interpreter);
    assert(allocateStatus == TfLiteStatus::kTfLiteOk);
}


extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_testndk_demo_NativeClass_00024Companion_testTensorflowLite(JNIEnv *env,
                                                                            jobject thiz,
                                                                            jlong mat_addr_rgba,
                                                                            jlong dest_addr) {
    cv::Mat &mRgb = *(cv::Mat *) mat_addr_rgba;
    cv::cvtColor(mRgb, mRgb, cv::COLOR_RGBA2RGB);
    cv::Mat image;
    cv::resize(mRgb, image, cv::Size(256, 256));
    mRgb.type(); mRgb.channels();

    cv::Mat inputMat;
    image.convertTo(inputMat, CV_32FC3);

    inputMat /= 255;
    // Allocate tensors and populate the input tensor data.
    TfLiteTensor* input_tensor =
            TfLiteInterpreterGetInputTensor(interpreter, 0);
    auto copyFromStatus = TfLiteTensorCopyFromBuffer(input_tensor, inputMat.data, inputMat.u->size);

    auto invokeStatus = TfLiteInterpreterInvoke(interpreter);

    const TfLiteTensor* output_tensor =
          TfLiteInterpreterGetOutputTensor(interpreter, 0);

    cv::Mat outputMat(cv::Size(256, 256), CV_32F);
    auto status = TfLiteTensorCopyToBuffer(output_tensor, (void *)outputMat.data,
                             outputMat.u->size);
    std::cout << status;



    cv::Mat &outMat = *(cv::Mat *) dest_addr;
    cv::resize(outputMat, outputMat, outMat.size());
    outputMat.copyTo(outMat);
    std::cout << "";
    return true;
}