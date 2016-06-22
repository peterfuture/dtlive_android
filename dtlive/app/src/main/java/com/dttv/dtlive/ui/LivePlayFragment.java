package com.dttv.dtlive.ui;

import android.app.Fragment;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.VideoView;

import com.dttv.dtlive.R;
import com.dttv.dtlive.model.LiveChannelModel;

import java.io.IOException;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link LivePlayFragment.OnLivePlayFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link LivePlayFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LivePlayFragment extends Fragment {

    static final String TAG = "PLAY-FRAGMENT";
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private VideoView mVideoView;
    private LiveChannelModel.LiveChannelItem mItem;

    private OnLivePlayFragmentInteractionListener mListener;


    public LivePlayFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment LivePlayFragment.
     */
    // TODO: Rename and change types and number of parameters
    /*
    public static LivePlayFragment newInstance(String param1, String param2) {
        LivePlayFragment fragment = new LivePlayFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);

        return fragment;
    }*/

    public static LivePlayFragment newInstance(LiveChannelModel.LiveChannelItem item) {
        LivePlayFragment fragment = new LivePlayFragment();
        fragment.mItem = item;
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
        //return inflater.inflate(R.layout.fragment_live_play, container, false);

        View view = inflater.inflate(R.layout.fragment_live_play, container, false);
        TextView title = (TextView) view.findViewById(R.id.title);
        title.setText(mItem.title);

        TextView content = (TextView) view.findViewById(R.id.content);
        content.setText(mItem.details);

        mVideoView = (VideoView) view.findViewById(R.id.id_videoview);
        final ImageButton mWatchButton = (ImageButton) view.findViewById(R.id.pre_play_button);
        mWatchButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                try {
                    Log.i(TAG, "Start playing:" + mItem.uri);
                    Uri uri=Uri.parse(mItem.uri);
                    mVideoView.setVideoURI(uri);
                    mVideoView.start();
                    mVideoView.requestFocus();
                    mWatchButton.setVisibility(View.INVISIBLE);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onLivePlayFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnLivePlayFragmentInteractionListener) {
            mListener = (OnLivePlayFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnLivePlayFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        mVideoView.stopPlayback();
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnLivePlayFragmentInteractionListener {
        // TODO: Update argument type and name
        void onLivePlayFragmentInteraction(Uri uri);
    }
}
