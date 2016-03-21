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

package com.google.blockly.android.demo;

import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.google.blockly.android.BlocklySectionsActivity;
import com.google.blockly.android.codegen.CodeGenerationRequest;
import com.google.blockly.android.ui.BlockViewFactory;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.android.ui.vertical.VerticalBlockViewFactory;
import com.google.blockly.util.JavascriptUtil;

import java.util.Arrays;
import java.util.List;


/**
 * Demo app with the Blockly Games turtle game in a webview.
 */
public class TurtleActivity extends BlocklySectionsActivity {
    private static final String TAG = "TurtleActivity";

    static final List<String> TURTLE_BLOCK_DEFINITIONS = Arrays.asList(new String[]{
            "default/loop_blocks.json",
            "default/math_blocks.json",
            "default/variable_blocks.json",
            "default/colour_blocks.json",
            "turtle/turtle_blocks.json"
    });

    static final List<String> TURTLE_BLOCK_GENERATORS = Arrays.asList(new String[]{
            "turtle/generators.js"
    });

    private static final int MAX_LEVELS = 10;
    private static final String[] LEVEL_TOOLBOX = new String[MAX_LEVELS];
    static {
        LEVEL_TOOLBOX[0] = "toolbox_basic.xml";
        LEVEL_TOOLBOX[1] = "toolbox_basic.xml";
        LEVEL_TOOLBOX[2] = "toolbox_colour.xml";
        LEVEL_TOOLBOX[3] = "toolbox_colour_pen.xml";
        LEVEL_TOOLBOX[4] = "toolbox_colour_pen.xml";
        LEVEL_TOOLBOX[5] = "toolbox_colour_pen.xml";
        LEVEL_TOOLBOX[6] = "toolbox_colour_pen.xml";
        LEVEL_TOOLBOX[7] = "toolbox_colour_pen.xml";
        LEVEL_TOOLBOX[8] = "toolbox_colour_pen.xml";
        LEVEL_TOOLBOX[9] = "toolbox_all.xml";
    }

    private final Handler mHandler = new Handler();
    private WebView mTurtleWebview;
    private final CodeGenerationRequest.CodeGeneratorCallback mCodeGeneratorCallback =
            new CodeGenerationRequest.CodeGeneratorCallback() {
                @Override
                public void onFinishCodeGeneration(final String generatedCode) {
                    // Sample callback.
                    Log.i(TAG, "generatedCode:\n" + generatedCode);
                    Toast.makeText(getApplicationContext(), generatedCode,
                            Toast.LENGTH_LONG).show();
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            String encoded = "Turtle.execute("
                                    + JavascriptUtil.makeJsString(generatedCode) + ")";
                            mTurtleWebview.loadUrl("javascript:" + encoded);
                        }
                    });
                }
            };

    @NonNull
    @Override
    protected List<String> getBlockDefinitionsJsonPaths() {
        // Use the same blocks for all the levels. This lets the user's block code carry over from
        // level to level. The set of blocks shown in the toolbox for each level is defined by the
        // toolbox path below.
        return TURTLE_BLOCK_DEFINITIONS;
    }

    @NonNull
    @Override
    protected List<String> getGeneratorsJsPaths() {
        return TURTLE_BLOCK_GENERATORS;
    }

    @NonNull
    @Override
    protected String getToolboxContentsXmlPath() {
        // Expose a different set of blocks to the user at each level.
        return "turtle/" + LEVEL_TOOLBOX[getCurrentSectionIndex()];
    }

    @Override
    public BlockViewFactory onCreateBlockViewFactory(WorkspaceHelper helper) {
        return new VerticalBlockViewFactory(this, helper);
    }

    @Override
    protected void onInitBlankWorkspace() {
        // TODO: (#22) Remove this override when variables are supported properly
        getController().addVariable("item");
        getController().addVariable("leo");
        getController().addVariable("don");
        getController().addVariable("mike");
        getController().addVariable("raf");
    }

    @NonNull
    @Override
    protected ListAdapter onCreateSectionsListAdapter() {
        // Create the game levels with the labels "Level 1", "Level 2", etc., displaying
        // them as simple text items in the sections drawer.
        String[] levelNames = new String[MAX_LEVELS];
        for (int i = 0; i < MAX_LEVELS; ++i) {
            levelNames[i] = "Level " + (i +1);
        }
        return new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_activated_1,
                android.R.id.text1,
                levelNames);
    }

    @Override
    protected boolean onSectionChanged(int oldSection, int newSection) {
        reloadToolbox();
        return true;
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
    protected CodeGenerationRequest.CodeGeneratorCallback getCodeGenerationCallback() {
        return mCodeGeneratorCallback;
    }
}
