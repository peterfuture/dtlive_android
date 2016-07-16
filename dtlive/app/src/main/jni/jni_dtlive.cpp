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

#include "codec_api.h"
#include "rtmp_api.h"
#include "flvmux_api.h"

struct codec_context *video_codec;
static struct codec_context *audio_codec;
static struct rtmp_context *rtmp_handle = NULL;
static struct flvmux_context *flvmux_handle = NULL;

extern "C" int Java_com_dttv_dtlive_utils_LiveJniLib_native_1audio_1init (JNIEnv *env, jobject thiz, jint samplerate, jint channels) {
    codec_register_all();

    struct codec_para para;
    para.media_format = CODEC_MEDIA_FORMAT_AAC;
    para.media_type = CODEC_MEDIA_TYPE_AUDIO;
    para.samplerate = samplerate;
    para.channels = channels;
    para.is_encoder = 1;
    audio_codec = codec_create_codec(&para);
    LOGI("Audio Encoder Init, samplerate:%d channels:%d]", samplerate, channels);
    return 0;
}

extern "C"  int Java_com_dttv_dtlive_utils_LiveJniLib_native_1audio_1process(JNIEnv *env, jobject thiz, jbyteArray in, jbyteArray out, jint size) {

    jboolean isCopy = JNI_TRUE;
    jbyte* inbuf = env->GetByteArrayElements(in, NULL);
    jbyte* outbuf = env->GetByteArrayElements(out, &isCopy);

    struct codec_packet pkt;
    pkt.data = (uint8_t *)outbuf;
    struct codec_frame frame;
    frame.data = (uint8_t *)inbuf;
    frame.size = size;
    frame.key = 1;

    int ret = codec_encode_frame(audio_codec, &pkt, &frame);
    if(ret < 0) {
        LOGI("Encode audio frame ok");
    }

    env->ReleaseByteArrayElements(out, outbuf, 0);
    env->ReleaseByteArrayElements(in, inbuf, JNI_ABORT);
    return ret;
}

extern "C" int Java_com_dttv_dtlive_utils_LiveJniLib_native_1audio_1release(JNIEnv *env, jobject thiz) {
    codec_destroy_codec(audio_codec);
    audio_codec = NULL;
    return 0;
}


extern "C" int Java_com_dttv_dtlive_utils_LiveJniLib_native_1video_1init (JNIEnv *env, jobject thiz, jint width, jint height) {
    codec_register_all();

    struct codec_para para;
    para.media_type = CODEC_MEDIA_TYPE_VIDEO;
    para.media_format = CODEC_MEDIA_FORMAT_H264;
    para.width = width;
    para.height = height;
    para.is_encoder = 1;

    video_codec = codec_create_codec(&para);
    LOGI("Video Encoder Init, [%d:%d]", width, height);
    return 0;
}

extern "C"  int Java_com_dttv_dtlive_utils_LiveJniLib_native_1video_1process(JNIEnv *env, jobject thiz, jbyteArray in, jbyteArray out, jint size) {

    jboolean isCopy = JNI_TRUE;
    jbyte* inbuf = env->GetByteArrayElements(in, NULL);
    jbyte* outbuf = env->GetByteArrayElements(out, &isCopy);

    struct codec_packet pkt;
    pkt.data = (uint8_t *)outbuf;
    struct codec_frame frame;
    frame.data = (uint8_t *)inbuf;
    frame.size = size;
    frame.key = 1;

    int ret = codec_encode_frame(video_codec, &pkt, &frame);
    if(ret < 0) {
        LOGI("Encode one frame faild");
    }

    env->ReleaseByteArrayElements(out, outbuf, 0);
    env->ReleaseByteArrayElements(in, inbuf, JNI_ABORT);
    return ret;
}

extern "C" int Java_com_dttv_dtlive_utils_LiveJniLib_native_1video_1release(JNIEnv *env, jobject thiz) {
    codec_destroy_codec(video_codec);
    video_codec = NULL;
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

    jbyte* inbuf = env->GetByteArrayElements(data, NULL);
    LOGI("BEFORE SEND");
    for(int i = 0; i < 100; i += 5) {
        LOGI("%02x %02x %02x %02x %02x \n", inbuf[i], inbuf[i+1], inbuf[i+2], inbuf[i+3], inbuf[i+4]);
    }
    LOGI("AFTER SEND");
    int size = 0;
    if(rtmp_handle && flvmux_handle) {

        struct flvmux_packet in, out;
        memset(&in, 0, sizeof(struct flvmux_packet));
        memset(&out, 0, sizeof(struct flvmux_packet));
        in.data = (uint8_t *)inbuf;
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