package com.dttv.dtlive.utils;

/**
 * Created by dttv on 16-6-29.
 */
public class LiveJniLib {
    static {
        System.loadLibrary("dtlive_jni");
    }

    public static native int native_video_init(int width, int height);
    public static native int native_video_process(byte[] in, byte[] out, int size);
    public static native int native_video_release();

    public static native int native_stream_init(String uri);
    public static native int native_stream_send(byte[] data, int size);
    public static native int native_stream_release();

}
