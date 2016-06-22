package com.dttv.dtlive.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.dttv.dtlive.R;
import com.dttv.dtlive.ui.LiveBrowserFragment.OnLiveBrowserListFragmentInteractionListener;
import com.dttv.dtlive.model.LiveChannelModel;
import com.dttv.dtlive.model.LiveChannelModel.LiveChannelItem;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link LiveChannelItem} and makes a call to the
 * specified {@link OnLiveBrowserListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class LiveBrowserAdapter extends RecyclerView.Adapter<LiveBrowserAdapter.ViewHolder> {

    private final List<LiveChannelModel.LiveChannelItem> mValues;
    private final OnLiveBrowserListFragmentInteractionListener mListener;

    public LiveBrowserAdapter(List<LiveChannelModel.LiveChannelItem> items, OnLiveBrowserListFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_live_browser_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mIdView.setText(mValues.get(position).id);
        holder.mTitleView.setText(mValues.get(position).title);
        holder.mContentView.setText(mValues.get(position).content);

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onLiveBrowserListFragmentInteraction(holder.mItem);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mIdView;
        public TextView mTitleView;
        public final TextView mContentView;
        public ImageView mThumbImageView;
        public LiveChannelItem mItem;


        public ViewHolder(View view) {
            super(view);
            mView = view;
            mIdView = (TextView) view.findViewById(R.id.id);
            mTitleView = (TextView) view.findViewById(R.id.title);
            mContentView = (TextView) view.findViewById(R.id.content);
            mThumbImageView = (ImageView) view.findViewById(R.id.thumb);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}
