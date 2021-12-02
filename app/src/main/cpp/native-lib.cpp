#include <jni.h>
#include <string>
#include "opencv2/objdetect.hpp"
#include "opencv2/highgui.hpp"
#include "opencv2/imgproc.hpp"
#include "opencv2/videoio.hpp"
#include "opencv2/core.hpp"

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_testndk_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}


//cv::CascadeClassifier faceDetector;

//extern "C"
//JNIEXPORT void JNICALL
//Java_com_example_testndk_demo_NativeClass_00024Companion_testFunction(JNIEnv *env, jobject thiz,
//                                                                      jlong addr_rgba) {
//    cv::Mat &img = *(cv::Mat *) addr_rgba;
//
////    auto rectangles = face_detector.detect_face_rectangles(img);
////    cv::Scalar color(0, 105, 205);
////    int frame_thickness = 4;
////    for (const auto &r : rectangles) {
////        cv::rectangle(img, r, color, frame_thickness);
////    }
//}

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
            rotate(src, src, cv::ROTATE_90_CLOCKWISE);
            flip(src, src, 1);
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

        float w = x + rw;
        float h = y + rh;
        float yFix, hFix;
        float xFix;

        // draw rectangle
//        for (const auto &r : rectangles) {
//            cv::rectangle(img, r, color, frame_thickness);
//        }

        switch (rotation) {
            case 0:
                cv::rectangle(rgba, rect, color,
                              frame_thickness); // TODO fix according to orientation
//                rectFace(rgba, y, x, h, w, RED);
//                drawDot(rgba, y, x, GREEN);
                break;

            case 90:
//                rectFace(rgba, x, y, w, h, RED);
//                drawDot(rgba, x, y, GREEN);
                cv::rectangle(rgba, rect, color,
                              frame_thickness); // TODO fix according to orientation

                break;

            case 180:
                // fix height
//                yFix = scrW - y;
//                hFix = yFix - rh;
//                rectFace(rgba, yFix, x, hFix, w, YELLOW);
//                drawDot(rgba, yFix, x, BLUE);
                 xFix = scrW - y - h;

                cv::rectangle(rgba, cv::Rect(xFix, x, h, w), color,
                              frame_thickness);
//                cv::rectangle(rgba, cv::Rect(x, yFix, w, hFix), color, frame_thickness); // TODO fix according to orientation
                break;

            case 270:
                // fix height
//                yFix = scrH - y;
//                hFix = yFix - rh;
//                rectFace(rgba, x, yFix, w, hFix, YELLOW);
//                drawDot(rgba, x, yFix, BLUE);
                cv::rectangle(rgba, rect, color,
                              frame_thickness); // TODO fix according to orientation
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
    cv::Mat &mRgb = *(cv::Mat *) mat_addr_rgba;
    cv::Mat mGr = cv::Mat();
    int mHeight = (int) height;
    int mRotation = (int) rotation;
//    if (!cv::color2Gray(mRgb, mGr)) {
//        lge("Grayscale conversion: failed!!!");
//    }
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

