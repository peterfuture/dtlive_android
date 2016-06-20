package com.dttv.dtlive.ui;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.Bundle;
import android.os.Handler;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.dttv.dtlive.R;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.BitSet;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class CameraActivity extends AppCompatActivity {

    static final String TAG = "CAMERA-ACTIVITY";

    private Camera mCamera;
    private CameraPreview mPreview;
    private MediaRecorder mMediaRecorder;

    Button captureButton;
    Button buttonCapturePhoto;
    Button buttonCaptureVideo;
    Button buttonCaptureLive;
    private boolean isRecording = false;
    private boolean isLiveing = false;

    private int mMaxEncodeFrameSize = 1920 * 1080 * 2;

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    private static final int CAPTURE_TYPE_PHOTO = 0;
    private static final int CAPTURE_TYPE_VIDEO = 1;
    private static final int CAPTURE_TYPE_LIVE = 2;
    int mCurrentMode = CAPTURE_TYPE_PHOTO;

    private ReentrantLock previewLock = new ReentrantLock();

    private int STREAMING_MODE_RTMP_BIT = 0;
    private int STREAMING_MODE_WEBSOCKET_BIT = 1;
    private BitSet mStreamMode = new BitSet();

    private ReentrantLock mStreamingLock = new ReentrantLock();
    private StreamingServer streamingServer = null;
    private int mWebSocketPort = 9000;
    Handler streamingHandler;
    private final int mStreamingInterval = 100;
    private List<byte[]> mListVideoFrames;
    byte[] mEncodedVideoFrame = new byte[mMaxEncodeFrameSize];

    private String mRTMPServerIP = "192.168.1.1";
    private int mRTMPServerPort = 1935; // red5 port


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        // Add a listener to the Capture button
        captureButton = (Button) findViewById(R.id.button_capture);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(mCurrentMode == CAPTURE_TYPE_PHOTO) {
                            // get an image from the camera
                            mCamera.takePicture(null, null, mPicture);
                        }

                        if(mCurrentMode == CAPTURE_TYPE_VIDEO) {
                            if (isRecording) {
                                // stop recording and release camera
                                mMediaRecorder.stop();  // stop the recording
                                releaseMediaRecorder(); // release the MediaRecorder object
                                mCamera.lock();         // take camera access back from MediaRecorder

                                // inform the user that recording has stopped
                                setCaptureVideoButtonText("Capture Video");
                                isRecording = false;
                            } else {
                                // initialize video camera
                                if (prepareVideoRecorder()) {
                                    // Camera is available and unlocked, MediaRecorder is prepared,
                                    // now you can start recording
                                    mMediaRecorder.start();

                                    // inform the user that recording has started
                                    setCaptureVideoButtonText("Stop");
                                    isRecording = true;
                                } else {
                                    // prepare didn't work, release the camera
                                    releaseMediaRecorder();
                                    // inform user
                                }
                            }
                        }

                        if(mCurrentMode == CAPTURE_TYPE_LIVE) {
                            if (isLiveing) {
                                stoptLive();
                            } else {
                                startLive();
                            }
                        }


                    }
                }
        );

        // Add a listener to the Capture type button
        buttonCapturePhoto = (Button) findViewById(R.id.id_capture_type_photo);
        buttonCapturePhoto.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCurrentMode = CAPTURE_TYPE_PHOTO;
                        captureTypeChange(mCurrentMode);
                    }
                }
        );

        buttonCaptureVideo = (Button) findViewById(R.id.id_capture_type_video);
        buttonCaptureVideo.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCurrentMode = CAPTURE_TYPE_VIDEO;
                        captureTypeChange(mCurrentMode);
                    }
                }
        );

        buttonCaptureLive = (Button) findViewById(R.id.id_capture_type_live);
        buttonCaptureLive.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCurrentMode = CAPTURE_TYPE_LIVE;
                        captureTypeChange(mCurrentMode);
                    }
                }
        );

        captureTypeChange(mCurrentMode);
        streamingHandler = new Handler();
        streamingHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                doStreaming();
            }
        }, mStreamingInterval);


        // Create an instance of Camera
        mCamera = getCameraInstance();
        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        mStreamMode.set(STREAMING_MODE_RTMP_BIT, false);
        mStreamMode.set(STREAMING_MODE_WEBSOCKET_BIT, false);
    }

    private void captureTypeChange(int type)
    {
        buttonCapturePhoto.setTextColor(Color.BLACK);
        buttonCaptureVideo.setTextColor(Color.BLACK);
        buttonCaptureLive.setTextColor(Color.BLACK);
        if(type == CAPTURE_TYPE_PHOTO)
            buttonCapturePhoto.setTextColor(Color.BLUE);
        if(type == CAPTURE_TYPE_VIDEO)
            buttonCaptureVideo.setTextColor(Color.BLUE);
        if(type == CAPTURE_TYPE_LIVE)
            buttonCaptureLive.setTextColor(Color.BLUE);
    }

    /**
     * Check if this device has a camera
     */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null) {
                //Log.d(TAG, "Error creating media file, check storage permissions: " +
                //        e.getMessage());
                return;
            }
            Log.i(TAG, "Save image to: " + pictureFile.getPath());
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }

            mCamera.startPreview();
        }
    };

    /**
     * Create a file Uri for saving an image or video
     */
    private static Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * Create a File for saving an image or video
     */
    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    private void setCaptureVideoButtonText(String title) {
        captureButton.setText(title);
    }

    // video capture
    private boolean prepareVideoRecorder() {

        //mCamera = getCameraInstance();
        mMediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);
        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        // Use the same size for recording profile.
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        profile.videoFrameWidth = mPreview.getCurrentSize().width;
        profile.videoFrameHeight = mPreview.getCurrentSize().height;
        mMediaRecorder.setProfile(profile);

        // Step 4: Set output file
        mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());

        // Step 5: Set the preview output
        //mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

        // Step 6: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private Camera.PreviewCallback previewCb = new Camera.PreviewCallback() {
        public void onPreviewFrame(byte[] frame, Camera c) {
            previewLock.lock();
            doVideoEncode(frame);
            c.addCallbackBuffer(frame);
            previewLock.unlock();
        }
    };

    private void doVideoEncode(byte[] frame) {
        int picWidth = mPreview.getCurrentSize().width;
        int picHeight = mPreview.getCurrentSize().height;
        int size = picWidth*picHeight + picWidth*picHeight/2;
        int framesize = native_video_process(frame, mEncodedVideoFrame, size);
        if(framesize <= 0)
            return;

        byte[] videoHeader = new byte[8];
        int millis = (int)(System.currentTimeMillis() % 65535);
        videoHeader[0] = (byte)0x19;
        videoHeader[1] = (byte)0x79;
        // timestamp
        videoHeader[2] = (byte)(millis & 0xFF);
        videoHeader[3] = (byte)((millis>>8) & 0xFF);
        // length
        videoHeader[4] = (byte)(framesize & 0xFF);
        videoHeader[5] = (byte)((framesize>>8) & 0xFF);
        videoHeader[6] = (byte)((framesize>>16) & 0xFF);
        videoHeader[7] = (byte)((framesize>>24) & 0xFF);

        mStreamingLock.lock();
        if(mListVideoFrames.size() < 10) {
            mListVideoFrames.add(new byte[framesize+8]);
            System.arraycopy(videoHeader, 0, mListVideoFrames.get(mListVideoFrames.size()), 0, 8);
            System.arraycopy(mEncodedVideoFrame, 0, mListVideoFrames.get(mListVideoFrames.size()), 8, framesize);
        }
        mStreamingLock.unlock();
    };

    // Live
    private int startLive()
    {
        if(isRecording) {
            Log.i(TAG, "Error, quit record video first");
            return 0;
        }

        native_video_init(mPreview.getCurrentSize().width, mPreview.getCurrentSize().height);
        native_stream_init(mRTMPServerIP, mRTMPServerPort);
        mPreview.startCaptureLive(previewCb);
        setCaptureVideoButtonText("Stop");
        isLiveing = true;
        return 0;
    }

    // Live
    private int stoptLive()
    {
        mPreview.stopCaptureLive();
        setCaptureVideoButtonText("Capture");
        native_video_release();
        native_stream_release();
        isLiveing = false;
        return 0;
    }


    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    private void doStreaming () {
        synchronized(CameraActivity.this) {

            if(isLiveing) {

                mStreamingLock.lock();

                // websocket streaming
                if (mStreamMode.get(STREAMING_MODE_WEBSOCKET_BIT) == true) {
                    if (mListVideoFrames.size() > 0) {
                        streamingServer.sendMedia(mListVideoFrames.get(0), mListVideoFrames.get(0).length);
                    }
                }

                // rtmp streaming
                if(mStreamMode.get(STREAMING_MODE_RTMP_BIT) == true) {
                    if (mListVideoFrames.size() > 0) {
                        native_stream_send(mListVideoFrames.get(0), mListVideoFrames.get(0).length);
                    }
                }
                if (mListVideoFrames.size() > 0) {
                    streamingServer.sendMedia(mListVideoFrames.get(0), mListVideoFrames.get(0).length);
                    mListVideoFrames.remove(0);
                }

                mStreamingLock.unlock();
            }
        }

        streamingHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                doStreaming();
            }
        }, mStreamingInterval);

    }

    private class StreamingServer extends WebSocketServer {
        private WebSocket mediaSocket = null;
        public boolean inStreaming = false;
        ByteBuffer buf = ByteBuffer.allocate(mMaxEncodeFrameSize);

        public StreamingServer( int port) throws UnknownHostException {
            super( new InetSocketAddress( port ) );
        }

        public boolean sendMedia(byte[] data, int length) {
            boolean ret = false;

            if ( inStreaming == true) {
                buf.clear();
                buf.put(data, 0, length);
                buf.flip();
            }

            if ( inStreaming == true) {
                mediaSocket.send( buf );
                ret = true;
            }

            return ret;
        }

        @Override
        public void onOpen( WebSocket conn, ClientHandshake handshake ) {
            if ( inStreaming == true) {
                conn.close();
            } else {
                mediaSocket = conn;
                inStreaming = true;
            }
        }

        @Override
        public void onClose( WebSocket conn, int code, String reason, boolean remote ) {
            if ( conn == mediaSocket) {
                inStreaming = false;
                mediaSocket = null;
            }
        }

        @Override
        public void onError( WebSocket conn, Exception ex ) {
            if ( conn == mediaSocket) {
                inStreaming = false;
                mediaSocket = null;
            }
        }

        @Override
        public void onMessage( WebSocket conn, ByteBuffer blob ) {

        }

        @Override
        public void onMessage( WebSocket conn, String message ) {

        }

    }


    public native int native_video_init(int width, int height);
    public native int native_video_process(byte[] in, byte[] out, int size);
    public native int native_video_release();

    private native int native_stream_init(String ip, int port);
    public native int native_stream_send(byte[] data, int size);
    public native int native_stream_release();


    static {
        System.loadLibrary("dtlive_jni");
    }

}
