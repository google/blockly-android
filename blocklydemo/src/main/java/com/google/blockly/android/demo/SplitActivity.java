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

package com.google.blockly.android.demo;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.blockly.android.AbstractBlocklyActivity;
import com.google.blockly.android.codegen.CodeGenerationRequest;

import java.util.ArrayList;
import java.util.List;


/**
 * Demo activity that programmatically adds a view to split the screen between the Blockly workspace
 * and an arbitrary other view or fragment.
 */
public class SplitActivity extends AbstractBlocklyActivity {
    private static final String TAG = "SplitActivity";

    private static final String SAVE_FILENAME = "split_workspace.xml";
    private static final String AUTOSAVE_FILENAME = "split_workspace_temp.xml";

    private TextView mGeneratedTextView;
    private Handler mHandler;

    private String mNoCodeText;

    CodeGenerationRequest.CodeGeneratorCallback mCodeGeneratorCallback =
            new CodeGenerationRequest.CodeGeneratorCallback() {
                @Override
                public void onFinishCodeGeneration(final String generatedCode) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mGeneratedTextView.setText(generatedCode);
                            updateTextMinWidth();
                        }
                    });
                }
            };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return TurtleActivity.onDemoItemSelected(item, this) || super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler();
    }

    @Override
    protected View onCreateContentView(int parentId) {
        View root = getLayoutInflater().inflate(R.layout.split_content, null);
        mGeneratedTextView = (TextView) root.findViewById(R.id.generated_code);
        updateTextMinWidth();

        mNoCodeText = mGeneratedTextView.getText().toString(); // Capture initial value.

        return root;
    }

    @Override
    protected int getActionBarMenuResId() {
        return R.menu.split_actionbar;
    }

    @NonNull
    @Override
    protected List<String> getBlockDefinitionsJsonPaths() {
        return TurtleActivity.TURTLE_BLOCK_DEFINITIONS;
    }

    @NonNull
    @Override
    protected String getToolboxContentsXmlPath() {
        return "turtle/toolbox_advanced.xml";
    }

    @NonNull
    @Override
    protected List<String> getGeneratorsJsPaths() {
        List<String> paths = new ArrayList<String>(1);
        paths.add("turtle/generators.js");
        return paths;
    }

    @NonNull
    @Override
    protected CodeGenerationRequest.CodeGeneratorCallback getCodeGenerationCallback() {
        // Uses the same callback for every generation call.
        return mCodeGeneratorCallback;
    }

    @Override
    public void onClearWorkspace() {
        super.onClearWorkspace();
        mGeneratedTextView.setText(mNoCodeText);
        updateTextMinWidth();
    }

    /**
     * Estimate the pixel size of the longest line of text, and set that to the TextView's minimum
     * width.
     */
    private void updateTextMinWidth() {
        String text = mGeneratedTextView.getText().toString();
        int maxline = 0;
        int start = 0;
        int index = text.indexOf('\n', start);
        while (index > 0) {
            maxline = Math.max(maxline, index - start);
            start = index + 1;
            index = text.indexOf('\n', start);
        }
        int remainder = text.length() - start;
        if (remainder > 0) {
            maxline = Math.max(maxline, remainder);
        }

        float density = getResources().getDisplayMetrics().density;
        mGeneratedTextView.setMinWidth((int) (maxline * 13 * density));
    }

    /**
     * Optional override of the save path, since this demo Activity has multiple Blockly
     * configurations.
     * @return Workspace save path used by this Activity.
     */
    @Override
    @NonNull
    protected String getWorkspaceSavePath() {
        return SAVE_FILENAME;
    }

    /**
     * Optional override of the auto-save path, since this demo Activity has multiple Blockly
     * configurations.
     * @return Workspace auto-save path used by this Activity.
     */
    @Override
    @NonNull
    protected String getWorkspaceAutosavePath() {
        return AUTOSAVE_FILENAME;
    }
}
