package com.dttv.dtlive.ui;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.IdRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.dttv.dtlive.R;
import com.dttv.dtlive.model.LiveChannelModel;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnMenuTabClickListener;

public class MainActivity extends AppCompatActivity
        implements LiveBrowserFragment.OnLiveBrowserListFragmentInteractionListener,
        LivePlayFragment.OnLivePlayFragmentInteractionListener {

    // UI
    private BottomBar mBottomBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // top toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.id_camera_activity_toolbar);
        setSupportActionBar(toolbar);
        // bottom bar
        mBottomBar = BottomBar.attach(this, savedInstanceState);
        mBottomBar.setItems(R.menu.main_activity_bottom_menu);
        mBottomBar.setOnMenuTabClickListener(new OnMenuTabClickListener() {
            @Override
            public void onMenuTabSelected(@IdRes int menuItemId) {
                if (menuItemId == R.id.id_do_live) {
                    // The user selected item number one.
                    Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                    startActivity(intent);
                }

                if (menuItemId == R.id.id_watch_live) {
                    // The user selected item number one.
                }

                if (menuItemId == R.id.id_acount) {
                    // The user selected item number one.
                }
            }

            @Override
            public void onMenuTabReSelected(@IdRes int menuItemId) {
                if (menuItemId == R.id.id_do_live) {
                    // The user reselected item number one, scroll your content to top.
                    Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                    startActivity(intent);
                }
            }
        });

        // Setting colors for different tabs when there's more than three of them.
        // You can set colors for tabs in three different ways as shown below.
        mBottomBar.mapColorForTab(0, ContextCompat.getColor(this, R.color.colorAccent));
        mBottomBar.mapColorForTab(1, 0xFF5D4037);
        mBottomBar.mapColorForTab(2, "#7B1FA2");
        //mBottomBar.mapColorForTab(3, "#FF5252");
        //mBottomBar.mapColorForTab(4, "#FF9800");
        mBottomBar.noTopOffset();

        // fragment setup
        LiveBrowserFragment fragment = new LiveBrowserFragment();
        fragment.setArguments(getIntent().getExtras());
        getSupportFragmentManager().beginTransaction()
                .add(R.id.id_main_fragment, fragment).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_activity_top_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.id_action_settings:
                // User chose the "Settings" item, show the app settings UI...
                return true;

            case R.id.id_action_favorite:
                // User chose the "Favorite" action, mark the current item
                // as a favorite...
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    public void onLiveBrowserListFragmentInteraction(LiveChannelModel.LiveChannelItem item) {
        // The user selected the headline of an article from the HeadlinesFragment
        // Do something here to display that article
    }

    public void onLivePlayFragmentInteraction(Uri uri)
    {
        // The user selected the headline of an article from the HeadlinesFragment
        // Do something here to display that article
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Necessary to restore the BottomBar's state, otherwise we would
        // lose the current tab on orientation change.
        mBottomBar.onSaveInstanceState(outState);
    }

}
