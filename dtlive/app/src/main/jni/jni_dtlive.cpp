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
#include "rtmp_api.h"
#include "flvmux_api.h"

struct video_processor
{
    int width;
    int height;
    int count;

    dt_lock_t mutex;
    struct codec_context *video_codec;
};

static struct video_processor vp;
static struct rtmp_context *rtmp_handle = NULL;
static struct flvmux_context *flvmux_handle = NULL;

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


    jboolean isCopy = JNI_TRUE;
    jbyte* inbuf = env->GetByteArrayElements(in, NULL);
    jbyte* outbuf = env->GetByteArrayElements(out, &isCopy);

    struct codec_packet pkt;
    pkt.data = (uint8_t *)outbuf;
    struct codec_frame frame;
    frame.data = (uint8_t *)inbuf;
    frame.size = size;
    frame.key = 1;

    int ret = codec_encode_frame(vp.video_codec, &pkt, &frame);
    if(ret < 0) {
        LOGI("Encode one frame ok");
#if 0
        for(int i = 0; i < 100; i += 5) {
            LOGI("%02x %02x %02x %02x %02x \n", frame.data[i], frame.data[i+1], frame.data[i+2], frame.data[i+3], frame.data[i+4]);
        }
        LOGI("AFTER PROCESS");
#endif
    }

    dt_unlock(&vp.mutex);

    env->ReleaseByteArrayElements(out, outbuf, 0);
    env->ReleaseByteArrayElements(in, inbuf, JNI_ABORT);
    return ret;
}

extern "C" int Java_com_dttv_dtlive_utils_LiveJniLib_native_1video_1release(JNIEnv *env, jobject thiz) {
    codec_destroy_codec(vp.video_codec);
    return 0;
}

extern "C" int Java_com_dttv_dtlive_utils_LiveJniLib_native_1stream_1init(JNIEnv *env, jobject thiz, jstring uri) {
    struct rtmp_para rtmp_para;
    memset(&rtmp_para, 0, sizeof(struct rtmp_para));
    rtmp_para.write_enable = 1;

    jboolean isCopy;
    const char *server_addr = env->GetStringUTFChars(uri, &isCopy);
    strcpy(rtmp_para.uri, server_addr);
    LOGI("publish url:%s\n", server_addr);
    rtmp_handle = rtmp_open(&rtmp_para);
    if(!rtmp_handle) {
        LOGI("RTMP Open Failed \n");
        return -1;
    }

    struct flvmux_para flv_para;
    memset(&flv_para, 0, sizeof(struct flvmux_para));
    flv_para.has_video = 1;
    flvmux_handle = flvmux_open(&flv_para);
    rtmp_write(rtmp_handle, (uint8_t *)flvmux_handle->header, (int)flvmux_handle->header_size);
    LOGI("RTMP Open ok and send header size:%d \n", (int)flvmux_handle->header_size);
    return 0;
}

extern "C" int Java_com_dttv_dtlive_utils_LiveJniLib_native_1stream_1send(JNIEnv *env, jobject thiz, jbyteArray data, jint length) {

    unsigned char *buf = as_unsigned_char_array(env, data);
    LOGI("BEFORE SEND");
    for(int i = 0; i < 100; i += 5) {
        LOGI("%02x %02x %02x %02x %02x \n", buf[i], buf[i+1], buf[i+2], buf[i+3], buf[i+4]);
    }
    LOGI("AFTER SEND");
    int size = 0;
    if(rtmp_handle && flvmux_handle) {

        struct flvmux_packet in, out;
        memset(&in, 0, sizeof(struct flvmux_packet));
        memset(&out, 0, sizeof(struct flvmux_packet));
        in.data = buf;
        in.size = length;

        size = flvmux_setup_video_frame(flvmux_handle, &in, &out);
        if(size < 0) {
            LOGI("flv setup video frame failed \n");
            return 0;
        }
        size = rtmp_write(rtmp_handle, out.data, size);
        if(size < 0) {
            LOGI("rtmp send frame failed:%d", size);
        }
        free(out.data);

        LOGI("rtmp send frame ok:%d", size);
    }
    return 0;
}

extern "C" int Java_com_dttv_dtlive_utils_LiveJniLib_native_1stream_1release(JNIEnv *env, jobject thiz) {
    if(rtmp_handle)
        rtmp_close(rtmp_handle);
    if(flvmux_handle)
        flvmux_close(flvmux_handle);
    rtmp_handle = NULL;
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