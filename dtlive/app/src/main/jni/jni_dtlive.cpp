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

#include "pthread.h"
#define dt_lock_t         pthread_mutex_t
#define dt_lock_init(x,v) pthread_mutex_init(x,v)
#define dt_lock(x)        pthread_mutex_lock(x)
#define dt_unlock(x)      pthread_mutex_unlock(x)

#include "codec_api.h"

struct video_processor
{
    int width;
    int height;
    int count;

    dt_lock_t mutex;
    struct codec_context *video_codec;
};

static struct video_processor vp;

extern "C" int Java_com_dttv_dtlive_utils_LiveJniLib_native_1video_1init (JNIEnv *env, jobject thiz, jint width, jint height) {
    codec_register_all();
    memset(&vp, 0, sizeof(struct video_processor));
    dt_lock_init(&vp.mutex, NULL);
    vp.width = width;
    vp.height = height;

    struct codec_para para;
    para.width = width;
    para.height = height;
    para.is_encoder = 1;
    para.media_format = CODEC_MEDIA_FORMAT_H264;
    para.media_type = CODEC_MEDIA_TYPE_VIDEO;
    vp.video_codec = codec_create_codec(&para);
    LOGI("Video Encoder Init, [%d:%d]", width, height);
    return 0;
}

unsigned char* as_unsigned_char_array(JNIEnv *env, jbyteArray array) {
    int len = env->GetArrayLength (array);
    unsigned char* buf = new unsigned char[len];
    env->GetByteArrayRegion (array, 0, len, reinterpret_cast<jbyte*>(buf));
    return buf;
}

extern "C"  int Java_com_dttv_dtlive_utils_LiveJniLib_native_1video_1process(JNIEnv *env, jobject thiz, jbyteArray in, jbyteArray out, jint size) {
    dt_lock(&vp.mutex);

    struct codec_packet pkt;
    pkt.data = (uint8_t *)malloc(1920*1080*4);
    unsigned char *buf = as_unsigned_char_array(env, in);
    struct codec_frame frame;
    frame.data = (uint8_t *)buf;
    frame.size = size;
    frame.key = 1;

    int ret = codec_encode_frame(vp.video_codec, &pkt, &frame);
    if(ret > 0) {
        LOGI("Encode one frame ok");
    }

    free(pkt.data);
    free(buf);
    dt_unlock(&vp.mutex);
    return 0;
}

extern "C" int Java_com_dttv_dtlive_utils_LiveJniLib_native_1video_1release(JNIEnv *env, jobject thiz) {
    codec_destroy_codec(vp.video_codec);
    return 0;
}

extern "C" int Java_com_dttv_dtlive_utils_LiveJniLib_native_1stream_1init(JNIEnv *env, jobject thiz, jstring ip, int port) {
    return 0;
}

extern "C" int Java_com_dttv_dtlive_utils_LiveJniLib_native_1stream_1send(JNIEnv *env, jobject thiz, jbyteArray data, jint length) {
    return 0;
}

extern "C" int Java_com_dttv_dtlive_utils_LiveJniLib_native_1stream_1release(JNIEnv *env, jobject thiz) {
    return 0;
}

extern "C" jint JNIEXPORT JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {
    JNIEnv* jni;
    if (jvm->GetEnv(reinterpret_cast<void**>(&jni), JNI_VERSION_1_6) != JNI_OK)
        return -1;
    return JNI_VERSION_1_6;
}

extern "C" jint JNIEXPORT JNICALL JNI_OnUnLoad(JavaVM *jvm, void *reserved) {
    return 0;
}