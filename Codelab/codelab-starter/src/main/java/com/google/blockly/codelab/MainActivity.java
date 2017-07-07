package com.google.blockly.codelab;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

/**
 * The application's main (and only) activity, hosting the toolbar and content fragments.
 */
public class MainActivity extends AppCompatActivity {
    // View and fragment references:
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
