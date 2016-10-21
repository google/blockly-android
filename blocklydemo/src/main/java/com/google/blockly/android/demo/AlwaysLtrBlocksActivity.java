package com.google.blockly.android.demo;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.google.blockly.android.AbstractBlocklyActivity;
import com.google.blockly.android.codegen.CodeGenerationRequest;
import com.google.blockly.android.codegen.LoggingCodeGeneratorCallback;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Created by marshalla on 10/21/16.
 */

public class AlwaysLtrBlocksActivity extends AbstractBlocklyActivity {
    private static final String TAG = "SimpleActivity";

    private static final List<String> BLOCK_DEFINITIONS = Arrays.asList(
            "default/list_blocks.json",
            "default/logic_blocks.json",
            "default/loop_blocks.json",
            "default/math_blocks.json",
            "default/text_blocks.json",
            "default/variable_blocks.json",
            "default/colour_blocks.json"
    );
    private static final List<String> JAVASCRIPT_GENERATORS = Arrays.asList(
            // Custom block generators go here. Default blocks are already included.
            // TODO(#99): Include Javascript defaults when other languages are supported.
    );

    CodeGenerationRequest.CodeGeneratorCallback mCodeGeneratorCallback =
            new LoggingCodeGeneratorCallback(this, TAG);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Configuration configuration = getResources().getConfiguration();
        configuration.setLayoutDirection(new Locale("en"));
        getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());

        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    protected List<String> getBlockDefinitionsJsonPaths() {
        return BLOCK_DEFINITIONS;
    }

    @NonNull
    @Override
    protected String getToolboxContentsXmlPath() {
        return "default/toolbox.xml";
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
        getController().addVariable("leo");
        getController().addVariable("don");
        getController().addVariable("mike");
        getController().addVariable("raf");
    }
}
