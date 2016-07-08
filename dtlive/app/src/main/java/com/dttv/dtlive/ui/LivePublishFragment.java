package com.dttv.dtlive.ui;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.dttv.dtlive.R;
import com.dttv.dtlive.utils.LiveJniLib;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link LivePublishFragment.OnLivePublishFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link LivePublishFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LivePublishFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnLivePublishFragmentInteractionListener mListener;

    static final String TAG = "LIVE-PUBLISH";

    private Camera mCamera;
    private CameraPreview mPreview;

    ImageButton captureButton;
    TextView tv_PublishServer;
    TextView tv_NetworkStatus;
    TextView tv_LiveStatus;

    private boolean isLiveing = false;
    private int mMaxEncodeFrameSize = 1920 * 1080 * 2;
    private ReentrantLock previewLock = new ReentrantLock();
    private ReentrantLock mStreamingLock = new ReentrantLock();
    Handler streamingHandler;
    private final int mStreamingInterval = 100;
    private List<byte[]> mListVideoFrames;
    byte[] mEncodedVideoFrame = new byte[mMaxEncodeFrameSize];

    private SharedPreferences mSharedPreferences;
    private String mRTMPServerIP = "192.168.1.101";
    private int mRTMPServerPort = 1935; // red5 port
    private String mLivePublishUri;


    public LivePublishFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment LivePublishFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static LivePublishFragment newInstance(String param1, String param2) {
        LivePublishFragment fragment = new LivePublishFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_live_publish, container, false);

        // publish server
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
        mLivePublishUri = mSharedPreferences.getString("publish_server", "rtmp://192.168.1.101:1935/live/test");

        // Add a listener to the Capture button
        captureButton = (ImageButton) view.findViewById(R.id.id_button_capture);
        captureButton.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isLiveing) {
                        stoptLive();
                    } else {
                        startLive();
                    }
                }
            }
        );

        tv_PublishServer = (TextView) view.findViewById(R.id.id_publish_server);
        tv_NetworkStatus = (TextView) view.findViewById(R.id.id_network_status);
        tv_LiveStatus = (TextView) view.findViewById(R.id.id_live_status);

        tv_PublishServer.setText(getText(R.string.publish_server).toString() + mLivePublishUri);
        tv_NetworkStatus.setText(getText(R.string.publish_network_status).toString() + getText(R.string.publish_network_status_wifi).toString());
        tv_LiveStatus.setText(getText(R.string.publish_live_status).toString() + getText(R.string.publish_live_status_no).toString());

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
        mPreview = new CameraPreview(this.getActivity(), mCamera);
        FrameLayout preview = (FrameLayout) view.findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        mListVideoFrames = new ArrayList<byte[]>();
        return view;
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
        int framesize = LiveJniLib.native_video_process(frame, mEncodedVideoFrame, size);
        if(framesize <= 0)
            return;
        mStreamingLock.lock();
        if(mListVideoFrames.size() < 10) {
            mListVideoFrames.add(new byte[framesize]);
            System.arraycopy(mEncodedVideoFrame, 0, mListVideoFrames.get(mListVideoFrames.size() - 1), 0, framesize);
        }
        mStreamingLock.unlock();

    };

    // Live
    private int startLive()
    {
        LiveJniLib.native_video_init(mPreview.getCurrentSize().width, mPreview.getCurrentSize().height);
        LiveJniLib.native_stream_init(mLivePublishUri);
        mPreview.startCaptureLive(previewCb);
        isLiveing = true;
        captureButton.setImageResource(R.mipmap.ic_adjust_white_48dp);
        tv_LiveStatus.setText(getText(R.string.publish_live_status).toString() + getText(R.string.publish_live_status_yes).toString());
        return 0;
    }

    // Live
    private int stoptLive()
    {
        mPreview.stopCaptureLive();
        LiveJniLib.native_video_release();
        LiveJniLib.native_stream_release();
        isLiveing = false;
        captureButton.setImageResource(R.mipmap.ic_brightness_1_white_48dp);
        tv_LiveStatus.setText(getText(R.string.publish_live_status).toString() + getText(R.string.publish_live_status_no).toString());
        return 0;
    }



    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onLivePublishFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnLivePublishFragmentInteractionListener) {
            mListener = (OnLivePublishFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnLivePublishFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        releaseCamera();              // release the camera immediately on pause event
        super.onDetach();
        mListener = null;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    private void doStreaming () {
        synchronized(this) {

            if(isLiveing) {

                mStreamingLock.lock();
                // rtmp streaming
                if (mListVideoFrames != null && mListVideoFrames.size() > 0) {
                    LiveJniLib.native_stream_send(mListVideoFrames.get(0), mListVideoFrames.get(0).length);
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

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnLivePublishFragmentInteractionListener {
        // TODO: Update argument type and name
        void onLivePublishFragmentInteraction(Uri uri);
    }
}
