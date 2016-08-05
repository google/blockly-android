/*
 *  Copyright 2015 Google Inc. All Rights Reserved.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.google.blockly.android;

import android.content.Context;
import android.os.Looper;
import android.test.AndroidTestCase;
import android.view.ContextThemeWrapper;

import com.google.blockly.android.ui.vertical.R;

import org.mockito.MockitoAnnotations;

/**
 * Base class for Android tests with Mockito.
 */
public class MockitoAndroidTestCase extends AndroidTestCase {

    Context mThemeContext;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // Espresso support requires AndroidJUnitRunner, and that doesn't run tests on in the main
        // thread (and thus, not in a Looper).  Adding a Looper allows normal unit tests to run
        // correctly.
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        mThemeContext = new ContextThemeWrapper(getContext(), R.style.BlocklyVerticalTheme);
        // To solve some issue with Dexmaker.  This allows us to use mockito.
        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());
        MockitoAnnotations.initMocks(this);
    }

    @Override
    public Context getContext() {
        return mThemeContext != null ? mThemeContext : super.getContext();
    }
}
