package com.dttv.dtlive;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CameraActivity extends Activity {

    static final String TAG = "CAMERA-ACTIVITY";

    private Camera mCamera;
    private CameraPreview mPreview;
    private MediaRecorder mMediaRecorder;

    Button captureButton;
    Button buttonCapturePhoto;
    Button buttonCaptureVideo;
    Button buttonCaptureLive;
    private boolean isRecording = false;

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    private static final int CAPTURE_TYPE_PHOTO = 0;
    private static final int CAPTURE_TYPE_VIDEO = 1;
    private static final int CAPTURE_TYPE_LIVE = 2;
    int mCurrentMode = CAPTURE_TYPE_PHOTO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        // Create an instance of Camera
        mCamera = getCameraInstance();
        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

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
                            // Fixme
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
                        native_video_encoder_init(1280, 720);
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

    public native int native_video_encoder_init(int width, int height);
    public native int native_video_encoder_encode(byte[] in, byte[] out, int key);
    public native void native_video_encoder_release();

    static {
        System.loadLibrary("dtlive_jni");
    }

}
