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
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.google.blockly.android.AbstractBlocklyActivity;
import com.google.blockly.android.BlocklySectionsActivity;
import com.google.blockly.android.codegen.CodeGenerationRequest;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.model.DefaultBlocks;
import com.google.blockly.util.JavascriptUtil;
import com.google.blockly.utils.BlockLoadingException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;


/**
 * Demo app with the Blockly Games turtle game in a webview.
 */
public class TurtleActivity extends BlocklySectionsActivity {
    private static final String TAG = "TurtleActivity";

    private static final String SAVE_FILENAME = "turtle_workspace.xml";
    private static final String AUTOSAVE_FILENAME = "turtle_workspace_temp.xml";

    static final List<String> TURTLE_BLOCK_DEFINITIONS = Arrays.asList(
            DefaultBlocks.COLOR_BLOCKS_PATH,
            DefaultBlocks.LOGIC_BLOCKS_PATH,
            DefaultBlocks.LOOP_BLOCKS_PATH,
            DefaultBlocks.MATH_BLOCKS_PATH,
            DefaultBlocks.TEXT_BLOCKS_PATH,
            DefaultBlocks.VARIABLE_BLOCKS_PATH,
            "turtle/turtle_blocks.json"
    );
    static final List<String> TURTLE_BLOCK_GENERATORS = Arrays.asList(
            "turtle/generators.js"
    );
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
        LEVEL_TOOLBOX[9] = "toolbox_advanced.xml";
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

    @Override
    public void onLoadWorkspace() {
        mBlocklyActivityHelper.loadWorkspaceFromAppDirSafely(SAVE_FILENAME);
    }

    @Override
    public void onSaveWorkspace() {
        mBlocklyActivityHelper.saveWorkspaceToAppDirSafely(SAVE_FILENAME);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return onDemoItemSelected(item, this) || super.onOptionsItemSelected(item);
    }

    static boolean onDemoItemSelected(MenuItem item, AbstractBlocklyActivity activity) {
        BlocklyController controller = activity.getController();
        int id = item.getItemId();
        boolean loadWorkspace = false;
        String filename = "";
        if (id == R.id.action_demo_android) {
            loadWorkspace = true;
            filename = "android.xml";
        } else if (id == R.id.action_demo_lacey_curves) {
            loadWorkspace = true;
            filename = "lacey_curves.xml";
        } else if (id == R.id.action_demo_paint_strokes) {
            loadWorkspace = true;
            filename = "paint_strokes.xml";
        }

        if (loadWorkspace) {
            String assetFilename = "turtle/demo_workspaces/" + filename;
            try {
                controller.loadWorkspaceContents(activity.getAssets().open(assetFilename));
            } catch (IOException | BlockLoadingException e) {
                throw new IllegalStateException(
                        "Couldn't load demo workspace from assets: " + assetFilename, e);
            }
            addDefaultVariables(controller);
            return true;
        }

        return false;
    }

    @NonNull
    @Override
    protected List<String> getBlockDefinitionsJsonPaths() {
        // Use the same blocks for all the levels. This lets the user's block code carry over from
        // level to level. The set of blocks shown in the toolbox for each level is defined by the
        // toolbox path below.
        return TURTLE_BLOCK_DEFINITIONS;
    }

    @Override
    protected int getActionBarMenuResId() {
        return R.menu.turtle_actionbar;
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
    protected void onInitBlankWorkspace() {
        addDefaultVariables(getController());
    }

    @NonNull
    @Override
    protected ListAdapter onCreateSectionsListAdapter() {
        // Create the game levels with the labels "Level 1", "Level 2", etc., displaying
        // them as simple text items in the sections drawer.
        String[] levelNames = new String[MAX_LEVELS];
        for (int i = 0; i < MAX_LEVELS; ++i) {
            levelNames[i] = "Level " + (i + 1);
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

    static void addDefaultVariables(BlocklyController controller) {
        // TODO: (#22) Remove this override when variables are supported properly
        controller.addVariable("item");
        controller.addVariable("count");
        controller.addVariable("marshmallow");
        controller.addVariable("lollipop");
        controller.addVariable("kitkat");
        controller.addVariable("android");
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
