/*
 * Copyright  2015 Google Inc. All Rights Reserved.
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

package com.google.blockly.demo;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.google.blockly.BlocklySectionsActivity;
import com.google.blockly.NavigationDrawerFragment;
import com.google.blockly.model.BlocklySerializerException;
import com.google.blockly.utils.CodeGenerationRequest;
import com.google.blockly.utils.StringOutputStream;


/**
 * Demo app with the Blockly Games turtle game in a webview.
 */
public class TurtleActivity extends BlocklySectionsActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    private static final String TAG = "BlocklyTurtleActivity";

    public static final String WORKSPACE_FOLDER_PREFIX = "turtle/level_";
    private final Handler mHandler = new Handler();
    private WebView mTurtleWebview;
    private final CodeGenerationRequest.CodeGeneratorCallback mCodeGeneratorCallback =
            new CodeGenerationRequest.CodeGeneratorCallback() {
                @Override
                public void onFinishCodeGeneration(final String generatedCode) {
                    // Sample callback.
                    Toast.makeText(getApplicationContext(), generatedCode,
                            Toast.LENGTH_LONG).show();
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            // TODO(fenichel): Correctly escape all relevant characters, not just
                            // newlines.
                            String encoded =
                                    "Turtle.execute('" + generatedCode.replace("\n", "\\n") + "')";
                            mTurtleWebview.loadUrl("javascript:" + encoded);
                        }
                    });
                }
            };

    @Override
    protected String getWorkspaceBlocksPath(int section) {
        // Use the same blocks for all the levels. This lets the user's block code carry over from
        // level to level. The set of blocks shown in the toolbox for each level is defined by the
        // toolbox path below.
        return "turtle/definitions.json";
    }

    @Override
    protected String getWorkspaceToolboxPath(int section) {
        return "turtle/level_1/toolbox.xml";
    }

    @Override
    protected ListAdapter onCreateSectionsAdapter() {
        // Create three sections with the labels "Turtle 1", "Turtle 2", and "Turtle 3" displaying
        // them as simple text items in the sections drawer.
        return new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_activated_1,
                android.R.id.text1,
                new String[]{"Turtle 1", "Turtle 2", "Turtle 3"});
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout frLayout = (FrameLayout) findViewById(R.id.container);
        // Find the container we'll put our webview in and make it take up half the screen width.
        LayoutParams frlp = new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
        frlp.weight = 1;
        frLayout.setLayoutParams(frlp);

        mTurtleWebview = new WebView(this);
        mTurtleWebview.getSettings().setJavaScriptEnabled(true);
        mTurtleWebview.setWebChromeClient(new WebChromeClient());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        mTurtleWebview.loadUrl("file:///android_asset/turtle/turtle.html");
        frLayout.addView(mTurtleWebview);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == com.google.blockly.R.id.action_run) {
            try {
                if (mBound) {
                    final StringOutputStream serialized = new StringOutputStream();
                    mWorkspaceFragment.getWorkspace().serializeToXml(serialized);

                    mCodeGeneratorService.requestCodeGeneration(
                            new CodeGenerationRequest(serialized.toString(),
                                    mCodeGeneratorCallback,
                                    "turtle/definitions.json",
                                    "turtle/generators.js"));
                }
            } catch (BlocklySerializerException e) {
                Log.wtf(TAG, e);
                Toast.makeText(getApplicationContext(), "Code generation failed.",
                        Toast.LENGTH_LONG).show();

            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
