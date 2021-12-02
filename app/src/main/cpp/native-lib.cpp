#include <jni.h>
#include <string>
#include "opencv2/core.hpp"
#include <opencv2/imgproc.hpp>

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_testndk_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_testndk_demo_NativeClass_00024Companion_testFunction(JNIEnv *env, jobject thiz,
                                                                      jlong addr_rgba) {
    cv::Mat &img = *(cv::Mat *) addr_rgba;
    cv::cvtColor(img, img, cv::COLOR_RGB2GRAY);
}