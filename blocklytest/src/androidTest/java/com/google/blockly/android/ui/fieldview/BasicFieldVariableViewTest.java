/*
 *  Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.blockly.android.ui.fieldview;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.google.blockly.android.MockitoAndroidTestCase;
import com.google.blockly.android.control.NameManager;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.model.FieldVariable;

import org.mockito.AdditionalAnswers;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests for {@link BasicFieldVariableView}.
 */
public class BasicFieldVariableViewTest extends MockitoAndroidTestCase {
    /**
     * Default timeout of 1 second, which should be plenty for all FieldVariableView actions.
     * Anything longer is an error.  However, to step through this code with a debugger, use
     * a much longer duration.
     */
    private static final long TIMEOUT = 1000L;

    @Mock
    private WorkspaceHelper mMockWorkspaceHelper;

    private FieldVariable mFieldVariable;
    private String[] mVariables = new String[] {"var1", "var2", "var3"};
    private NameManager mNameManager;
    private BasicFieldVariableView.VariableViewAdapter mVariableAdapter;

    private Context mMockContext;
    private HandlerThread mThread;
    private Looper mLooper;
    private Handler mHandler;
    private Throwable mExceptionInThread = null;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mThread = new HandlerThread("DraggerTest");
        mThread.start();
        mLooper = mThread.getLooper();
        mHandler = new Handler(mLooper);

        mMockContext = Mockito.mock(Context.class, AdditionalAnswers.delegatesTo(getContext()));
        Mockito.doReturn(mLooper).when(mMockContext).getMainLooper();

        mFieldVariable = new FieldVariable("field", "var2");

        mNameManager = new NameManager.VariableNameManager();
        mNameManager.addName("var1");
        mNameManager.addName(mFieldVariable.getVariable());
        mNameManager.addName("var3");

        mVariableAdapter = new BasicFieldVariableView.VariableViewAdapter(getContext(), mNameManager,
                android.R.layout.simple_spinner_item);
    }

    // Verify object instantiation.
    public void testInstantiation() {
        final BasicFieldVariableView[] view = new BasicFieldVariableView[1];
        runAndSync(new Runnable() {
            @Override
            public void run() {
                view[0] = makeFieldVariableView();
            }
        }, TIMEOUT);

        assertSame(mFieldVariable, view[0].getField());
        assertEquals(mVariables.length + 2, view[0].getCount());
        assertEquals(mFieldVariable.getVariable(), (String) (view[0].getSelectedItem()));
    }

    // Verify update of field when an item is selected from the dropdown.
    // TODO(#69): need tests (using Espresso?) to confirm that user interaction has the same
    //            effect as calling BasicFieldVariableView.setSelection().
    public void testUpdateFieldFromView() {
        final BasicFieldVariableView view = makeFieldVariableView();

        runAndSync(new Runnable() {
            @Override
            public void run() {
                view.setSelection(2);
            }
        }, TIMEOUT);
        assertEquals(mVariables[2], mFieldVariable.getVariable());
        assertEquals(view.getSelectedItem().toString(), mFieldVariable.getVariable());

        runAndSync(new Runnable() {
            @Override
            public void run() {
                view.setSelection(0);
            }
        }, TIMEOUT);
        assertEquals(mVariables[0], mFieldVariable.getVariable());
        assertEquals(view.getSelectedItem().toString(), mFieldVariable.getVariable());

        runAndSync(new Runnable() {
            @Override
            public void run() {
                view.setSelection(1);
            }
        }, TIMEOUT);
        assertEquals(mVariables[1], mFieldVariable.getVariable());
        assertEquals(view.getSelectedItem().toString(), mFieldVariable.getVariable());
    }

    // Test update of view if variable selection changes.
    public void testUpdateViewFromField() {
        final BasicFieldVariableView view = makeFieldVariableView();

        // Updates complete asynchronously, so wait before testing.
        runAndSync(new Runnable() {
            @Override
            public void run() {
                mFieldVariable.setVariable(mVariables[0]);
            }
        }, TIMEOUT);
        assertEquals(mVariables[0], view.getSelectedItem().toString());

        runAndSync(new Runnable() {
            @Override
            public void run() {
                mFieldVariable.setVariable(mVariables[1]);
            }
        }, TIMEOUT);
        assertEquals(mVariables[1], view.getSelectedItem().toString());

        runAndSync(new Runnable() {
            @Override
            public void run() {
                mFieldVariable.setVariable(mVariables[2]);
            }
        }, TIMEOUT);
        assertEquals(mVariables[2], view.getSelectedItem().toString());
    }

    @NonNull
    private BasicFieldVariableView makeFieldVariableView() {
        BasicFieldVariableView view = new BasicFieldVariableView(mMockContext);
        view.setAdapter(mVariableAdapter);
        view.setField(mFieldVariable);
        return view;
    }

    private void runAndSync(final Runnable runnable, long timeoutMilliseconds) {
        assertNull(mExceptionInThread);

        final CountDownLatch latch = new CountDownLatch(1);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();

                    // Defer the latch until after all Runnables posted have completed.
                    // TODO: Consider using MessageQueue.isIdle() (API >= M)
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            latch.countDown();
                        }
                    });
                } catch (Throwable e) {
                    mExceptionInThread = e;
                }
            }
        });
        await(latch, timeoutMilliseconds);

        if (mExceptionInThread != null) {
            throw new IllegalStateException("Unhandled exception in mock main thread.",
                    mExceptionInThread);
        }
    }

    private void await(CountDownLatch latch, long timeoutMilliseconds) {
        try {
            latch.await(timeoutMilliseconds, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Timeout exceeded.", e);
        }
    }
}
