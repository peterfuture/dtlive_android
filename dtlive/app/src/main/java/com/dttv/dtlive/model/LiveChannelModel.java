package com.dttv.dtlive.model;

import android.widget.ImageView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by dttv on 16-6-22.
 */

public class LiveChannelModel {

    /**
     * An array of sample (dummy) items.
     */
    public static final List<LiveChannelItem> ITEMS = new ArrayList<LiveChannelItem>();

    /**
     * A map of sample (dummy) items, by ID.
     */
    public static final Map<String, LiveChannelItem> ITEM_MAP = new HashMap<String, LiveChannelItem>();

    private static final int COUNT = 3;

    static {
        // Add some sample items.

        String id = "1";
        String uri = "http://devimages.apple.com/iphone/samples/bipbop/bipbopall.m3u8";
        String title = "HLS Test";
        String content = "HLS Test";
        String details = "HLS Test";
        LiveChannelItem item1 = new LiveChannelItem("1", uri, title, content, details);
        addItem(item1);
        LiveChannelItem item2 = new LiveChannelItem("2", uri, title, content, details);
        addItem(item2);
        LiveChannelItem item3 = new LiveChannelItem("3", uri, title, content, details);
        addItem(item3);
    }

    private static void addItem(LiveChannelItem item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.id, item);
    }

    /**
     * A dummy item representing a piece of content.
     */
    public static class LiveChannelItem {
        public final String id;
        public final String uri;
        public final String title;
        public final String content;
        public final String details;

        public LiveChannelItem(String id, String uri, String title, String content, String details) {
            this.id = id;
            this.uri = uri;
            this.title = title;
            this.content = content;
            this.details = details;
        }

        @Override
        public String toString() {
            return content;
        }
    }
}
