#include <stdlib.h>
#include <stdio.h>
#include <assert.h>

#include <jni.h>
#include <android/log.h>

#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, "dtlive::", __VA_ARGS__))

/* This is a trivial JNI example where we use a native method
 * to return a new VM String. See the corresponding Java source
 * file located at:
 *
 *   app/src/main/java/com/dttv/dtlive/CameraActivity.java
 */

static int video_encoder_init(JNIEnv *env, jobject thiz, jint width, jint height) {
    LOGI("Video Encoder Init");
    return 0;
}

static int video_encoder_encode(JNIEnv *env, jobject thiz, jbyteArray in, jbyteArray out, jint key) {
    LOGI("Video Encoder Init");
    return 0;
}

static int video_encoder_release(JNIEnv *env, jobject thiz) {
    LOGI("Video Encoder Init");
    return 0;
}


#define NELEM(x) ((int)(sizeof(x) / sizeof((x)[0])))
static const char *const kClassName = "com/dttv/dtlive/CameraActivity";

static JNINativeMethod g_Methods[] = {
    {"native_video_encoder_init",  "(II)I",   (void *) video_encoder_init},
    {"native_video_encoder_encode",  "([B[BI)I",   (void *) video_encoder_encode},
    {"native_video_encoder_release",  "()V",   (void *) video_encoder_release},
};

static int register_natives(JNIEnv *env) {
    jclass clazz;
    clazz = env->FindClass(kClassName);
    if (clazz == NULL) {
        LOGI("Error:Not found java activity");
        return JNI_FALSE;
    }

    if (env->RegisterNatives(clazz, g_Methods, NELEM(g_Methods)) < 0) {
        LOGI("Error: Register native failed");
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = NULL;

    if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
        LOGI("ERROR: GetEnv failed\n");
        return JNI_FALSE;
    }

    if (register_natives(env) < 0) {
        LOGI("ERROR: MediaPlayer native registration failed\n");
        return JNI_FALSE;
    }

    return JNI_VERSION_1_4;
}