package com.dttv.dtlive.ui;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.dttv.dtlive.utils.LiveJniLib;

public class AudioCapture {

    private AudioRecord audioCapture = null;

    static int mSampleRate = 44100;
    static int mChannels = 2;

    public void start() {
        int minBufferSize = AudioRecord.getMinBufferSize(mSampleRate, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        int targetSize = mSampleRate * mChannels;      // 1 seconds buffer size
        if (targetSize < minBufferSize) {
            targetSize = minBufferSize;
        }
        if (audioCapture == null) {
            try {
                audioCapture = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        mSampleRate,
                        AudioFormat.CHANNEL_IN_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        targetSize);
            } catch (IllegalArgumentException	 e) {
                audioCapture = null;
            }
        }

        LiveJniLib.native_audio_init(mSampleRate, mChannels);

        if ( audioCapture != null) {
            audioCapture.startRecording();
            AudioEncoder audioEncoder = new AudioEncoder();
            audioEncoder.start();
        }
    }

    public void stop() {
        audioCapture.release();
    }


    private class AudioEncoder extends Thread {
        private byte[] audioPCM = new byte[1024 * 32];
        private byte[] audioPacket = new byte[1024 * 1024];

        // 1s
        int packageSize = 44100*2*2;

        @Override
        public void run() {
            while (true) {
                int millis = (int) (System.currentTimeMillis() % 65535);

                int ret = audioCapture.read(audioPCM, 0, packageSize);
                if (ret == AudioRecord.ERROR_INVALID_OPERATION ||
                        ret == AudioRecord.ERROR_BAD_VALUE) {
                    break;
                }

                ret = LiveJniLib.native_audio_process(audioPCM, audioPacket, ret);
                if (ret <= 0) {
                    break;
                }
            }
        }
    }

}
