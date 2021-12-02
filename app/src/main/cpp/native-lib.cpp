#include <jni.h>
#include <string>
#include "opencv2/core.hpp"
#include <opencv2/imgproc.hpp>
#include "FaceDetector.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_testndk_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

FaceDetector face_detector;

extern "C"
JNIEXPORT void JNICALL
Java_com_example_testndk_demo_NativeClass_00024Companion_testFunction(JNIEnv *env, jobject thiz,
                                                                      jlong addr_rgba) {
    cv::Mat &img = *(cv::Mat *) addr_rgba;
    auto rectangles = face_detector.detect_face_rectangles(img);
    cv::Scalar color(0, 105, 205);
    int frame_thickness = 4;
    for (const auto &r : rectangles) {
        cv::rectangle(img, r, color, frame_thickness);
    }
}

