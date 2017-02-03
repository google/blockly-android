package com.google.blockly.android.demo;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.View;

import com.google.blockly.android.AbstractBlocklyActivity;
import com.google.blockly.android.codegen.CodeGenerationRequest;
import com.google.blockly.android.codegen.LoggingCodeGeneratorCallback;

import java.util.Arrays;
import java.util.List;

/**
 * Demonstration of how to replace {@code blockly_unified_layout} with a custom layout.
 * This alternative layout that places the toolbox on top, configured to always remain open.
 */
// TODO: Merge this demo into StyledFlyoutsActivity.
public class AlwaysOpenToolboxActivity extends AbstractBlocklyActivity {
    private static final String TAG = "SimpleActivity";

    private static final List<String> BLOCK_DEFINITIONS = Arrays.asList(
            "default/logic_blocks.json",
            "default/loop_blocks.json",
            "default/math_blocks.json"
    );
    private static final List<String> JAVASCRIPT_GENERATORS = Arrays.asList(
            // Custom block generators go here. Default blocks are already included.
            // TODO(#99): Include Javascript defaults when other languages are supported.
    );

    CodeGenerationRequest.CodeGeneratorCallback mCodeGeneratorCallback =
            new LoggingCodeGeneratorCallback(this, TAG);

    /**
     * Inflates a layout for the contents of the app that includes a toolbox that is always open.
     *
     * @param containerId The container id to target if using a {@link Fragment}
     * @return The {@link View} constructed. If using a {@link Fragment}, return null.
     */
    protected View onCreateContentView(int containerId) {
        return getLayoutInflater().inflate(R.layout.always_open_toolbox, null);
    }

    @NonNull
    @Override
    protected List<String> getBlockDefinitionsJsonPaths() {
        return BLOCK_DEFINITIONS;
    }

    @NonNull
    @Override
    protected String getToolboxContentsXmlPath() {
        return "simple_playground_toolbox.xml";
    }

    @NonNull
    @Override
    protected List<String> getGeneratorsJsPaths() {
        return JAVASCRIPT_GENERATORS;
    }

    @NonNull
    @Override
    protected CodeGenerationRequest.CodeGeneratorCallback getCodeGenerationCallback() {
        // Uses the same callback for every generation call.
        return mCodeGeneratorCallback;
    }

    @Override
    protected void onInitBlankWorkspace() {
        // Initialize variable names.
        // TODO: (#22) Remove this override when variables are supported properly
        getController().addVariable("item");
    }
}
