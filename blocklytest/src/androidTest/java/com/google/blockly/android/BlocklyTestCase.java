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
import android.os.Handler;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.view.ContextThemeWrapper;

import com.google.blockly.android.ui.vertical.R;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Base class for Android tests with Mockito.
 */
public class BlocklyTestCase {
    protected Looper mTargetMainLooper = null;  // set during configureForUIThread()
    private Handler mHandler;

    /**
     * Default timeout of 1 second, which should be plenty for most UI actions.  Anything longer
     * is an error.  However, to step through this code with a debugger, use a much longer duration.
     */
    protected static final long DEFAULT_TEST_TIMEOUT_MILLIS = 1000L;

    private Context mThemeContext;
    protected long testTimeoutMs = DEFAULT_TEST_TIMEOUT_MILLIS;

    protected void configureForThemes() {
        mThemeContext = new ContextThemeWrapper(getContext(), R.style.BlocklyVerticalTheme);
        // To solve some issue with Dexmaker.  This allows us to use mockito.
        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());
    }

    // TODO: Investigate whether use of getContext() (and associated getMainLooper()) fixes the need
    //       for Looper.prepare().
    protected void configureForUIThread() {
        // Espresso support requires AndroidJUnitRunner, and that doesn't run tests on in the main
        // thread (and thus, not in a Looper).  Adding a Looper allows normal unit tests to run
        // correctly.
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        // Set up the Handler for used by runAndSync()
        mTargetMainLooper = InstrumentationRegistry.getTargetContext().getMainLooper();
        mHandler = new Handler(mTargetMainLooper);
    }

    protected Context getContext() {
        return mThemeContext != null ? mThemeContext : InstrumentationRegistry.getContext();
    }

    /**
     * Runs {@code runnable} on the test thread, and waits until the runnable completes before
     * returning.
     * @param runnable The code to run.
     */
    protected void runAndSync(final Runnable runnable) {
        final Throwable[] thrownRef = {null};
        final CountDownLatch latch = new CountDownLatch(1);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (Throwable e) {
                    thrownRef[0] = e;
                }

                // Defer the latch until after all posted Runnables have completed.
                // TODO: Consider using MessageQueue.isIdle() (API >= M)
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        latch.countDown();
                    }
                });
            }
        });
        awaitTimeout(latch);

        if (thrownRef[0] != null) {
            throw new IllegalStateException("Unhandled exception in mock main thread.",
                    thrownRef[0]);
        }
    }

    protected void awaitTimeout(CountDownLatch latch) {
        try {
            latch.await(testTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Timeout exceeded.", e);
        }
    }
}
