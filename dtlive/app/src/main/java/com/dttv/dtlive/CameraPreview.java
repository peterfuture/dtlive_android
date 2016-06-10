package com.dttv.dtlive;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import java.io.IOException;
import java.util.*;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    static final String TAG = "CAMERA-PREVIEW";

    private SurfaceHolder mHolder;
    private Camera mCamera;

    private List<Camera.Size> mListSupportedSizes;
    Camera.Size mCurrentSize;
    private List<int[]> mListSupportedFps;
    int mCurrentFrameRate;

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        // setup camera parameter
        Camera.Parameters p = mCamera.getParameters();

        mListSupportedFps = p.getSupportedPreviewFpsRange();
        for(int i=0; i< mListSupportedFps.size(); i++)
        {
            Log.d(TAG, "Support FrameFps: " + "[" + mListSupportedFps.get(i)[0] + ":" + mListSupportedFps.get(i)[1] + "]" );
        }

        mListSupportedSizes = p.getSupportedPreviewSizes();
        for(Camera.Size size : mListSupportedSizes)
        {
            Log.d(TAG, "Support Size: [" + size.width + ":" +size.height +"]");
        }
        mCurrentSize = mListSupportedSizes.get(mListSupportedSizes.size()-1);
        p.setPreviewSize(mCurrentSize.width, mCurrentSize.height);
        mCamera.setParameters(p);

        Log.i(TAG, "OnCreate\n");
    }

    public Camera.Size getCurrentSize()
    {
        return mCurrentSize;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }

        Log.i(TAG, "SurfaceCreated \n");
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        Log.i(TAG, "surfaceChanged width: " + w + " height:" + h);
        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

}