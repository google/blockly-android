package com.google.blockly.android.codegen;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.blockly.android.codegen.CodeGenerationRequest.CodeGeneratorCallback;
import com.google.blockly.model.BlocklySerializerException;
import com.google.blockly.model.Workspace;
import com.google.blockly.utils.StringOutputStream;

import java.util.List;

/**
 * Client Side class responsible for connecting to the {@link CodeGeneratorService}.
 */
public class CodeGeneratorManager {
    private static final String TAG = "CodeGeneratorManager";

    private final Context mContext;
    private final LazyServiceConnection mCodeGenerationConnection;

    public CodeGeneratorManager(Context context) {
        this.mContext = context;
        this.mCodeGenerationConnection = new LazyServiceConnection(context);
    }

    /**
     * Resumes the underlying code generation connection.
     */
    public void onResume() {
        mCodeGenerationConnection.onResume();
    }

    /**
     * Pauses the underlying code generation connection.
     */
    public void onPause() {
        mCodeGenerationConnection.onPause();
    }

    /**
     * Calls the Service to request code generation for the workspace passed in.
     *
     * @param workspace the workspace to request code generation for.
     * @param codeGenerationCallback the callback to call with the generated code.
     */
    public void requestCodeGeneration(List<String> blockDefinitionsJsonPaths,
                                      List<String> generatorsJsPaths,
                                      Workspace workspace,
                                      CodeGeneratorCallback codeGenerationCallback) {

        if (workspace.hasBlocks()) {
            try {
                final StringOutputStream serialized = new StringOutputStream();
                workspace.serializeToXml(serialized);
                CodeGenerationRequest codeGenerationRequest =
                    new CodeGenerationRequest(
                        serialized.toString(),
                        codeGenerationCallback,
                        blockDefinitionsJsonPaths,
                        generatorsJsPaths);
                mCodeGenerationConnection.requestCodeGeneration(codeGenerationRequest);
            } catch (BlocklySerializerException e) {
                Log.wtf(TAG, e);
                Toast.makeText(mContext, "Code generation failed.",
                    Toast.LENGTH_LONG).show();

            }
        } else {
            Log.i(TAG, "No blocks in workspace. Skipping run request.");
        }
    }
}
