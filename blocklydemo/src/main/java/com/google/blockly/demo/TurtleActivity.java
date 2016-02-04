/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.google.blockly.BlocklySectionsActivity;
import com.google.blockly.utils.CodeGenerationRequest;


/**
 * Demo app with the Blockly Games turtle game in a webview.
 */
public class TurtleActivity extends BlocklySectionsActivity {
    private static final String TAG = "TurtleActivity";

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

    @NonNull
    @Override
    protected String getBlockDefinitionsJsonPath() {
        // Use the same blocks for all the levels. This lets the user's block code carry over from
        // level to level. The set of blocks shown in the toolbox for each level is defined by the
        // toolbox path below.
        return "turtle/definitions.json";
    }

    @NonNull
    @Override
    protected String getToolboxContentsXmlPath() {
        return "turtle/level_1/toolbox.xml";
    }

    @NonNull
    @Override
    protected ListAdapter onCreateSectionsListAdapter() {
        // Create three sections with the labels "Turtle 1", "Turtle 2", and "Turtle 3" displaying
        // them as simple text items in the sections drawer.
        return new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_activated_1,
                android.R.id.text1,
                new String[]{"Turtle 1", "Turtle 2", "Turtle 3"});
    }

    @Override
    protected boolean onSectionChanged(int oldSection, int newSection) {
        // TODO(#363): Load different levels.
        return false;
    }

    @Override
    protected View onCreateContentView(int parentId) {
        View root = getLayoutInflater().inflate(R.layout.turtle_content, null);

        mTurtleWebview = (WebView) root.findViewById(R.id.turtle_runtime);
        mTurtleWebview.getSettings().setJavaScriptEnabled(true);
        mTurtleWebview.setWebChromeClient(new WebChromeClient());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        mTurtleWebview.loadUrl("file:///android_asset/turtle/turtle.html");

        return root;
    }

    @NonNull
    @Override
    protected String getGeneratorJsFilename() {
        return "turtle/generators.js";
    }

    @NonNull
    @Override
    protected CodeGenerationRequest.CodeGeneratorCallback getCodeGenerationCallback() {
        return mCodeGeneratorCallback;
    }
}
