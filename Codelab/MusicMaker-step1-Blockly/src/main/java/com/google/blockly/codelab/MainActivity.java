/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    private BlocklyFragment mBlocklyFragment = new BlocklyFragment();

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

    public void editSoundScript(int buttonNumber) {
        mActionBar.setDisplayHomeAsUpEnabled(true);
        FragmentTransaction fragTx = getSupportFragmentManager().beginTransaction();
        fragTx.replace(R.id.content_frame, mBlocklyFragment, "BlocklyFragment");
        fragTx.addToBackStack("blockly");
        fragTx.commit();
    }
}
