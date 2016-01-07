/*
 *  Copyright  2015 Google Inc. All Rights Reserved.
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

package com.google.blockly;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.google.blockly.control.BlocklyController;
import com.google.blockly.model.Workspace;

/**
 * Base Blockly activity class. This can be extended when creating a custom Blockly activity that
 * isn't supported by one of the concrete implementations.
 */
public abstract class AbsBlocklyActivity extends AppCompatActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    private static final String TAG = "AbsBlocklyActivity";

    private BlocklyController mController;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mController == null) {
            throw new IllegalArgumentException(
                    "A BlocklyController must have been created in onCreate");
        }
    }

    public final BlocklyController getController() {
        return mController;
    }

    public final Workspace getWorkspace() {
        return mController == null ? null : mController.getWorkspace();
    }

    /**
     * Create the BlocklyController if necessary and configures it. This must be called during
     * {@link #onCreate}. It may also be called while the activity is running to reconfigure the
     * controller.
     */
    public final void createController() {
        mController = onConfigureBlockly();
    }

    /**
     * Builds the BlocklyController for this activity. Override to build a controller with a custom
     * configuration. This should not be called directly, instead call {@link #createController()}
     * to create or reconfigure the controller.
     */
    abstract protected BlocklyController onConfigureBlockly();
}
