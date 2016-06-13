#include <string.h>
#include <jni.h>
#include <inttypes.h>
#include <android/log.h>

#define LOGI(...) \
  ((void)__android_log_print(ANDROID_LOG_INFO, "dtlive::", __VA_ARGS__))

/* This is a trivial JNI example where we use a native method
 * to return a new VM String. See the corresponding Java source
 * file located at:
 *
 *   app/src/main/java/com/dttv/dtlive/CameraActivity.java
 */

 static int width;
 static int height;

extern "C" JNIEXPORT jint JNICALL
Java_com_dttv_dtlive_CameraActivity_native_video_encoder_init(JNIEnv *env, jobject thiz, jint width, jint height) {

    LOGI("Video Encoder Init");
    return 0;
}