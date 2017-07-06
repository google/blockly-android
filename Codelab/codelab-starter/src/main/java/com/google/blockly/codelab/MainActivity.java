package com.google.blockly.codelab;

import android.support.annotation.IntDef;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * The application's main (and only) activity, hosting the toolbar and content fragments.
 */
public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_MODE = "MODE";

    @Retention(SOURCE)
    @IntDef({MODE_PLAY, MODE_EDIT})
    public @interface AppMode {}
    public static final int MODE_PLAY = 1;
    public static final int MODE_EDIT = 2;

    private int mMode = MODE_PLAY;

    private ActionBar mActionBar;
    private FragmentManager mFragmentManager;
    private PlaybackFragment mPlaybackFragment = new PlaybackFragment();
    private EditFragment mEditFragment = new EditFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowHomeEnabled(true);

        mFragmentManager = getSupportFragmentManager();
        FragmentTransaction fragTx = getSupportFragmentManager().beginTransaction();
        fragTx.add(R.id.content_frame, mPlaybackFragment, "PlaybackFragment");
        fragTx.commit();
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (mEditFragment.isAdded()) {
            mFragmentManager.popBackStack();
            mActionBar.setDisplayHomeAsUpEnabled(false);
            return true;
        }
        return false;
    }

    /* package */ void startSelectForEdit() {
        mActionBar.setDisplayHomeAsUpEnabled(true);
        FragmentTransaction fragTx = getSupportFragmentManager().beginTransaction();
        fragTx.replace(R.id.content_frame, mEditFragment, "EditFragment");
        fragTx.addToBackStack("edit");
        fragTx.commit();
    }
}
