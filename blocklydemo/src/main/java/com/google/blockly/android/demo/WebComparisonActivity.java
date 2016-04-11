/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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
import android.support.annotation.NonNull;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.google.blockly.android.AbstractBlocklyActivity;
import com.google.blockly.android.codegen.CodeGenerationRequest;
import com.google.blockly.android.codegen.LoggingCodeGeneratorCallback;
import com.google.blockly.android.ui.BlockViewFactory;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.android.ui.vertical.VerticalBlockViewFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Demo app with the Blockly Games turtle game in a webview.
 */
public class WebComparisonActivity extends AbstractBlocklyActivity {
    private static final String TAG = "ComparisonActivity";

    static final List<String> COMPARE_BLOCK_DEFINITIONS = Arrays.asList(new String[]{
            "default/loop_blocks.json",
            "default/math_blocks.json",
            "default/variable_blocks.json",
            "default/test_blocks.json",
            "sample_sections/definitions.json"
    });

    static final List<String> COMPARE_BLOCK_GENERATORS = Arrays.asList(new String[]{
            "sample_sections/generators.js"
    });

    private WebView mWebBlocklyView;
    private final CodeGenerationRequest.CodeGeneratorCallback mCodeGeneratorCallback
            = new LoggingCodeGeneratorCallback(this, "ComparisonActivity");

    @NonNull
    @Override
    protected List<String> getBlockDefinitionsJsonPaths() {
        // Use the same blocks for all the levels. This lets the user's block code carry over from
        // level to level. The set of blocks shown in the toolbox for each level is defined by the
        // toolbox path below.
        return COMPARE_BLOCK_DEFINITIONS;
    }

    @NonNull
    @Override
    protected List<String> getGeneratorsJsPaths() {
        return COMPARE_BLOCK_GENERATORS;
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
    protected String getToolboxContentsXmlPath() {
        return "sample_sections/level_3/toolbox.xml";
    }

    @Override
    public BlockViewFactory onCreateBlockViewFactory(WorkspaceHelper helper) {
        return new VerticalBlockViewFactory(this, helper);
    }

    @Override
    protected View onCreateContentView(int parentId) {
        View root = getLayoutInflater().inflate(R.layout.turtle_content, null);

        mWebBlocklyView = (WebView) root.findViewById(R.id.turtle_runtime);
        mWebBlocklyView.getSettings().setJavaScriptEnabled(true);
        mWebBlocklyView.setWebChromeClient(new WebChromeClient());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        mWebBlocklyView.loadUrl("file:///android_asset/webblockly/index.html");

        return root;
    }

    @NonNull
    @Override
    protected CodeGenerationRequest.CodeGeneratorCallback getCodeGenerationCallback() {
        return mCodeGeneratorCallback;
    }
}
