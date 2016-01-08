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

package com.google.blockly.demo;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.widget.FrameLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.google.blockly.AbstractBlocklyActivity;
import com.google.blockly.utils.CodeGenerationRequest;

/**
 * Demo activity that programmatically adds a view to split the screen between the Blockly workspace
 * and an arbitrary other view or fragment.
 */
public class SplitActivity extends AbstractBlocklyActivity {
    private static final String TAG = "SplitActivity";

    TextView mGeneratedTextView;
    Handler mHandler;

    CodeGenerationRequest.CodeGeneratorCallback mCodeGeneratorCallback =
            new CodeGenerationRequest.CodeGeneratorCallback() {
                @Override
                public void onFinishCodeGeneration(final String generatedCode) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mGeneratedTextView.setText(generatedCode);
                        }
                    });
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler();
    }

    @Override
    protected void onCreateContentView() {
        super.onCreateContentView();
        FrameLayout frLayout = (FrameLayout) findViewById(R.id.container);

        mGeneratedTextView = new TextView(this);
        mGeneratedTextView.setText("Run the block code\nto see it in javascript!");

        mGeneratedTextView.setLayoutParams(
                new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        frLayout.addView(mGeneratedTextView);
    }

    @Override
    @NonNull
    protected String getWorkspaceBlocksPath() {
        return "turtle/definitions.json";
    }

    @Override
    @NonNull
    protected String getWorkspaceToolboxPath() {
        return "turtle/level_1/toolbox.xml";
    }

    @Override
    @NonNull
    protected String getGeneratorJsFilename() {
        return "turtle/generators.js";
    }

    @Override
    @NonNull
    protected String getBlockDefinitionsFilename() {
        return "turtle/definitions.json";
    }

    @NonNull
    @Override
    protected CodeGenerationRequest.CodeGeneratorCallback getCreateCodeGenerationCallback() {
        // Uses the same callback for every generation call.
        return mCodeGeneratorCallback;
    }
}
